/**
 * Copyright(c) Live2D Inc. All rights reserved.
 * <p>
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at https://www.live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

package com.live2image.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA = 1001;

    private GLSurfaceView glSurfaceView;
    private GLRenderer glRenderer;
    private FaceTracker faceTracker;
    private Button faceToggle;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JniBridgeJava.SetActivityInstance(this);
        JniBridgeJava.SetContext(this);

        setContentView(R.layout.activity_main);

        glSurfaceView = findViewById(R.id.gl_surface_view);
        glSurfaceView.setEGLContextClientVersion(2);
        glRenderer = new GLRenderer();
        glSurfaceView.setRenderer(glRenderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        TextureView previewView = findViewById(R.id.preview_view);
        faceTracker = new FaceTracker(this, previewView);

        faceToggle = findViewById(R.id.face_toggle);
        faceToggle.setOnClickListener(v -> toggleFace());

        // GLSurfaceViewでタッチイベントを処理する
        glSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, final MotionEvent event) {
                final float pointX = event.getX();
                final float pointY = event.getY();

                glSurfaceView.queueEvent(
                    new Runnable() {
                        @Override
                        public void run() {
                            switch (event.getAction()) {
                                case MotionEvent.ACTION_DOWN:
                                    JniBridgeJava.nativeOnTouchesBegan(pointX, pointY);
                                    break;
                                case MotionEvent.ACTION_UP:
                                    JniBridgeJava.nativeOnTouchesEnded(pointX, pointY);
                                    break;
                                case MotionEvent.ACTION_MOVE:
                                    JniBridgeJava.nativeOnTouchesMoved(pointX, pointY);
                                    break;
                            }
                        }
                    }
                );
                return true;
            }
        });

        hideSystemUi();
    }

    private void toggleFace() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
            return;
        }
        startFaceTracking();
    }

    private void startFaceTracking() {
        boolean willEnable = !faceTracker.isEnabled();
        if (willEnable && !faceTracker.isEnabled()) {
            faceTracker.start(this); // 初回のみライフサイクルをバインド
        }
        faceTracker.setEnabled(willEnable);
        faceToggle.setText(willEnable ? R.string.face_on : R.string.face_off);
        Toast.makeText(this,
                willEnable ? R.string.face_start : R.string.face_stop,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startFaceTracking();
        } else if (requestCode == REQUEST_CAMERA) {
            Toast.makeText(this, R.string.camera_denied, Toast.LENGTH_SHORT).show();
        }
    }

    private void hideSystemUi() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        } else {
            getWindow().getInsetsController().hide(WindowInsets.Type.navigationBars()
                | WindowInsets.Type.statusBars());
            getWindow().getInsetsController().setSystemBarsBehavior(
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        JniBridgeJava.nativeOnStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        glSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        glSurfaceView.onPause();
        JniBridgeJava.nativeOnPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        JniBridgeJava.nativeOnStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        JniBridgeJava.nativeOnDestroy();
    }
}
