package com.legnix.lxnav

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.legnix.lxnav.data.model.Tab
import com.legnix.lxnav.databinding.ActivityMainBinding
import com.legnix.lxnav.view.DynamicIslandView
import com.legnix.lxnav.view.TabStackPanel
import com.legnix.lxnav.view.ToolbarDrawerView

/**
 * 主界面（阶段三）。
 * 灵动岛 + 多任务面板 + 左侧抽屉工具栏联动。
 *
 * 返回键逻辑：
 * 1. 抽屉打开 → 优先关闭抽屉
 * 2. 多任务面板打开 → 优先关闭面板
 * 3. 否则 → 网页后退 / 关闭标签 / 退出APP
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    /** TODO 阶段六：接入 TabManager 后由其管理 */
    private val demoTabs = mutableListOf(
        Tab("1", "LXNav 主页", "about:blank"),
        Tab("2", "百度", "https://www.baidu.com"),
        Tab("3", "GitHub", "https://github.com")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDynamicIsland()
        setupTabStackPanel()
        setupToolbarDrawer()
        setupSettingsButton()
    }

    private fun setupDynamicIsland() {
        binding.dynamicIsland.setListener(object : DynamicIslandView.Listener {
            override fun onLongPress() {
                openMultiTask()
            }
        })
    }

    private fun setupTabStackPanel() {
        binding.tabStackPanel.setListener(object : TabStackPanel.Listener {
            override fun onTabSelected(tab: Tab) {
                // TODO 阶段六：切换到该网页
                closeMultiTask()
            }

            override fun onTabClosed(tab: Tab) {
                demoTabs.removeAll { it.id == tab.id }
                if (demoTabs.isEmpty()) {
                    closeMultiTask()
                } else {
                    binding.tabStackPanel.setTabs(demoTabs)
                }
            }

            override fun onDismiss() {
                closeMultiTask()
            }
        })
    }

    private fun setupToolbarDrawer() {
        binding.btnMenu.setOnClickListener {
            if (!binding.toolbarDrawer.isOpen()) binding.toolbarDrawer.open()
        }
        binding.toolbarDrawer.setListener(object : ToolbarDrawerView.Listener {
            override fun onBack() {
                // TODO 阶段六：WebView 后退
            }
            override fun onForward() {
                // TODO 阶段六：WebView 前进
            }
            override fun onRefresh() {
                // TODO 阶段六：WebView 刷新
            }
            override fun onNewTab() {
                // TODO 阶段六：新建标签
            }
            override fun onAddBookmark() {
                // TODO 阶段七：添加收藏
            }
            override fun onShowBookmarks() {
                // TODO 阶段七：打开收藏列表
            }
            override fun onDismiss() {
                // 抽屉收回完成
            }
        })
    }

    private fun setupSettingsButton() {
        binding.btnSettings.setOnClickListener {
            // TODO 阶段五：跳转 SettingsActivity
        }
    }

    private fun openMultiTask() {
        binding.dynamicIsland.stretch()
        binding.tabStackPanel.setTabs(demoTabs)
        binding.tabStackPanel.show()
    }

    private fun closeMultiTask() {
        binding.tabStackPanel.hide()
        binding.dynamicIsland.shrink()
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        when {
            binding.toolbarDrawer.isOpen() -> binding.toolbarDrawer.close()
            binding.tabStackPanel.visibility == View.VISIBLE -> closeMultiTask()
            else -> {
                // TODO 阶段六：WebView 可后退则后退
                // TODO 阶段六：否则关闭标签，再按退出APP
                @Suppress("DEPRECATION")
                super.onBackPressed()
            }
        }
    }
}
