package com.live2image.app

import com.live2d.sdk.cubism.framework.CubismUserModel
import com.live2d.sdk.cubism.math.CubismMatrix44

/**
 * 单个 Live2D 模型的封装，继承官方 [CubismUserModel]。
 *
 * 说明：以下 Cubism 调用的命名对齐官方 Android Sample；
 * 具体方法签名可能随 SDK 版本变化，请以你下载的 Cubism SDK for Native 对应 Sample 为准。
 */
class LAppModel : CubismUserModel() {

    private lateinit var textureManager: LAppTextureManager
    private var modelDir: String = ""

    init {
        LAppDelegate.getInstance().activity?.let {
            textureManager = LAppTextureManager(it.assets)
        }
    }

    /** 从 assets 的指定目录加载 model3.json。 */
    fun loadAssetsFromFileSystem(dirPath: String, jsonName: String) {
        modelDir = dirPath
        val manager = LAppDelegate.getInstance().activity?.assets ?: return
        val json = LAppPal.loadFileFromAssets(manager, "$dirPath/$jsonName")
        // 官方 API（示例）：loadAssets(dirPath, jsonName, buffer) —— 内部解析并创建 CubismMoc/Model
        loadAssets(dirPath, jsonName, json)
    }

    /** 每帧更新：动作、物理、眨眼、呼吸等。 */
    fun updateModel() {
        // 官方 API：doUpdate() / update() —— 签名以 SDK 为准
        doUpdate()
    }

    /** 以给定矩阵绘制模型。 */
    fun drawModel(matrix: CubismMatrix44) {
        // 官方 API：draw(matrix: CubismMatrix44)
        draw(matrix)
    }

    /** 播放动作。 */
    fun playMotion(group: String, no: Int, priority: Int) {
        startMotion(group, no, priority)
    }

    /** 设置表情。 */
    fun playExpression(expressionId: String) {
        setExpression(expressionId)
    }
}
