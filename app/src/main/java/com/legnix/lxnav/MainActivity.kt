package com.legnix.lxnav

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatActivity
import com.legnix.lxnav.data.BookmarkStore
import com.legnix.lxnav.data.HistoryStore
import com.legnix.lxnav.data.Prefs
import com.legnix.lxnav.databinding.ActivityMainBinding
import com.legnix.lxnav.ui.bookmarks.BookmarksActivity
import com.legnix.lxnav.ui.history.HistoryActivity
import com.legnix.lxnav.ui.settings.SettingsActivity
import com.legnix.lxnav.view.DynamicIslandView
import com.legnix.lxnav.view.TabStackPanel
import com.legnix.lxnav.view.ToolbarDrawerView
import com.legnix.lxnav.web.BrowserTab
import com.legnix.lxnav.web.TabManager

/**
 * 主界面（第二阶段完整版）。
 *
 * 功能：灵动岛 + 搜索栏 + WebView + 标签管理 + 抽屉工具栏（头像/昵称/八项菜单）
 *      + 多任务面板 + 历史记录 + 无痕模式 + 夜间模式。
 *
 * 抽屉 14 项回调：
 *   onBack / onForward / onRefresh / onHistory / onDownloads / onShowBookmarks /
 *   onAddBookmark / onToggleIncognito / onToggleNightMode / onTranslate / onOffline /
 *   onNicknameChanged / onDismiss（+ 内部 isOpen 判断）。
 *
 * 返回键逻辑（优先级）：
 * 1. 抽屉打开 → 关闭抽屉
 * 2. 多任务面板打开 → 关闭面板
 * 3. WebView 可后退 → 网页后退
 * 4. 有多个标签 → 关闭当前标签
 * 5. 最后一个标签 → 关闭标签并退出APP
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var tabManager: TabManager

    /** 无痕模式开关（不记录历史、退出清缓存 Cookie） */
    private var incognito = false

    private val bookmarkLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val url = result.data?.getStringExtra("url")
            if (!url.isNullOrBlank()) {
                loadInCurrentTab(url)
            }
        }
    }

    private val historyLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val url = result.data?.getStringExtra("url")
            if (!url.isNullOrBlank()) {
                loadInCurrentTab(url)
            }
        }
    }

    /** 需求 2：系统图片选择器（选择抽屉头像） */
    private val avatarPicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            binding.toolbarDrawer.onAvatarPicked(uri, this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.load(this)
        applyThemeFromPrefs()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tabManager = TabManager(this, binding.webContainer)

        applyAnimationSetting()

        setupDynamicIsland()
        setupSearchBar()
        setupToolbarDrawer()
        setupSettingsButton()
        setupTabStackPanel()

        // 需求 2：把图片选择器注入抽屉
        binding.toolbarDrawer.avatarPickerLauncher = avatarPicker

        // 抽屉昵称/开关与 Prefs 同步
        binding.toolbarDrawer.setNickname(Prefs.nickname)
        binding.toolbarDrawer.syncToggles(incognito, isNightModeOn())

        // 需求 12：冷启动不自动创建 about:blank 空白标签、不弹多任务预览，
        // 直接加载原生主页（灵动岛 + 搜索框 + Logo + 自定义背景）。
        binding.dynamicIsland.setPageInfo("", HOME_URL)
        updateMultiTaskPanel()
        updateHomeVisibility()
    }

    companion object {
        /** 需求 12：应用内主页标识，非 about:blank，用于区分原生主页 */
        const val HOME_URL = "lxnav://home"
    }

    /** 依据 Prefs.theme 应用深/浅色模式（第二阶段：深色模式生效） */
    private fun applyThemeFromPrefs() {
        val mode = when (Prefs.theme) {
            Prefs.Theme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            Prefs.Theme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            Prefs.Theme.LIQUID_GLASS -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    private fun isNightModeOn(): Boolean {
        val cfg = resources.configuration
        val nightMask = cfg.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return nightMask == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    private fun applyAnimationSetting() {
        val enabled = Prefs.animationsEnabled
        binding.dynamicIsland.animationsEnabled = enabled
        binding.tabStackPanel.animationsEnabled = enabled
        binding.toolbarDrawer.animationsEnabled = enabled
    }

    /**
     * 需求 11：应用自定义主页背景。
     * 若已选择背景图且文件存在，则把图片设置到根布局；否则恢复默认背景色。
     * 背景仅在原生主页（无网页标签占满内容区）时可见。
     */
    private fun applyCustomBackground() {
        val path = Prefs.customBgPath
        val file = java.io.File(path)
        if (path.isNotBlank() && file.exists()) {
            try {
                val bmp = android.graphics.BitmapFactory.decodeFile(path)
                if (bmp != null) {
                    binding.rootView.setBackground(
                        android.graphics.drawable.BitmapDrawable(resources, bmp)
                    )
                    return
                }
            } catch (e: Exception) {
                // 解码失败则回退默认背景
            }
        }
        binding.rootView.setBackgroundResource(R.color.bg_page)
    }

    /** 需求 6：Logo 与搜索栏仅在原生主页（无标签或标签为原生主页）时展示 */
    private fun updateHomeVisibility() {
        val isHome = tabManager.currentTab == null ||
                tabManager.currentTab?.url.isNullOrBlank() ||
                tabManager.currentTab?.url == HOME_URL
        binding.logoArea.visibility = if (isHome) View.VISIBLE else View.GONE
    }

    private fun setupDynamicIsland() {
        binding.dynamicIsland.setListener(object : DynamicIslandView.Listener {
            override fun onLongPress() {
                openMultiTask()
            }
        })
    }

    private fun setupSearchBar() {
        binding.searchBar.onSearch = { input ->
            // 需求 10：网址直访；关键词用当前选中的搜索引擎
            loadInCurrentTab(input)
        }
    }

    /** 加载 URL，并在历史开启时记录；无痕模式下不记录 */
    private fun loadInCurrentTab(url: String) {
        // 需求 12：懒创建 —— 首个真实加载时才建立 WebView 标签，
        // 冷启动停留在原生主页不会产生 about:blank 标签。
        tabManager.ensureCurrentTab().loadUrl(url)
        binding.dynamicIsland.setPageInfo("", url)
        updateHomeVisibility()
        if (!incognito && Prefs.historyEnabled) {
            HistoryStore.add(this, url, url)
        }
    }

    private fun setupToolbarDrawer() {
        binding.btnMenu.setOnClickListener {
            if (!binding.toolbarDrawer.isOpen()) binding.toolbarDrawer.open()
        }
        binding.toolbarDrawer.setListener(object : ToolbarDrawerView.Listener {
            override fun onBack() {
                tabManager.goBack()
            }

            override fun onForward() {
                tabManager.goForward()
            }

            override fun onRefresh() {
                tabManager.reload()
            }

            override fun onHistory() {
                if (!Prefs.historyEnabled) {
                    toast(getString(R.string.history_disabled))
                    return
                }
                historyLauncher.launch(Intent(this@MainActivity, HistoryActivity::class.java))
            }

            override fun onDownloads() {
                // 打开系统下载管理器界面（轻量实现）
                try {
                    startActivity(Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS))
                } catch (e: Exception) {
                    toast(getString(R.string.menu_downloads))
                }
            }

            override fun onShowBookmarks() {
                bookmarkLauncher.launch(Intent(this@MainActivity, BookmarksActivity::class.java))
            }

            override fun onAddBookmark() {
                val tab = tabManager.currentTab ?: return
                val url = tab.url
                if (url.isBlank() || url == "about:blank") {
                    toast(getString(R.string.bookmark_exists))
                    return
                }
                BookmarkStore.add(this@MainActivity, tab.title.ifEmpty { url }, url)
                toast(getString(R.string.bookmark_added))
            }

            override fun onToggleIncognito(currentlyOn: Boolean) {
                incognito = currentlyOn
                toast(
                    if (currentlyOn) getString(R.string.toast_incognito_on)
                    else getString(R.string.toast_incognito_off)
                )
            }

            override fun onToggleNightMode(currentlyOn: Boolean) {
                // currentlyOn 为抽屉内部切换后的新状态
                Prefs.setTheme(
                    this@MainActivity,
                    if (currentlyOn) Prefs.Theme.DARK else Prefs.Theme.LIGHT
                )
                applyThemeFromPrefs()
                toast(
                    if (currentlyOn) getString(R.string.toast_night_on)
                    else getString(R.string.toast_night_off)
                )
            }

            override fun onTranslate() {
                // 轻量实现：用 Google 翻译代理页打开当前 URL
                val url = tabManager.currentTab?.url ?: return
                if (url.isBlank() || url.startsWith("about:")) {
                    toast(getString(R.string.menu_translate))
                    return
                }
                val translateUrl = "https://translate.google.com/translate?sl=auto&tl=zh-CN&u=" +
                        Uri.encode(url)
                loadInCurrentTab(translateUrl)
            }

            override fun onOffline() {
                // 轻量实现：将当前页保存到本地文件（离线快照）
                val tab = tabManager.currentTab ?: return
                val url = tab.url
                if (url.isBlank() || url.startsWith("about:")) {
                    toast(getString(R.string.menu_offline))
                    return
                }
                saveOfflineSnapshot(url)
            }

            override fun onNicknameChanged(newNickname: String) {
                Prefs.setNickname(this@MainActivity, newNickname)
            }

            override fun onAvatarChanged(newPath: String) {
                // 需求 2：抽屉内部已负责刷新头像显示，这里只做持久化
                Prefs.setAvatarPath(this@MainActivity, newPath)
            }

            override fun onDismiss() {
                // 抽屉关闭回调（预留）
            }
        })
    }

    /** 离线保存：把当前标题/URL 写入应用私有文件，作为离线书签快照 */
    private fun saveOfflineSnapshot(url: String) {
        try {
            val dir = java.io.File(filesDir, "offline")
            if (!dir.exists()) dir.mkdirs()
            val name = "snapshot_" + System.currentTimeMillis() + ".txt"
            java.io.File(dir, name).writeText(url)
            toast(getString(R.string.menu_offline))
        } catch (e: Exception) {
            toast(getString(R.string.menu_offline))
        }
    }

    private fun setupSettingsButton() {
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun setupTabStackPanel() {
        binding.tabStackPanel.setListener(object : TabStackPanel.Listener {
            override fun onTabSelected(tab: com.legnix.lxnav.data.model.Tab) {
                tabManager.switchToById(tab.id)
                closeMultiTask()
            }

            override fun onTabClosed(tab: com.legnix.lxnav.data.model.Tab) {
                tabManager.closeTab(tab.id)
                if (tabManager.tabCount == 0) {
                    // 需求 12：关闭最后一个标签后回到原生主页，不建 about:blank 标签
                    closeMultiTask()
                    binding.dynamicIsland.setPageInfo("", HOME_URL)
                }
                updateMultiTaskPanel()
                updateHomeVisibility()
            }

            override fun onDismiss() {
                closeMultiTask()
            }
        })
    }

    private fun openMultiTask() {
        binding.dynamicIsland.stretch()
        updateMultiTaskPanel()
        binding.tabStackPanel.show()
    }

    private fun closeMultiTask() {
        binding.tabStackPanel.hide()
        binding.dynamicIsland.shrink()
    }

    private fun updateMultiTaskPanel() {
        binding.tabStackPanel.setTabs(tabManager.tabs, tabManager.getCurrentIndex())
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        when {
            binding.toolbarDrawer.isOpen() -> binding.toolbarDrawer.close()
            binding.tabStackPanel.visibility == View.VISIBLE -> closeMultiTask()
            tabManager.goBack() -> { /* 网页后退成功 */ }
            tabManager.tabCount > 1 -> {
                tabManager.currentTab?.let { tabManager.closeTab(it.tab.id) }
                updateMultiTaskPanel()
            }
            else -> {
                tabManager.destroyAll()
                if (incognito) {
                    com.legnix.lxnav.web.BrowserTab.clearCache(this)
                    com.legnix.lxnav.web.BrowserTab.clearCookies()
                }
                @Suppress("DEPRECATION")
                super.onBackPressed()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 设置可能变更，重新加载
        Prefs.load(this)
        applyAnimationSetting()
        applyThemeFromPrefs()
        binding.dynamicIsland.refreshStyle()
        binding.searchBar.refreshTextColor()
        applyCustomBackground()
        binding.toolbarDrawer.refreshAvatar()
        binding.toolbarDrawer.setNickname(Prefs.nickname)
        binding.toolbarDrawer.syncToggles(incognito, isNightModeOn())
        updateHomeVisibility()

        tabManager.currentTab?.applyUa()
        tabManager.currentTab?.applyZoom()
        tabManager.currentTab?.let { tab ->
            binding.dynamicIsland.setPageInfo(tab.title, tab.url)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tabManager.destroyAll()
    }
}