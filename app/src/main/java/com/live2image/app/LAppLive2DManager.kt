package com.live2image.app

import com.live2d.sdk.cubism.math.CubismMatrix44

/**
 * 场景管理：加载/持有当前模型，转发更新与绘制（对齐官方 LAppLive2DManager）。
 */
class LAppLive2DManager private constructor() {

    private var model: LAppModel? = null

    // 视图矩阵：控制模型的缩放与平移（拖拽/缩放作用于它）
    val viewMatrix: CubismMatrix44 = CubismMatrix44()

    private var isLoadModel = false

    fun loadModel(dir: String, json: String) {
        val m = LAppModel()
        m.loadAssetsFromFileSystem(dir, json)
        model = m
        isLoadModel = true
        LAppPal.logInfo("model loaded: %s/%s", dir, json)
    }

    fun isLoadModel(): Boolean = isLoadModel

    fun update() {
        model?.updateModel()
    }

    /** 绘制当前模型，返回使用的模型矩阵。 */
    fun draw(): CubismMatrix44 {
        val m = model ?: return CubismMatrix44()
        m.drawModel(viewMatrix)
        return viewMatrix
    }

    fun playMotionTapBody() {
        model?.playMotion(LAppDefine.MOTION_GROUP_TAP_BODY, 0, LAppDefine.PRIORITY_NORMAL)
    }

    // --- 交互 ---
    fun onDrag(dx: Float, dy: Float) {
        // 拖拽：平移视图矩阵（像素 -> 模型坐标的换算可在此细化）
        val screenToModel = 2.0f / maxOf(width, 1)
        viewMatrix.translateX(dx * screenToModel)
        viewMatrix.translateY(-dy * screenToModel)
    }

    fun onPinch(scale: Float) {
        var s = lastScale * scale
        s = s.coerceIn(LAppDefine.VIEW_MIN_SCALE, LAppDefine.VIEW_MAX_SCALE)
        viewMatrix.scale(s / lastScale)
        lastScale = s
    }

    fun resetViewScale() {
        lastScale = 1.0f
    }

    private var lastScale = 1.0f

    // 供坐标换算使用，由 LAppView 在 onSurfaceChanged 时更新
    var width: Int = 1
    var height: Int = 1

    fun release() {
        model = null
        isLoadModel = false
    }

    companion object {
        @Volatile
        private var instance: LAppLive2DManager? = null

        fun getInstance(): LAppLive2DManager =
            instance ?: synchronized(this) {
                instance ?: LAppLive2DManager().also { instance = it }
            }
    }
}
