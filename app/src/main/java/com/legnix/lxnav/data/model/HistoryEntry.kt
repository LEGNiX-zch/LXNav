package com.legnix.lxnav.data.model

/**
 * 一条浏览历史记录。
 *
 * @param id        唯一标识（创建时的毫秒时间戳）
 * @param title     页面标题
 * @param url       页面地址
 * @param visitedAt 访问时间（毫秒）
 */
data class HistoryEntry(
    val id: Long,
    var title: String,
    val url: String,
    val visitedAt: Long
)
