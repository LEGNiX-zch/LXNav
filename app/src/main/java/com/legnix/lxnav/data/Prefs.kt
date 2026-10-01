package com.legnix.lxnav.data

import android.content.Context
import android.content.SharedPreferences

/**
 * SharedPreferences 统一封装，管理所有用户设置项。
 */
object Prefs {

    private const val FILE_NAME = "lxnav_prefs"

    private const val KEY_MAX_TABS = "max_tabs"
    private const val KEY_THEME = "theme"
    private const val KEY_DEFAULT_ZOOM = "default_zoom"
    private const val KEY_UA_MODE = "ua_mode"
    private const val KEY_ANIMATIONS = "animations_enabled"
    private const val KEY_SCREEN_OVERRIDE = "screen_override"
    private const val KEY_ENGINE = "search_engine"
    private const val KEY_LIQUID_GLASS = "liquid_glass_enabled"
    private const val KEY_BG_TYPE = "custom_bg_type"
    private const val KEY_BG_URI = "custom_bg_uri"
    private const val KEY_UI_MODE = "ui_mode"

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

    /** 搜索引擎 */
    enum class Engine(val key: String, val label: String) {
        BING("bing", "必应"),
        BAIDU("baidu", "百度"),
        SO360("so360", "360搜索"),
        SOGOU("sogou", "搜狗"),
        DUCKDUCKGO("duckduckgo", "DuckDuckGo")
    }

    /** 自定义背景类型 */
    enum class BgType(val key: String, val label: String) {
        NONE("none", "默认"),
        IMAGE("image", "静态壁纸"),
        VIDEO("video", "动态壁纸")
    }

    /** UI 模式：手机端 / 手表端 */
    enum class UiMode(val key: String, val label: String) {
        PHONE("phone", "手机模式"),
        WATCH("watch", "手表模式")
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

    var engine: Engine = Engine.BING
        private set

    var liquidGlassEnabled: Boolean = true
        private set

    var bgType: BgType = BgType.NONE
        private set

    var bgUri: String = ""
        private set

    var uiMode: UiMode = UiMode.WATCH
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
        engine = Engine.entries.firstOrNull { it.key == sp.getString(KEY_ENGINE, "") }
            ?: Engine.BING
        liquidGlassEnabled = sp.getBoolean(KEY_LIQUID_GLASS, true)
        bgType = BgType.entries.firstOrNull { it.key == sp.getString(KEY_BG_TYPE, "") }
            ?: BgType.NONE
        bgUri = sp.getString(KEY_BG_URI, "") ?: ""
        uiMode = UiMode.entries.firstOrNull { it.key == sp.getString(KEY_UI_MODE, "") }
            ?: UiMode.WATCH
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

    fun setEngine(context: Context, value: Engine) {
        engine = value
        sp(context).edit().putString(KEY_ENGINE, value.key).apply()
    }

    fun setLiquidGlassEnabled(context: Context, value: Boolean) {
        liquidGlassEnabled = value
        sp(context).edit().putBoolean(KEY_LIQUID_GLASS, value).apply()
    }

    fun setBg(context: Context, type: BgType, uri: String = "") {
        bgType = type
        bgUri = uri
        sp(context).edit()
            .putString(KEY_BG_TYPE, type.key)
            .putString(KEY_BG_URI, uri)
            .apply()
    }

    fun setUiMode(context: Context, value: UiMode) {
        uiMode = value
        sp(context).edit().putString(KEY_UI_MODE, value.key).apply()
    }
}
