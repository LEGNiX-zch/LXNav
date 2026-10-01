package com.legnix.lxnav.data

import android.content.Context
import android.content.SharedPreferences

/**
 * SharedPreferences 统一封装，管理所有用户设置项。
 *
 * 设置项：
 * - maxTabs：最大标签数量（1/2/3/4）
 * - theme：主题（liquid_glass / dark / light）
 * - defaultZoom：默认网页缩放（百分比）
 * - uaMode：UA 模式（mobile / desktop）
 * - animationsEnabled：动画总开关
 * - screenShapeOverride：屏幕形状覆盖（auto / square / round）
 */
object Prefs {

    private const val FILE_NAME = "lxnav_prefs"

    private const val KEY_MAX_TABS = "max_tabs"
    private const val KEY_THEME = "theme"
    private const val KEY_DEFAULT_ZOOM = "default_zoom"
    private const val KEY_UA_MODE = "ua_mode"
    private const val KEY_ANIMATIONS = "animations_enabled"
    private const val KEY_SCREEN_OVERRIDE = "screen_override"

    enum class Theme(val key: String, val label: String) {
        LIQUID_GLASS("liquid_glass", "液态玻璃"),
        DARK("dark", "深色"),
        LIGHT("light", "浅色")
    }

    enum class UaMode(val key: String, val label: String) {
        MOBILE("mobile", "手机网页"),
        DESKTOP("desktop", "桌面网页")
    }

    enum class ScreenOverride(val key: String, val label: String) {
        AUTO("auto", "自动检测"),
        SQUARE("square", "强制方形"),
        ROUND("round", "强制圆形")
    }

    private fun sp(context: Context): SharedPreferences =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    var maxTabs: Int = 3
        private set

    var theme: Theme = Theme.LIQUID_GLASS
        private set

    var defaultZoom: Int = 100
        private set

    var uaMode: UaMode = UaMode.MOBILE
        private set

    var animationsEnabled: Boolean = true
        private set

    var screenOverride: ScreenOverride = ScreenOverride.AUTO
        private set

    /** 从 SharedPreferences 加载所有设置 */
    fun load(context: Context) {
        val sp = sp(context)
        maxTabs = sp.getInt(KEY_MAX_TABS, 3)
        theme = Theme.entries.firstOrNull { it.key == sp.getString(KEY_THEME, "") }
            ?: Theme.LIQUID_GLASS
        defaultZoom = sp.getInt(KEY_DEFAULT_ZOOM, 100)
        uaMode = UaMode.entries.firstOrNull { it.key == sp.getString(KEY_UA_MODE, "") }
            ?: UaMode.MOBILE
        animationsEnabled = sp.getBoolean(KEY_ANIMATIONS, true)
        screenOverride = ScreenOverride.entries.firstOrNull { it.key == sp.getString(KEY_SCREEN_OVERRIDE, "") }
            ?: ScreenOverride.AUTO
    }

    fun setMaxTabs(context: Context, value: Int) {
        maxTabs = value
        sp(context).edit().putInt(KEY_MAX_TABS, value).apply()
    }

    fun setTheme(context: Context, value: Theme) {
        theme = value
        sp(context).edit().putString(KEY_THEME, value.key).apply()
    }

    fun setDefaultZoom(context: Context, value: Int) {
        defaultZoom = value
        sp(context).edit().putInt(KEY_DEFAULT_ZOOM, value).apply()
    }

    fun setUaMode(context: Context, value: UaMode) {
        uaMode = value
        sp(context).edit().putString(KEY_UA_MODE, value.key).apply()
    }

    fun setAnimationsEnabled(context: Context, value: Boolean) {
        animationsEnabled = value
        sp(context).edit().putBoolean(KEY_ANIMATIONS, value).apply()
    }

    fun setScreenOverride(context: Context, value: ScreenOverride) {
        screenOverride = value
        sp(context).edit().putString(KEY_SCREEN_OVERRIDE, value.key).apply()
    }
}
