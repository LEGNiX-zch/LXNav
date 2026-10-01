package com.legnix.lxnav.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.legnix.lxnav.R

/**
 * 左侧抽屉式工具栏。
 *
 * - 从屏幕左侧滑入，只弹出半宽面板。
 * - 包含：前进、后退、刷新、新建标签、添加收藏、查看收藏列表。
 * - 点击空白区域 / 返回键 → 抽屉收回隐藏。
 * - 半宽面板 + 右侧半屏半透明遮罩。
 */
class ToolbarDrawerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    interface Listener {
        fun onBack()
        fun onForward()
        fun onRefresh()
        fun onNewTab()
        fun onAddBookmark()
        fun onShowBookmarks()
        fun onDismiss()
    }

    /** 动画总开关 */
    var animationsEnabled: Boolean = true

    private var listener: Listener? = null
    private lateinit var scrim: View
    private lateinit var drawerContent: LinearLayout
    private var isOpen = false

    init {
        // 半透明遮罩（点击关闭）
        scrim = View(context).apply {
            setBackgroundColor(Color.parseColor("#80000000"))
            setOnClickListener { close() }
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        addView(scrim)

        // 抽屉内容
        drawerContent = (LayoutInflater.from(context)
            .inflate(R.layout.view_toolbar_drawer, this, false) as LinearLayout)
        addView(drawerContent)

        drawerContent.findViewById<View>(R.id.btnBack).setOnClickListener {
            listener?.onBack(); close()
        }
        drawerContent.findViewById<View>(R.id.btnForward).setOnClickListener {
            listener?.onForward(); close()
        }
        drawerContent.findViewById<View>(R.id.btnRefresh).setOnClickListener {
            listener?.onRefresh(); close()
        }
        drawerContent.findViewById<View>(R.id.btnNewTab).setOnClickListener {
            listener?.onNewTab(); close()
        }
        drawerContent.findViewById<View>(R.id.btnAddBookmark).setOnClickListener {
            listener?.onAddBookmark(); close()
        }
        drawerContent.findViewById<View>(R.id.btnBookmarks).setOnClickListener {
            listener?.onShowBookmarks(); close()
        }

        visibility = GONE
        // 初始位置在屏幕左侧外
        post {
            drawerContent.translationX = -drawerContent.width.toFloat()
        }
    }

    fun setListener(l: Listener?) {
        listener = l
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        // 抽屉占屏幕 2/3 宽
        val drawerWidth = ((right - left) * 2 / 3).coerceAtLeast(1)
        drawerContent.layout(0, 0, drawerWidth, bottom - top)
        scrim.layout(0, 0, right - left, bottom - top)
        if (!isOpen) {
            drawerContent.translationX = -drawerWidth.toFloat()
        }
    }

    fun open() {
        if (isOpen) return
        isOpen = true
        visibility = VISIBLE
        val targetX = 0f
        val startX = -drawerContent.width.toFloat().coerceAtMost(-1f)
        drawerContent.translationX = startX
        if (animationsEnabled) {
            scrim.alpha = 0f
            scrim.animate().alpha(1f).setDuration(250).start()
            drawerContent.animate()
                .translationX(targetX)
                .setDuration(250)
                .start()
        } else {
            scrim.alpha = 1f
            drawerContent.translationX = targetX
        }
    }

    fun close() {
        if (!isOpen) return
        isOpen = false
        val endX = -drawerContent.width.toFloat().coerceAtMost(-1f)
        if (animationsEnabled) {
            scrim.animate().alpha(0f).setDuration(250).start()
            drawerContent.animate()
                .translationX(endX)
                .setDuration(250)
                .withEndAction {
                    visibility = GONE
                    listener?.onDismiss()
                }
                .start()
        } else {
            drawerContent.translationX = endX
            visibility = GONE
            listener?.onDismiss()
        }
    }

    fun isOpen(): Boolean = isOpen
}
