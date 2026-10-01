package com.legnix.lxnav.web

import android.content.Context
import android.graphics.Bitmap
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

    init {
        setupWebView()
    }

    private fun setupWebView() {
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
                return false // 不拦截，交给 WebView 自己加载
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                if (url != null) {
                    tab.url = url
                    onUrlChanged?.invoke(url)
                }
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
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView?, newTitle: String?) {
                if (!newTitle.isNullOrBlank()) {
                    tab.title = newTitle
                    onTitleChanged?.invoke(newTitle)
                }
            }
        }

        // 初始尺寸
        webView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    fun applyUa(settings: WebSettings = webView.settings) {
        settings.userAgentString = when (Prefs.uaMode) {
            Prefs.UaMode.MOBILE -> {
                // 保留 WebView 默认 UA（手机版）
                WebSettings.getDefaultUserAgent(context)
            }
            Prefs.UaMode.DESKTOP -> {
                // 桌面 UA
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            }
        }
    }

    fun applyZoom(settings: WebSettings = webView.settings) {
        settings.textZoom = Prefs.defaultZoom
    }

    fun loadUrl(targetUrl: String) {
        tab.url = targetUrl
        webView.loadUrl(targetUrl)
    }

    /** 空白主页：加载 LEGNIX 标识页 */
    fun loadHomePage() {
        tab.url = "about:blank"
        webView.loadData(
            homePageHtml(),
            "text/html",
            "UTF-8"
        )
    }

    private fun homePageHtml(): String {
        return """
            <html><head><meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
              body{margin:0;display:flex;align-items:center;justify-content:center;
                  height:100vh;background:transparent;font-family:sans-serif;}
              .logo{font-size:42px;font-weight:800;letter-spacing:0.15em;color:#2D6CF6;
                    text-shadow:0 1px 3px rgba(0,0,0,0.15);}
            </style></head>
            <body><div class="logo">LEGNIX</div></body></html>
        """.trimIndent()
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
        /** 清除全局 WebView 缓存 */
        fun clearCache(context: Context) {
            WebView(context).clearCache(true)
        }

        /** 清除全局 Cookie */
        fun clearCookies() {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        }
    }
}
