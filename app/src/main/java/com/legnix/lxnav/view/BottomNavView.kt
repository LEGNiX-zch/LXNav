package com.legnix.lxnav.view

import android.content.Context
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import com.legnix.lxnav.R

/**
 * 底部液态玻璃悬浮导航条。
 *
 * 包含：后退、刷新、前进 三个操作按钮。
 * 毛玻璃半透明效果：API 31+ 用 RenderEffect 模糊，API 26-30 用半透明渐变模拟。
 * 仅在网页浏览模式显示，主页自动隐藏。
 */
class BottomNavView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    interface Listener {
        fun onBack()
        fun onRefresh()
        fun onForward()
    }

    private var listener: Listener? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.view_bottom_nav, this, true)

        // 液态玻璃背景
        setBackgroundResource(R.drawable.bg_liquid_glass)
        elevation = (8 * resources.displayMetrics.density)

        // API 31+ 叠加硬件模糊，透出下方网页
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setRenderEffect(
                RenderEffect.createBlurEffect(18f, 18f, Shader.TileMode.CLAMP)
            )
        }

        findViewById<ImageView>(R.id.btnNavBack).setOnClickListener { listener?.onBack() }
        findViewById<ImageView>(R.id.btnNavRefresh).setOnClickListener { listener?.onRefresh() }
        findViewById<ImageView>(R.id.btnNavForward).setOnClickListener { listener?.onForward() }
    }

    fun setListener(l: Listener?) {
        listener = l
    }

    fun show() {
        visibility = View.VISIBLE
        alpha = 0f
        animate().alpha(1f).setDuration(200).start()
    }

    fun hide() {
        animate().alpha(0f).setDuration(150).withEndAction {
            visibility = View.GONE
        }.start()
    }
}
