/**
 * 顔ドリブン追跡器：CameraX でフロントカメラをプレビューし、ML Kit で顔を検出して
 * Live2D モデルパラメータ（頭部角度・眼球・口の開き・まばたき）へマッピングする。
 *
 * 検出結果は EMA 平滑化してから JNI 経由で C++ (LAppModel) へ渡す。
 */
package com.live2image.app;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class FaceTracker {
    private static final float SMOOTH = 0.3f;          // EMA 係数（大きいほど追従が速い）
    private static final float ANGLE_RANGE = 30.0f;    // 頭部角度の正規化範囲（度）
    private static final float MOUTH_REF = 0.15f;      // 口の開きの正規化基準（唇縫い間隔/目間隔）

    private final Context context;
    private final TextureView previewView;
    private final FaceDetector faceDetector;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private boolean enabled = false;

    // 平滑済みパラメータ
    private float sAngleX = 0f, sAngleY = 0f, sAngleZ = 0f;
    private float sEyeX = 0f, sEyeY = 0f, sMouth = 0f;
    private float sBlinkL = 1f, sBlinkR = 1f;

    public FaceTracker(@NonNull Context context, @NonNull TextureView previewView) {
        this.context = context;
        this.previewView = previewView;

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setMinFaceSize(0.15f)
                .build();
        this.faceDetector = FaceDetection.getClient(options);
    }

    /** カメラライフサイクルをバインドしてプレビュー＋解析を開始する。 */
    public void start(@NonNull LifecycleOwner owner) {
        cameraProviderFuture = ProcessCameraProvider.getInstance(context);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider provider = cameraProviderFuture.get();
                bind(provider, owner);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(context));
    }

    private void bind(@NonNull ProcessCameraProvider provider, @NonNull LifecycleOwner owner) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(this::providePreviewSurface);

        CameraSelector selector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        ImageAnalysis analysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(320, 240))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        analysis.setAnalyzer(ContextCompat.getMainExecutor(context), this::analyze);

        provider.bindToLifecycle(owner, selector, preview, analysis);
    }

    // TextureView を CameraX のプレビュー出力先として提供する
    private void providePreviewSurface(@NonNull SurfaceRequest request) {
        SurfaceTexture st = previewView.getSurfaceTexture();
        if (st == null) {
            request.willNotProvideSurface();
            return;
        }
        st.setDefaultBufferSize(request.getResolution().getWidth(), request.getResolution().getHeight());
        Surface surface = new Surface(st);
        request.provideSurface(surface, ContextCompat.getMainExecutor(context), result -> surface.release());
    }

    private void analyze(@NonNull ImageProxy imageProxy) {
        if (!enabled) {
            imageProxy.close();
            return;
        }
        InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees());
        faceDetector.process(image)
                .addOnSuccessListener(this::onFaces)
                .addOnFailureListener(Throwable::printStackTrace)
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private void onFaces(@NonNull List<Face> faces) {
        if (faces.isEmpty()) {
            return; // 顔が無い場合は最後の値を維持
        }
        Face face = faces.get(0);

        // 頭部回転（度）→ [-1,1] 正規化
        float yaw = face.getHeadEulerAngleY();    // 左右（ヨー）
        float pitch = face.getHeadEulerAngleX();  // 上下（ピッチ）
        float roll = face.getHeadEulerAngleZ();   // 傾き（ロール）
        float angleX = clamp(yaw / ANGLE_RANGE, -1f, 1f);
        float angleY = clamp(pitch / ANGLE_RANGE, -1f, 1f);
        float angleZ = clamp(roll / ANGLE_RANGE, -1f, 1f);

        // 眼球は頭部の向きに追従
        float eyeX = angleX;
        float eyeY = angleY;

        // まばたき（1=開, 0=閉）
        Float lo = face.getLeftEyeOpenProbability();
        Float ro = face.getRightEyeOpenProbability();
        float blinkL = (lo != null) ? lo : 1f;
        float blinkR = (ro != null) ? ro : 1f;

        // 口の開き：上唇・下唇輪郭の中点距離 / 両目間距離
        float mouth = computeMouthOpen(face);

        // EMA 平滑化
        sAngleX += (angleX - sAngleX) * SMOOTH;
        sAngleY += (angleY - sAngleY) * SMOOTH;
        sAngleZ += (angleZ - sAngleZ) * SMOOTH;
        sEyeX += (eyeX - sEyeX) * SMOOTH;
        sEyeY += (eyeY - sEyeY) * SMOOTH;
        sMouth += (mouth - sMouth) * SMOOTH;
        sBlinkL += (blinkL - sBlinkL) * SMOOTH;
        sBlinkR += (blinkR - sBlinkR) * SMOOTH;

        // 角度は度単位で C++ へ渡す（ParamAngle* は度を想定）
        JniBridgeJava.nativeSetFaceParameters(
                sAngleX * ANGLE_RANGE, sAngleY * ANGLE_RANGE, sAngleZ * ANGLE_RANGE,
                sEyeX, sEyeY, sMouth, sBlinkL, sBlinkR);
    }

    private float computeMouthOpen(@NonNull Face face) {
        if (face.getContour(FaceContour.UPPER_LIP_BOTTOM) == null
                || face.getContour(FaceContour.LOWER_LIP_TOP) == null
                || face.getContour(FaceContour.LEFT_EYE) == null
                || face.getContour(FaceContour.RIGHT_EYE) == null) {
            return sMouth;
        }
        float[] upper = centroid(face.getContour(FaceContour.UPPER_LIP_BOTTOM).getPoints());
        float[] lower = centroid(face.getContour(FaceContour.LOWER_LIP_TOP).getPoints());
        float[] leye = centroid(face.getContour(FaceContour.LEFT_EYE).getPoints());
        float[] reye = centroid(face.getContour(FaceContour.RIGHT_EYE).getPoints());

        float lipDist = (float) Math.hypot(upper[0] - lower[0], upper[1] - lower[1]);
        float eyeDist = (float) Math.hypot(leye[0] - reye[0], leye[1] - reye[1]);
        if (eyeDist < 1e-3f) {
            return sMouth;
        }
        return clamp(lipDist / (eyeDist * MOUTH_REF), 0f, 1f);
    }

    private static float[] centroid(@NonNull List<android.graphics.PointF> points) {
        float x = 0f, y = 0f;
        for (android.graphics.PointF p : points) {
            x += p.x;
            y += p.y;
        }
        int n = Math.max(1, points.size());
        return new float[]{x / n, y / n};
    }

    public void setEnabled(boolean e) {
        enabled = e;
        JniBridgeJava.nativeSetFaceEnabled(e);
        if (!e) {
            // オフ時に値をリセットしてアイドルモーションへ戻す
            sAngleX = sAngleY = sAngleZ = sEyeX = sEyeY = sMouth = 0f;
            sBlinkL = sBlinkR = 1f;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
