package com.legnix.lxnav.data

import android.content.Context
import com.legnix.lxnav.data.model.Bookmark

/**
 * 收藏存储，基于 SharedPreferences + JSON 简单序列化。
 * 不引入第三方库，用手动拼装/解析的方式。
 */
object BookmarkStore {

    private const val FILE_NAME = "lxnav_bookmarks"
    private const val KEY_LIST = "list"

    private fun sp(context: Context) =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun getAll(context: Context): List<Bookmark> {
        val raw = sp(context).getString(KEY_LIST, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("\n")
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split("\u0001", limit = 4)
                if (parts.size == 4) {
                    Bookmark(
                        id = parts[0].toLongOrNull() ?: 0L,
                        title = parts[1],
                        url = parts[2],
                        createdAt = parts[3].toLongOrNull() ?: 0L
                    )
                } else null
            }
    }

    fun add(context: Context, title: String, url: String) {
        val list = getAll(context).toMutableList()
        // 避免重复
        if (list.any { it.url == url }) return
        val id = System.currentTimeMillis()
        list.add(Bookmark(id, title, url, id))
        save(context, list)
    }

    fun remove(context: Context, id: Long) {
        val list = getAll(context).filter { it.id != id }
        save(context, list)
    }

    private fun save(context: Context, list: List<Bookmark>) {
        val raw = list.joinToString("\n") { "${it.id}\u0001${it.title}\u0001${it.url}\u0001${it.createdAt}" }
        sp(context).edit().putString(KEY_LIST, raw).apply()
    }
}
