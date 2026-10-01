package com.legnix.lxnav.util

import com.legnix.lxnav.data.Prefs
import java.net.URLEncoder

/**
 * 搜索引擎工具。
 *
 * 根据 Prefs.engine 生成对应搜索结果页 URL。
 * 支持：必应 / 百度 / 360 / 搜狗 / DuckDuckGo，默认必应。
 *
 * 判断规则（参考 ESearch）：
 * - 包含 "://" → 直接作为 URL
 * - 包含 "." 且不含空格 → 当作域名，补 https://
 * - 其他 → 走搜索引擎
 */
object SearchEngine {

    fun toUrl(input: String): String {
        val q = input.trim()
        if (q.isEmpty()) return q
        // 直接 URL
        if (q.contains("://")) return q
        // 域名特征（含 . 且无空格）
        if (q.contains(".") && !q.contains(" ")) {
            return "https://$q"
        }
        // 搜索词
        return searchUrl(q)
    }

    private fun searchUrl(query: String): String {
        val encoded = URLEncoder.encode(query, "UTF-8")
        return when (Prefs.engine) {
            Prefs.Engine.BING -> "https://www.bing.com/search?q=$encoded"
            Prefs.Engine.BAIDU -> "https://www.baidu.com/s?wd=$encoded"
            Prefs.Engine.SO360 -> "https://www.so.com/s?q=$encoded"
            Prefs.Engine.SOGOU -> "https://www.sogou.com/web?query=$encoded"
            Prefs.Engine.DUCKDUCKGO -> "https://duckduckgo.com/?q=$encoded"
        }
    }
}
