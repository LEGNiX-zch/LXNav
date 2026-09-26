package com.legnix.lxnav

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.legnix.lxnav.databinding.ActivityMainBinding
import com.legnix.lxnav.view.DynamicIslandView

/**
 * 主界面（阶段一骨架）。
 * 目前仅展示 LEGNIX 文字 + 灵动岛胶囊，验证 DynamicIslandView 行为：
 * - 单击胶囊：时间 / 剩余内存 / 页面信息 循环
 * - 长按胶囊：弹性拉长（后续接入多任务面板）
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.dynamicIsland.setListener(object : DynamicIslandView.Listener {
            override fun onLongPress() {
                // TODO 阶段五：弹出堆叠卡片多任务面板
            }
        })
    }
}
