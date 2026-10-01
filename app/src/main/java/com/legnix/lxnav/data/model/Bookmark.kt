package com.legnix.lxnav.data.model

/**
 * 收藏书签数据模型。
 */
data class Bookmark(
    val id: Long,
    val title: String,
    val url: String,
    val createdAt: Long
)
