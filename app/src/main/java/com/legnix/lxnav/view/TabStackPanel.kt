package com.legnix.lxnav.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import com.legnix.lxnav.R
import com.legnix.lxnav.data.model.Tab
import kotlin.math.abs

/**
 * 堆叠卡片式多任务后台面板。
 *
 * 交互：
 * - 上下滑动（fling）：切换浏览不同卡片
 * - 左滑 / 右滑当前卡片：关闭对应标签
 * - 点击当前卡片：切换到该网页
 * - 点击遮罩区域：关闭面板
 *
 * 视觉：卡片扑克牌式堆叠，当前卡片在中间，其余在上下层叠（缩小+半透明）。
 */
class TabStackPanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    interface Listener {
        /** 用户点击卡片，切换到对应网页 */
        fun onTabSelected(tab: Tab)
        /** 用户滑走卡片，关闭对应标签 */
        fun onTabClosed(tab: Tab)
        /** 面板被关闭（遮罩点击 / 返回键） */
        fun onDismiss()
    }

    /** 动画总开关 */
    var animationsEnabled: Boolean = true

    private var listener: Listener? = null
    private var tabs: List<Tab> = emptyList()
    private var currentIndex = 0
    private val cardViews = mutableListOf<View>()

    private val cardWidth: Int
        get() = (width * 0.82f).toInt()

    private val cardHeight: Int
        get() = (height * 0.32f).toInt()

    private val cardOffsetY: Float
        get() = cardHeight * 0.45f

    private val touchSlop: Float
        get() = ViewConfiguration.get(context).scaledTouchSlop.toFloat()

    private val swipeThreshold: Float
        get() = width * 0.35f

    private val gestureDetector: GestureDetector

    /** 拖拽起点 X，用于松手时判断关闭 */
    private var dragStarted = false

    init {
        // 半透明遮罩
        setBackgroundColor(Color.parseColor("#CC000000"))

        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                val card = cardViews.getOrNull(currentIndex)
                if (card != null && isPointInCard(e, card)) {
                    // 点击当前卡片 → 切换网页
                    tabs.getOrNull(currentIndex)?.let { listener?.onTabSelected(it) }
                } else {
                    // 点击遮罩 → 关闭面板
                    listener?.onDismiss()
                }
                return true
            }

            override fun onScroll(
                e1: MotionEvent,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                val dx = e2.x - e1.x
                // 横向移动为主时，拖拽当前卡片
                if (abs(dx) > abs(distanceY) && abs(dx) > touchSlop) {
                    val card = cardViews.getOrNull(currentIndex)
                    card?.translationX = dx
                    dragStarted = true
                    return true
                }
                return false
            }

            override fun onFling(
                e1: MotionEvent,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                val dx = e2.x - e1.x
                val dy = e2.y - e1.y
                // 垂直方向 fling → 切换卡片（仅在未横向拖拽时）
                if (!dragStarted && abs(dy) > abs(dx) && abs(dy) > touchSlop * 3) {
                    if (dy < 0) showNext() else showPrev()
                    return true
                }
                return false
            }
        })
        setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                if (dragStarted) {
                    handleDragRelease()
                    dragStarted = false
                }
            }
            true
        }
    }

    private fun isPointInCard(e: MotionEvent, card: View): Boolean {
        val left = card.left + card.translationX
        val top = card.top + card.translationY
        val right = left + card.width
        val bottom = top + card.height
        return e.x in left..right && e.y in top..bottom
    }

    private fun handleDragRelease() {
        val card = cardViews.getOrNull(currentIndex) ?: return
        val tx = card.translationX
        if (abs(tx) > swipeThreshold) {
            // 超过阈值 → 关闭标签（卡片飞出）
            val dir = if (tx > 0) 1f else -1f
            if (animationsEnabled) {
                card.animate()
                    .translationX(dir * width * 1.5f)
                    .alpha(0f)
                    .setDuration(220)
                    .withEndAction {
                        tabs.getOrNull(currentIndex)?.let { listener?.onTabClosed(it) }
                    }
                    .start()
            } else {
                tabs.getOrNull(currentIndex)?.let { listener?.onTabClosed(it) }
            }
        } else {
            // 未超过阈值 → 回弹
            if (animationsEnabled) {
                card.animate()
                    .translationX(0f)
                    .setDuration(200)
                    .setInterpolator(OvershootInterpolator(0.5f))
                    .start()
            } else {
                card.translationX = 0f
            }
        }
    }

    fun setListener(l: Listener?) {
        listener = l
    }

    /** 设置标签数据并刷新卡片 */
    fun setTabs(newTabs: List<Tab>, activeIndex: Int = 0) {
        tabs = newTabs
        currentIndex = activeIndex.coerceIn(0, newTabs.lastIndex.coerceAtLeast(0))
        rebuildCards()
        updateStack(animate = false)
    }

    private fun rebuildCards() {
        cardViews.forEach { removeView(it) }
        cardViews.clear()
        val inflater = LayoutInflater.from(context)
        tabs.forEachIndexed { i, tab ->
            val card = inflater.inflate(R.layout.item_tab_card, this, false)
            card.findViewById<TextView>(R.id.tabTitle).text = tab.title.ifEmpty { tab.url }
            card.findViewById<TextView>(R.id.tabUrl).text = tab.url
            val lp = card.layoutParams as LayoutParams
            lp.width = cardWidth.coerceAtLeast(1)
            lp.height = cardHeight.coerceAtLeast(1)
            lp.gravity = android.view.Gravity.CENTER
            card.layoutParams = lp
            addView(card)
            cardViews.add(card)
        }
        visibility = if (tabs.isEmpty()) GONE else VISIBLE
    }

    private fun showNext() {
        if (currentIndex < tabs.lastIndex) {
            currentIndex++
            updateStack(animate = true)
        }
    }

    private fun showPrev() {
        if (currentIndex > 0) {
            currentIndex--
            updateStack(animate = true)
        }
    }

    /** 更新所有卡片的堆叠位置 / 缩放 / 透明度 */
    private fun updateStack(animate: Boolean) {
        cardViews.forEachIndexed { i, card ->
            val offset = i - currentIndex
            val targetScale = if (offset == 0) 1f else maxOf(0.8f, 0.92f - abs(offset) * 0.05f)
            val targetY = offset * cardOffsetY
            val targetAlpha = if (offset == 0) 1f else maxOf(0f, 1f - abs(offset) * 0.35f)
            card.z = (cardViews.size - abs(offset)).toFloat()
            if (animate && animationsEnabled) {
                card.animate()
                    .scaleX(targetScale)
                    .scaleY(targetScale)
                    .translationY(targetY)
                    .alpha(targetAlpha)
                    .setDuration(250)
                    .setInterpolator(OvershootInterpolator(0.3f))
                    .start()
            } else {
                card.scaleX = targetScale
                card.scaleY = targetScale
                card.translationY = targetY
                card.alpha = targetAlpha
            }
        }
    }

    /** 显示面板（带入场动画） */
    fun show() {
        visibility = VISIBLE
        if (animationsEnabled) {
            alpha = 0f
            animate().alpha(1f).setDuration(200).start()
            cardViews.forEach { it.alpha = 0f }
            post { updateStack(animate = true) }
        } else {
            alpha = 1f
            updateStack(animate = false)
        }
    }

    /** 隐藏面板（带退场动画） */
    fun hide() {
        if (animationsEnabled) {
            animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction { visibility = GONE }
                .start()
        } else {
            visibility = GONE
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        // 布局变化后重新设置卡片尺寸并刷新堆叠
        if (cardViews.isNotEmpty()) {
            cardViews.forEach { card ->
                val lp = card.layoutParams as LayoutParams
                lp.width = cardWidth.coerceAtLeast(1)
                lp.height = cardHeight.coerceAtLeast(1)
                card.layoutParams = lp
            }
            updateStack(animate = false)
        }
    }
}
