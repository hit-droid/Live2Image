package com.live2image.app

/**
 * 应用常量定义（命名贴近官方 Cubism SDK for Native 的 Android Sample）。
 */
object LAppDefine {
    // 模型资源位置：assets 下的目录 + model3.json 文件名
    const val MODEL_DIR = "Haru"
    const val MODEL_JSON = "Haru.model3.json"

    // 视图缩放范围
    const val VIEW_MAX_SCALE = 3.0f
    const val VIEW_MIN_SCALE = 0.5f

    // 模型在屏幕上的归一化位置（0~1，左下为原点）
    const val MODEL_X = 0.5f
    const val MODEL_Y = 0.55f

    // 动作 / 表情
    const val MOTION_GROUP_IDLE = "Idle"
    const val MOTION_GROUP_TAP_BODY = "TapBody"
    const val PRIORITY_IDLE = 1
    const val PRIORITY_NORMAL = 2
    const val PRIORITY_FORCE = 3

    // 是否把模型渲染到纹理（贴图/捕获用）。默认关。
    const val USE_RENDER_TARGET = false
    const val USE_MODEL_RENDER_TARGET = false
}
