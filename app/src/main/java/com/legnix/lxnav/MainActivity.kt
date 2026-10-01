package com.legnix.lxnav

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.legnix.lxnav.data.model.Tab
import com.legnix.lxnav.databinding.ActivityMainBinding
import com.legnix.lxnav.view.DynamicIslandView
import com.legnix.lxnav.view.TabStackPanel

/**
 * 主界面（阶段二）。
 * 灵动岛 + 堆叠卡片多任务面板联动。
 *
 * 交互链路：
 * - 长按灵动岛 → 胶囊弹性拉长 → 弹出多任务面板
 * - 面板内上下滑动浏览卡片 / 左右滑关闭 / 点击切换
 * - 关闭面板 → 胶囊弹性收缩
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
            binding.tabStackPanel.visibility == android.view.View.VISIBLE -> closeMultiTask()
            else -> {
                @Suppress("DEPRECATION")
                super.onBackPressed()
            }
        }
    }
}
