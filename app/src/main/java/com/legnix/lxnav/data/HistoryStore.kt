package com.legnix.lxnav.data

import android.content.Context
import com.legnix.lxnav.data.model.HistoryEntry

/**
 * 浏览历史存储。
 *
 * 与 BookmarkStore 保持同一套"轻量手写序列化"风格：
 * - 用不可见字符 \u0001 作为字段分隔符，\n 作为行分隔符；
 * - 单字段内可能出现分隔符时做转义，避免解析错位。
 *
 * 结构：lines 中每行 = "id\u0001title\u0001url\u0001visitedAt"
 */
object HistoryStore {

    private const val FILE_NAME = "lxnav_history"
    private const val KEY_LIST = "history_list"

    /** 最多保留的历史条数，超出时丢弃最旧的 */
    private const val MAX_ENTRIES = 500

    private val SEP_FIELD = '\u0001'
    private val SEP_LINE = '\n'
    private val ESCAPE = '\u0002'

    /** 读取全部历史，按访问时间倒序（最近在前） */
    fun getAll(context: Context): List<HistoryEntry> {
        val raw = prefs(context).getString(KEY_LIST, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(SEP_LINE)
            .asSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { parseLine(it) }
            .sortedByDescending { it.visitedAt }
            .toList()
    }

    /** 追加一条历史记录（相同 URL 去重，只保留最新一条） */
    fun add(context: Context, title: String, url: String) {
        if (url.isBlank()) return
        val existing = getAll(context)
            .filterNot { it.url == url }
            .toMutableList()
        existing.add(0, HistoryEntry(System.currentTimeMillis(), title, url))
        save(context, existing.take(MAX_ENTRIES))
    }

    /** 删除单条历史 */
    fun remove(context: Context, entry: HistoryEntry) {
        val list = getAll(context).filterNot { it.id == entry.id }
        save(context, list)
    }

    /** 清空全部历史 */
    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_LIST).apply()
    }

    // ---- 内部 ----

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    private fun save(context: Context, list: List<HistoryEntry>) {
        val raw = list.joinToString(SEP_LINE.toString()) { encodeLine(it) }
        prefs(context).edit().putString(KEY_LIST, raw).apply()
    }

    private fun encodeLine(e: HistoryEntry): String {
        return listOf(
            e.id.toString(),
            escape(e.title),
            escape(e.url),
            e.visitedAt.toString()
        ).joinToString(SEP_FIELD.toString())
    }

    private fun parseLine(line: String): HistoryEntry? {
        val parts = line.split(SEP_FIELD)
        if (parts.size < 4) return null
        val id = parts[0].toLongOrNull() ?: return null
        val title = unescape(parts[1])
        val url = unescape(parts[2])
        val visitedAt = parts[3].toLongOrNull() ?: 0L
        return HistoryEntry(id, title, url, visitedAt)
    }

    private fun escape(s: String): String =
        s.replace(ESCAPE.toString(), ESCAPE.toString() + ESCAPE)
            .replace(SEP_FIELD.toString(), ESCAPE.toString() + "f")
            .replace(SEP_LINE.toString(), ESCAPE.toString() + "n")

    private fun unescape(s: String): String {
        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == ESCAPE && i + 1 < s.length) {
                when (s[i + 1]) {
                    'f' -> { sb.append(SEP_FIELD); i += 2; continue }
                    'n' -> { sb.append(SEP_LINE); i += 2; continue }
                    ESCAPE -> { sb.append(ESCAPE); i += 2; continue }
                }
            }
            sb.append(c)
            i++
        }
        return sb.toString()
    }
}
