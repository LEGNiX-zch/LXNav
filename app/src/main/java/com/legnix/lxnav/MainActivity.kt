package com.legnix.lxnav

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.legnix.lxnav.data.BookmarkStore
import com.legnix.lxnav.data.Prefs
import com.legnix.lxnav.databinding.ActivityMainBinding
import com.legnix.lxnav.ui.bookmarks.BookmarksActivity
import com.legnix.lxnav.ui.settings.SettingsActivity
import com.legnix.lxnav.view.DynamicIslandView
import com.legnix.lxnav.view.TabStackPanel
import com.legnix.lxnav.view.ToolbarDrawerView
import com.legnix.lxnav.web.TabManager

/**
 * 主界面。
 *
 * 完整功能：灵动岛 + 搜索栏 + WebView + 标签管理 + 抽屉工具栏 + 多任务面板。
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

    private val bookmarkLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val url = result.data?.getStringExtra("url")
            if (url != null) {
                tabManager.currentTab?.loadUrl(url)
                binding.dynamicIsland.setPageInfo("", url)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.load(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tabManager = TabManager(this, binding.webContainer)

        // 应用动画开关到各自定义 View
        applyAnimationSetting()

        setupDynamicIsland()
        setupSearchBar()
        setupToolbarDrawer()
        setupSettingsButton()
        setupTabStackPanel()

        // 启动时创建第一个标签（空白主页）
        tabManager.newTab(null)
        updateMultiTaskPanel()
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
            tabManager.currentTab?.loadUrl(url)
            binding.dynamicIsland.setPageInfo("", url)
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
            override fun onNewTab() {
                tabManager.newTab(null)
                updateMultiTaskPanel()
            }
            override fun onAddBookmark() {
                val tab = tabManager.currentTab ?: return
                BookmarkStore.add(this@MainActivity, tab.title.ifEmpty { tab.url }, tab.url)
            }
            override fun onShowBookmarks() {
                bookmarkLauncher.launch(android.content.Intent(this@MainActivity, BookmarksActivity::class.java))
            }
            override fun onDismiss() {}
        })
    }

    private fun setupSettingsButton() {
        binding.btnSettings.setOnClickListener {
            startActivity(android.content.Intent(this, SettingsActivity::class.java))
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
                    // 无标签时新建一个主页
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

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        when {
            binding.toolbarDrawer.isOpen() -> binding.toolbarDrawer.close()
            binding.tabStackPanel.visibility == View.VISIBLE -> closeMultiTask()
            tabManager.goBack() -> { /* 网页后退成功 */ }
            tabManager.tabCount > 1 -> {
                // 关闭当前标签，切换到上一个
                tabManager.currentTab?.let { tabManager.closeTab(it.tab.id) }
                updateMultiTaskPanel()
            }
            else -> {
                tabManager.destroyAll()
                @Suppress("DEPRECATION")
                super.onBackPressed()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 设置可能变更，重新加载
        applyAnimationSetting()
        tabManager.currentTab?.applyUa()
        tabManager.currentTab?.applyZoom()
        // 更新灵动岛页面信息
        tabManager.currentTab?.let { tab ->
            binding.dynamicIsland.setPageInfo(tab.title, tab.url)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tabManager.destroyAll()
    }
}
