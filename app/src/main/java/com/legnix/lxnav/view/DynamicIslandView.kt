package com.legnix.lxnav.view

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import com.legnix.lxnav.R
import com.legnix.lxnav.util.MemInfo
import java.util.Calendar

/**
 * 灵动岛胶囊 View。
 *
 * - 单击：循环切换 时间 → 剩余内存 → 网页标题/URL
 * - 长按：播放弹性拉长 duang 动画，回调 onLongPress（由宿主弹出多任务面板）
 * - 宿主关闭多任务面板时调用 shrink()，胶囊弹性收缩回原始尺寸
 */
class DynamicIslandView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    enum class InfoMode { TIME, MEMORY, PAGE }

    interface Listener {
        fun onLongPress()
    }

    /** 动画总开关（关闭时用瞬时切换，提升老手表性能） */
    var animationsEnabled: Boolean = true

    private val textView: TextView
    private val gestureDetector: GestureDetector
    private val handler = Handler(Looper.getMainLooper())

    private var infoMode = InfoMode.TIME
    private var pageTitle = ""
    private var pageUrl = ""
    private var listener: Listener? = null

    private val tickRunnable = object : Runnable {
        override fun run() {
            // 时间/内存模式下每秒刷新
            if (infoMode == InfoMode.TIME || infoMode == InfoMode.MEMORY) {
                updateDisplay()
            }
            handler.postDelayed(this, 1000)
        }
    }

    init {
        // 液态玻璃胶囊背景
        setBackgroundResource(R.drawable.bg_capsule_glass)

        textView = TextView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
            setTextColor(Color.WHITE)
            textSize = resources.getDimension(R.dimen.capsule_text_size) / resources.displayMetrics.scaledDensity
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setPadding(
                dp(14), 0, dp(14), 0
            )
            gravity = Gravity.CENTER
            includeFontPadding = false
        }
        addView(textView)

        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                cycleInfo()
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                performLongPress()
            }
        })
        setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }

        updateDisplay()
        handler.post(tickRunnable)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun cycleInfo() {
        infoMode = when (infoMode) {
            InfoMode.TIME -> InfoMode.MEMORY
            InfoMode.MEMORY -> InfoMode.PAGE
            InfoMode.PAGE -> InfoMode.TIME
        }
        updateDisplay()
    }

    private fun updateDisplay() {
        textView.text = when (infoMode) {
            InfoMode.TIME -> {
                val cal = Calendar.getInstance()
                String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
            }
            InfoMode.MEMORY -> {
                "${MemInfo.getAvailableMb(context)}MB"
            }
            InfoMode.PAGE -> {
                pageTitle.ifEmpty { pageUrl }.ifEmpty { context.getString(R.string.legnix) }
            }
        }
    }

    /** 更新当前网页标题/URL（PAGE 模式下展示） */
    fun setPageInfo(title: String, url: String) {
        pageTitle = title
        pageUrl = url
        if (infoMode == InfoMode.PAGE) updateDisplay()
    }

    fun setListener(l: Listener?) {
        listener = l
    }

    private fun performLongPress() {
        if (animationsEnabled) {
            animate()
                .scaleX(1.45f)
                .scaleY(1.3f)
                .setDuration(180)
                .setInterpolator(OvershootInterpolator(0.9f))
                .withEndAction { listener?.onLongPress() }
                .start()
        } else {
            scaleX = 1.45f
            scaleY = 1.3f
            listener?.onLongPress()
        }
    }

    /** 胶囊弹性拉长（供宿主在打开面板时调用，与长按效果一致） */
    fun stretch() {
        if (animationsEnabled) {
            animate()
                .scaleX(1.45f)
                .scaleY(1.3f)
                .setDuration(180)
                .setInterpolator(OvershootInterpolator(0.9f))
                .start()
        } else {
            scaleX = 1.45f
            scaleY = 1.3f
        }
    }

    /** 胶囊弹性收缩回原始尺寸（关闭多任务面板时调用） */
    fun shrink() {
        if (animationsEnabled) {
            animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(260)
                .setInterpolator(OvershootInterpolator(0.6f))
                .start()
        } else {
            scaleX = 1f
            scaleY = 1f
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(tickRunnable)
    }
}
