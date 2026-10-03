package com.legnix.lxnav.web

import android.content.Context
import android.view.ViewGroup
import com.legnix.lxnav.data.Prefs
import com.legnix.lxnav.data.model.Tab
import java.util.UUID

/**
 * 标签页管理器。
 *
 * 功能：
 * - 创建 / 切换 / 关闭标签
 * - 内存保护：标签数超过上限时自动销毁最早标签，释放 WebView 资源
 * - 仅当前标签的 WebView 被添加到视图树，其余标签 pause 释放渲染资源
 */
class TabManager(
    private val context: Context,
    private val container: ViewGroup
) {
    private val _tabs = mutableListOf<BrowserTab>()
    val tabs: List<Tab> get() = _tabs.map { it.tab }

    private var currentIndex = -1

    var onTabListChanged: (() -> Unit)? = null
    var onCurrentTabChanged: ((BrowserTab?) -> Unit)? = null

    val currentTab: BrowserTab?
        get() = _tabs.getOrNull(currentIndex)

    val tabCount: Int get() = _tabs.size

    /** 创建新标签并加载 URL（url 为空则加载主页） */
    fun newTab(url: String? = null): BrowserTab {
        // 内存保护：达到上限时销毁最早标签
        while (_tabs.size >= Prefs.maxTabs) {
            destroyOldestTab()
        }
        val tab = Tab(
            id = UUID.randomUUID().toString(),
            title = if (url.isNullOrBlank()) "LEGNIX" else "",
            url = url ?: "about:blank"
        )
        val browserTab = BrowserTab(context, tab)
        setupCallbacks(browserTab)
        _tabs.add(browserTab)
        switchTo(_tabs.lastIndex)
        if (url.isNullOrBlank()) {
            browserTab.loadHomePage()
        } else {
            browserTab.loadUrl(url)
        }
        onTabListChanged?.invoke()
        return browserTab
    }

    private fun setupCallbacks(tab: BrowserTab) {
        tab.onTitleChanged = { title ->
            if (tab == currentTab) {
                onCurrentTabChanged?.invoke(tab)
            }
            onTabListChanged?.invoke()
        }
        tab.onUrlChanged = { url ->
            if (tab == currentTab) {
                onCurrentTabChanged?.invoke(tab)
            }
        }
    }

    /** 切换到指定索引的标签 */
    fun switchTo(index: Int): BrowserTab? {
        if (index < 0 || index >= _tabs.size) return null

        // 移除旧标签 WebView
        currentTab?.let { old ->
            container.removeView(old.webView)
            old.pause()
        }

        currentIndex = index
        val tab = _tabs[index]
        container.addView(tab.webView)
        tab.resume()
        onCurrentTabChanged?.invoke(tab)
        return tab
    }

    /** 按 Tab.id 切换 */
    fun switchToById(id: String): BrowserTab? {
        val idx = _tabs.indexOfFirst { it.tab.id == id }
        return if (idx >= 0) switchTo(idx) else null
    }

    /** 关闭指定标签 */
    fun closeTab(id: String) {
        val idx = _tabs.indexOfFirst { it.tab.id == id }
        if (idx < 0) return

        val tab = _tabs[idx]
        if (idx == currentIndex) {
            container.removeView(tab.webView)
        }
        tab.destroy()
        _tabs.removeAt(idx)

        if (_tabs.isEmpty()) {
            currentIndex = -1
            onCurrentTabChanged?.invoke(null)
        } else {
            // 调整当前索引
            currentIndex = when {
                idx < currentIndex -> currentIndex - 1
                idx == currentIndex -> (idx - 1).coerceAtLeast(0)
                else -> currentIndex
            }
            switchTo(currentIndex.coerceIn(0, _tabs.lastIndex))
        }
        onTabListChanged?.invoke()
    }

    /** 销毁最早的标签（内存保护） */
    private fun destroyOldestTab() {
        if (_tabs.isEmpty()) return
        val oldest = _tabs.first()
        if (_tabs.indexOf(oldest) == currentIndex) {
            container.removeView(oldest.webView)
        }
        oldest.destroy()
        _tabs.removeAt(0)
        currentIndex--
        if (currentIndex < 0 && _tabs.isNotEmpty()) {
            currentIndex = 0
        }
    }

    /** 获取当前索引 */
    fun getCurrentIndex(): Int = currentIndex.coerceAtLeast(0)

    /**
     * 需求 12：确保存在当前标签（懒创建）。
     * 冷启动不建立任何标签，只有真正要加载网页时才调用本方法。
     */
    fun ensureCurrentTab(): BrowserTab {
        return currentTab ?: newTab("about:blank")
    }

    /** 全部销毁（退出时） */
    fun destroyAll() {
        currentTab?.let { container.removeView(it.webView) }
        _tabs.forEach { it.destroy() }
        _tabs.clear()
        currentIndex = -1
    }

    /** 返回键：当前标签网页后退，返回 false 表示无法后退 */
    fun goBack(): Boolean {
        return currentTab?.goBack() ?: false
    }

    /** 网页前进 */
    fun goForward(): Boolean {
        return currentTab?.goForward() ?: false
    }

    /** 刷新当前标签 */
    fun reload() {
        currentTab?.reload()
    }
}
