package com.live2image.app

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import com.live2d.sdk.cubism.math.CubismMatrix44
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * 渲染视图：GLSurfaceView + Renderer，负责 GL 生命周期、模型加载、触摸分发。
 * 命名与职责对齐官方 Android Sample 的 LAppView。
 */
class LAppView(context: Context) :
    GLSurfaceView(context),
    GLSurfaceView.Renderer,
    TouchManager.OnTouchListener {

    private val delegate = LAppDelegate.getInstance()
    private val manager = LAppLive2DManager.getInstance()
    private val touchManager = TouchManager()

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        setRenderer(this)
        renderMode = RENDERMODE_CONTINUOUSLY // 持续渲染，驱动待机动画
        touchManager.setOnTouchListener(this)
        delegate.attachView(this)
    }

    // ===== GLSurfaceView.Renderer =====
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA) // 预乘 alpha
        loadModelIfNeeded()
    }

    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
        manager.width = w
        manager.height = h
        GLES20.glViewport(0, 0, w, h)
        // 以参考高度做一次初始缩放，让模型大致铺满屏幕（矩阵方法名以 SDK 为准）
        val scale = h.toFloat() / REFERENCE_HEIGHT
        manager.viewMatrix.scale(scale)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        manager.update()
        manager.draw()
    }

    private fun loadModelIfNeeded() {
        if (manager.isLoadModel()) return
        val activity = delegate.activity ?: return
        val has = try {
            activity.assets.list(LAppDefine.MODEL_DIR)?.contains(LAppDefine.MODEL_JSON) == true
        } catch (e: Exception) {
            false
        }
        if (has) {
            manager.loadModel(LAppDefine.MODEL_DIR, LAppDefine.MODEL_JSON)
        } else {
            LAppPal.logWarn(
                "assets/%s 下未找到 %s，先显示空场景；请按 README 放入模型",
                LAppDefine.MODEL_DIR, LAppDefine.MODEL_JSON
            )
        }
    }

    // ===== 触摸 =====
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> touchManager.touchesBegan(event)
            MotionEvent.ACTION_MOVE -> touchManager.touchesMoved(event)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> touchManager.touchesEnded(event)
        }
        return true
    }

    // ===== TouchManager.OnTouchListener =====
    override fun onTap(x: Float, y: Float) {
        manager.playMotionTapBody()
    }

    override fun onDrag(dx: Float, dy: Float) {
        manager.onDrag(dx, dy)
    }

    override fun onPinch(scale: Float) {
        manager.onPinch(scale)
    }

    override fun onTouchesBegan() {}
    override fun onTouchesEnded() {}

    companion object {
        private const val REFERENCE_HEIGHT = 1280f
    }
}
