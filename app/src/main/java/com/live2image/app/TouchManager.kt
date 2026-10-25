package com.live2image.app

import android.view.MotionEvent

/**
 * 触摸交互管理：单指拖拽 + 双指缩放（对齐官方 Sample 的 TouchManager）。
 */
class TouchManager {
    private var startX = 0f
    private var startY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var lastTouchDistance = 0f
    private var deltaX = 0f
    private var deltaY = 0f

    private var listener: OnTouchListener? = null

    interface OnTouchListener {
        fun onTouchesBegan()
        fun onTouchesEnded()
        fun onTap(x: Float, y: Float)
        fun onDrag(dx: Float, dy: Float)
        fun onPinch(scale: Float)
    }

    fun setOnTouchListener(l: OnTouchListener) {
        listener = l
    }

    fun touchesBegan(event: MotionEvent): Boolean {
        if (event.pointerCount == 1) {
            startX = lastX = event.getX(0)
            startY = lastY = event.getY(0)
            listener?.onTouchesBegan()
        } else if (event.pointerCount >= 2) {
            lastTouchDistance = distance(event.getX(0), event.getY(0), event.getX(1), event.getY(1))
        }
        return true
    }

    fun touchesMoved(event: MotionEvent): Boolean {
        when (event.pointerCount) {
            1 -> {
                val x = event.getX(0)
                val y = event.getY(0)
                deltaX = x - lastX
                deltaY = y - lastY
                lastX = x
                lastY = y
                if (Math.abs(x - startX) > CLICK_THRESHOLD || Math.abs(y - startY) > CLICK_THRESHOLD) {
                    listener?.onDrag(deltaX, deltaY)
                }
            }
            else -> { // >=2 缩放
                val d = distance(event.getX(0), event.getY(0), event.getX(1), event.getY(1))
                if (lastTouchDistance != 0f && d != 0f) {
                    listener?.onPinch(d / lastTouchDistance)
                }
                lastTouchDistance = d
            }
        }
        return true
    }

    fun touchesEnded(event: MotionEvent) {
        val x = event.x
        val y = event.y
        if (Math.abs(x - startX) < CLICK_THRESHOLD && Math.abs(y - startY) < CLICK_THRESHOLD) {
            listener?.onTap(x, y)
        }
        listener?.onTouchesEnded()
        lastTouchDistance = 0f
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float =
        Math.hypot((x1 - x2).toDouble(), (y1 - y2).toDouble()).toFloat()

    companion object {
        private const val CLICK_THRESHOLD = 12f // px，超过视为拖拽
    }
}
