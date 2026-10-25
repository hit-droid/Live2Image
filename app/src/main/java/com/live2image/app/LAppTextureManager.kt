package com.live2image.app

import android.content.res.AssetManager
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import java.io.IOException

/**
 * 纹理管理：从 assets 加载 PNG 上传为 GL 纹理（预乘 alpha），带缓存。
 * 逻辑与官方 Sample 的 LAppTextureManager 一致。
 */
class LAppTextureManager(private val assetManager: AssetManager) {

    class TextureInfo(val id: Int, val width: Int, val height: Int)

    private val textureInfo = HashMap<String, TextureInfo>()

    fun getTexture(fileName: String): Int {
        textureInfo[fileName]?.let { return it.id }

        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ids[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        try {
            assetManager.open(fileName).use { input ->
                val bitmap = BitmapFactory.decodeStream(input) ?: return 0
                bitmap.setPremultiplied(true) // 保证混合正确
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
                GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
                textureInfo[fileName] = TextureInfo(ids[0], bitmap.width, bitmap.height)
                bitmap.recycle()
            }
        } catch (e: IOException) {
            LAppPal.logError("load texture failed: %s", fileName)
            e.printStackTrace()
        }
        return ids[0]
    }

    fun release() {
        for (info in textureInfo.values) {
            GLES20.glDeleteTextures(1, intArrayOf(info.id), 0)
        }
        textureInfo.clear()
    }
}
