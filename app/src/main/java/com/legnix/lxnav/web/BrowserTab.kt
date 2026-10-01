package com.legnix.lxnav.web

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.legnix.lxnav.data.Prefs
import com.legnix.lxnav.data.model.Tab

/**
 * 单个标签页：封装 WebView + 标签元数据。
 *
 * 内存保护策略：
 * - 当 WebView 不在前台时，调用 onPause() 暂停渲染。
 * - 被回收时调用 destroy() 释放 WebView 资源。
 */
class BrowserTab(
    val context: Context,
    val tab: Tab
) {
    val webView: WebView = WebView(context)

    var title: String
        get() = tab.title
        set(value) { tab.title = value }

    var url: String
        get() = tab.url
        set(value) { tab.url = value }

    var onTitleChanged: ((String) -> Unit)? = null
    var onUrlChanged: ((String) -> Unit)? = null
    var onProgressChanged: ((Int) -> Unit)? = null
    var onPageStarted: (() -> Unit)? = null
    var onPageFinished: ((Boolean) -> Unit)? = null // 参数：是否为主页

    init {
        setupWebView()
    }

    private fun setupWebView() {
        webView.setBackgroundColor(Color.TRANSPARENT)

        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.loadsImagesAutomatically = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.defaultFontSize = 14

        applyUa(settings)
        applyZoom(settings)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                if (url != null) {
                    tab.url = url
                    onUrlChanged?.invoke(url)
                }
                onPageStarted?.invoke()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                if (url != null) {
                    tab.url = url
                    onUrlChanged?.invoke(url)
                }
                view?.title?.let { newTitle ->
                    if (newTitle.isNotBlank()) {
                        tab.title = newTitle
                        onTitleChanged?.invoke(newTitle)
                    }
                }
                onPageFinished?.invoke(isHomePage())
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView?, newTitle: String?) {
                if (!newTitle.isNullOrBlank()) {
                    tab.title = newTitle
                    onTitleChanged?.invoke(newTitle)
                }
            }

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                onProgressChanged?.invoke(newProgress)
            }
        }

        webView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    fun applyUa(settings: WebSettings = webView.settings) {
        settings.userAgentString = when (Prefs.uaMode) {
            Prefs.UaMode.MOBILE -> WebSettings.getDefaultUserAgent(context)
            Prefs.UaMode.DESKTOP ->
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        }
    }

    fun applyZoom(settings: WebSettings = webView.settings) {
        settings.textZoom = Prefs.defaultZoom
    }

    fun loadUrl(targetUrl: String) {
        tab.url = targetUrl
        webView.loadUrl(targetUrl)
    }

    /** 空白主页：透明背景，主页覆盖层显示原生 LEGNIX logo */
    fun loadHomePage() {
        tab.url = "about:blank"
        webView.loadUrl("about:blank")
    }

    fun isHomePage(): Boolean {
        val u = tab.url
        return u.isEmpty() || u == "about:blank"
    }

    fun goBack(): Boolean {
        return if (webView.canGoBack()) {
            webView.goBack()
            true
        } else false
    }

    fun goForward(): Boolean {
        return if (webView.canGoForward()) {
            webView.goForward()
            true
        } else false
    }

    fun reload() {
        webView.reload()
    }

    fun pause() {
        webView.onPause()
    }

    fun resume() {
        webView.onResume()
    }

    fun destroy() {
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.destroy()
    }

    companion object {
        fun clearCache(context: Context) {
            WebView(context).clearCache(true)
        }

        fun clearCookies() {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        }
    }
}
