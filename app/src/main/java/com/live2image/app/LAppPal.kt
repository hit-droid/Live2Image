package com.live2image.app

import android.content.Context
import android.opengl.GLES20
import android.util.Log
import java.io.InputStream

/**
 * 通用工具：日志、资源读取、着色器编译/链接。
 */
object LAppPal {
    private const val TAG = "Live2Image"

    fun logInfo(fmt: String, vararg args: Any?) = Log.i(TAG, String.format(fmt, *args))
    fun logDebug(fmt: String, vararg args: Any?) = Log.d(TAG, String.format(fmt, *args))
    fun logWarn(fmt: String, vararg args: Any?) = Log.w(TAG, String.format(fmt, *args))
    fun logError(fmt: String, vararg args: Any?) = Log.e(TAG, String.format(fmt, *args))

    /** 从 assets 读取整个文件为 ByteArray。 */
    fun loadFileFromAssets(context: Context, path: String): ByteArray =
        context.assets.open(path).use { it.readBytes() }

    fun compileShader(shaderType: Int, shaderSource: String): Int {
        val shader = GLES20.glCreateShader(shaderType)
        GLES20.glShaderSource(shader, shaderSource)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == GLES20.GL_FALSE) {
            logError("compile shader failed: %s", GLES20.glGetShaderInfoLog(shader))
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    fun linkProgram(vertexShaderSource: String, fragmentShaderSource: String): Int {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderSource)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderSource)
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vs)
        GLES20.glAttachShader(program, fs)
        GLES20.glLinkProgram(program)
        val status = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
        if (status[0] == GLES20.GL_FALSE) {
            logError("link program failed: %s", GLES20.glGetProgramInfoLog(program))
            GLES20.glDeleteProgram(program)
            return 0
        }
        return program
    }
}
