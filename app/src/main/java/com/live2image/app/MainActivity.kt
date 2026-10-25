package com.live2image.app

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

/**
 * 入口 Activity：常亮，并把 Activity 生命周期桥接到 LAppDelegate / LAppView。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var lappView: LAppView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)
        lappView = findViewById(R.id.lapp_view)
        LAppDelegate.getInstance().onStart(this)
    }

    override fun onResume() {
        super.onResume()
        lappView.onResume()
    }

    override fun onPause() {
        lappView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        LAppDelegate.getInstance().onDestroy()
        super.onDestroy()
    }
}
