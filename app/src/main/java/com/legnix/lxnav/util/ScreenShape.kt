package com.legnix.lxnav.util

import android.content.Context

/**
 * 手表屏幕形状检测。
 *
 * 优先使用 Configuration.isScreenRound（Wear OS 可靠）。
 * 对于 OPPO Watch 一代等非标准 WearOS 设备，该值可能恒为方形，
 * 因此 Prefs 中提供"自动/强制方形/强制圆形"手动覆盖兜底。
 */
object ScreenShape {

    enum class Shape { SQUARE, ROUND }

    /** 仅依据系统配置自动判断 */
    fun detectAuto(context: Context): Shape {
        val cfg = context.resources.configuration
        return if (cfg.isScreenRound) Shape.ROUND else Shape.SQUARE
    }

    /**
     * 圆形屏幕需要向内预留的安全边距（px），防止 UI 被表盘裁切。
     * 方形屏幕返回 0。
     */
    fun safePaddingPx(context: Context, shape: Shape): Int {
        return if (shape == Shape.ROUND) {
            // 约屏幕短边的 10% 作为安全内缩
            val dm = context.resources.displayMetrics
            (minOf(dm.widthPixels, dm.heightPixels) * 0.10f).toInt()
        } else {
            0
        }
    }
}
