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
 * - nickname：抽屉头部显示的用户昵称（可编辑）
 * - islandPureBlack：灵动岛纯黑模式（第二阶段新增，默认开启）
 * - historyEnabled：是否记录浏览历史（第二阶段新增）
 */
object Prefs {

    private const val FILE_NAME = "lxnav_prefs"

    private const val KEY_MAX_TABS = "max_tabs"
    private const val KEY_THEME = "theme"
    private const val KEY_DEFAULT_ZOOM = "default_zoom"
    private const val KEY_UA_MODE = "ua_mode"
    private const val KEY_ANIMATIONS = "animations_enabled"
    private const val KEY_SCREEN_OVERRIDE = "screen_override"
    private const val KEY_NICKNAME = "nickname"
    private const val KEY_ISLAND_PURE_BLACK = "island_pure_black"
    private const val KEY_HISTORY_ENABLED = "history_enabled"
    // ===== 新增（本轮功能迭代） =====
    private const val KEY_AVATAR_PATH = "avatar_path"       // 需求 2：自定义头像本地路径
    private const val KEY_SEARCH_ENGINE = "search_engine"   // 需求 10：搜索引擎
    private const val KEY_CUSTOM_BG_PATH = "custom_bg_path" // 需求 11：自定义背景本地路径

    /** 默认昵称 */
    const val DEFAULT_NICKNAME = "FLENX 用户"

    /**
     * 需求 10：搜索引擎枚举。
     * queryUrl 为关键词搜索地址前缀，用户输入的关键词会 URL 编码后拼接其后。
     */
    enum class SearchEngine(val key: String, val label: String, val queryUrl: String) {
        BAIDU("baidu", "百度", "https://www.baidu.com/s?wd="),
        BING("bing", "必应", "https://cn.bing.com/search?q="),
        SO360("so360", "360", "https://www.so.com/s?q="),
        SOGOU("sogou", "搜狗", "https://www.sogou.com/web?query=")
    }

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

    var nickname: String = DEFAULT_NICKNAME
        private set

    var islandPureBlack: Boolean = true
        private set

    var historyEnabled: Boolean = true
        private set

    // ===== 新增（本轮功能迭代） =====
    /** 需求 2：自定义头像本地文件路径（空 = 使用默认占位图） */
    var avatarPath: String = ""
        private set

    /** 需求 10：当前搜索引擎（默认百度） */
    var searchEngine: SearchEngine = SearchEngine.BAIDU
        private set

    /** 需求 11：自定义主页背景本地文件路径（空 = 不使用自定义背景） */
    var customBgPath: String = ""
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
        nickname = sp.getString(KEY_NICKNAME, DEFAULT_NICKNAME)?.takeIf { it.isNotBlank() }
            ?: DEFAULT_NICKNAME
        islandPureBlack = sp.getBoolean(KEY_ISLAND_PURE_BLACK, true)
        historyEnabled = sp.getBoolean(KEY_HISTORY_ENABLED, true)
        // ===== 新增（本轮功能迭代） =====
        avatarPath = sp.getString(KEY_AVATAR_PATH, "") ?: ""
        searchEngine = SearchEngine.entries.firstOrNull { it.key == sp.getString(KEY_SEARCH_ENGINE, "") }
            ?: SearchEngine.BAIDU
        customBgPath = sp.getString(KEY_CUSTOM_BG_PATH, "") ?: ""
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

    fun setNickname(context: Context, value: String) {
        nickname = value.ifBlank { DEFAULT_NICKNAME }
        sp(context).edit().putString(KEY_NICKNAME, nickname).apply()
    }

    fun setIslandPureBlack(context: Context, value: Boolean) {
        islandPureBlack = value
        sp(context).edit().putBoolean(KEY_ISLAND_PURE_BLACK, value).apply()
    }

    fun setHistoryEnabled(context: Context, value: Boolean) {
        historyEnabled = value
        sp(context).edit().putBoolean(KEY_HISTORY_ENABLED, value).apply()
    }

    // ===== 新增（本轮功能迭代） =====
    /** 需求 2：设置自定义头像路径（空字符串 = 清除，恢复默认占位图） */
    fun setAvatarPath(context: Context, value: String) {
        avatarPath = value
        sp(context).edit().putString(KEY_AVATAR_PATH, value).apply()
    }

    /** 需求 10：设置当前搜索引擎 */
    fun setSearchEngine(context: Context, value: SearchEngine) {
        searchEngine = value
        sp(context).edit().putString(KEY_SEARCH_ENGINE, value.key).apply()
    }

    /** 需求 11：设置自定义主页背景路径（空字符串 = 清除） */
    fun setCustomBgPath(context: Context, value: String) {
        customBgPath = value
        sp(context).edit().putString(KEY_CUSTOM_BG_PATH, value).apply()
    }
}