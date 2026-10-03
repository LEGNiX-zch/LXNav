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

        // 抽屉昵称/开关与 Prefs 同步
        binding.toolbarDrawer.setNickname(Prefs.nickname)
        binding.toolbarDrawer.syncToggles(incognito, isNightModeOn())

        // 启动时创建第一个标签（空白主页）
        tabManager.newTab(null)
        updateMultiTaskPanel()
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

    private fun setupDynamicIsland() {
        binding.dynamicIsland.setListener(object : DynamicIslandView.Listener {
            override fun onLongPress() {
                openMultiTask()
            }
        })
    }

    private fun setupSearchBar() {
        binding.searchBar.onSearch = { url ->
            loadInCurrentTab(url)
        }
    }

    /** 加载 URL，并在历史开启时记录；无痕模式下不记录 */
    private fun loadInCurrentTab(url: String) {
        tabManager.currentTab?.loadUrl(url)
        binding.dynamicIsland.setPageInfo("", url)
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
                    closeMultiTask()
                    tabManager.newTab(null)
                }
                updateMultiTaskPanel()
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
        binding.toolbarDrawer.setNickname(Prefs.nickname)
        binding.toolbarDrawer.syncToggles(incognito, isNightModeOn())

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