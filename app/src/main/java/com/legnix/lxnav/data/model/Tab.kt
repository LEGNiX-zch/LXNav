package com.legnix.lxnav.data.model

/**
 * 标签页数据模型。
 * 方案C：不存截图缩略图，多任务卡片仅展示 title + url + 图标占位。
 */
data class Tab(
    val id: String,
    var title: String,
    var url: String
)
