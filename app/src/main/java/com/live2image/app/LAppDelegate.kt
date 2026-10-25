package com.live2image.app

import android.app.Activity
import com.live2d.sdk.cubism.framework.CubismFramework

/**
 * 应用委托：持有 Activity / View，并管理 Cubism Framework 的初始化与释放。
 */
class LAppDelegate private constructor() {

    var activity: Activity? = null
        private set
    var view: LAppView? = null
        private set

    private var isFrameworkStarted = false

    fun onStart(activity: Activity) {
        this.activity = activity
        startFrameworkIfNeeded()
        LAppPal.logInfo("LAppDelegate.onStart")
    }

    private fun startFrameworkIfNeeded() {
        if (isFrameworkStarted) return
        // 初始化 Cubism Framework（方法签名以 SDK 版本为准，官方为 CubismFramework.StartUp()）
        CubismFramework.StartUp()
        isFrameworkStarted = true
    }

    fun onPause() {
        // 需要时在此挂起动作/计时
    }

    fun onStop() {
        // 需要时在此处理
    }

    fun onDestroy() {
        if (isFrameworkStarted) {
            // 释放 Cubism Framework（官方为 CubismFramework.Dispose()）
            CubismFramework.Dispose()
            isFrameworkStarted = false
        }
        activity = null
        view = null
    }

    fun attachView(v: LAppView) {
        view = v
    }

    companion object {
        @Volatile
        private var instance: LAppDelegate? = null

        fun getInstance(): LAppDelegate =
            instance ?: synchronized(this) {
                instance ?: LAppDelegate().also { instance = it }
            }
    }
}
