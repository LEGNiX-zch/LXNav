package com.legnix.lxnav.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.legnix.lxnav.R
import com.legnix.lxnav.data.Prefs

/**
 * 左侧抽屉式工具栏（第二阶段重构版）。
 *
 * - 从屏幕左侧滑入，只弹出半宽面板。
 * - 顶部：圆形头像 + 昵称（点击昵称 → 弹出编辑）。
 * - 中部八项纵向纯文字菜单：
 *     历史记录 / 下载内容 / 收藏夹 / 添加收藏 /
 *     无痕模式 / 夜间模式 / 翻译页面 / 离线缓存
 * - 底部三键：后退 / 前进 / 刷新。
 * - 点击空白遮罩 / 返回键 → 抽屉收回隐藏。
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
        fun onHistory()
        fun onDownloads()
        fun onShowBookmarks()
        fun onAddBookmark()
        fun onToggleIncognito(currentlyOn: Boolean)
        fun onToggleNightMode(currentlyOn: Boolean)
        fun onTranslate()
        fun onOffline()
        /** 昵称被编辑提交 */
        fun onNicknameChanged(newNickname: String)
        fun onDismiss()
    }

    /** 动画总开关 */
    var animationsEnabled: Boolean = true

    private var listener: Listener? = null
    private lateinit var scrim: View
    private lateinit var drawerContent: LinearLayout
    private var isOpen = false

    private lateinit var tvNickname: TextView
    private lateinit var menuNightMode: TextView
    private lateinit var menuIncognito: TextView

    /** 各开关的本地状态，仅在界面内维护视觉，真正的持久化交给外部 listener */
    private var incognitoOn = false
    private var nightModeOn = false

    init {
        // 半透明遮罩（点击关闭）
        scrim = View(context).apply {
            setBackgroundColor(Color.parseColor("#80000000"))
            setOnClickListener { close() }
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        addView(scrim)

        // 抽屉内容
        drawerContent = LayoutInflater.from(context)
            .inflate(R.layout.view_toolbar_drawer, this, false) as LinearLayout
        addView(drawerContent)

        bindViews()
        bindActions()

        visibility = GONE
        // 初始位置在屏幕左侧外
        post { drawerContent.translationX = -drawerContent.width.toFloat() }
    }

    private fun bindViews() {
        tvNickname = drawerContent.findViewById(R.id.tvNickname)
        menuNightMode = drawerContent.findViewById(R.id.menuNightMode)
        menuIncognito = drawerContent.findViewById(R.id.menuIncognito)

        // 头像占位（第二阶段用字母占位图，用户后续可替换为真实头像）
        drawerContent.findViewById<ImageView>(R.id.imgAvatar).setImageResource(R.drawable.ic_avatar_placeholder)
    }

    private fun bindActions() {
        // 底部导航三键
        drawerContent.findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            listener?.onBack(); close()
        }
        drawerContent.findViewById<ImageButton>(R.id.btnForward).setOnClickListener {
            listener?.onForward(); close()
        }
        drawerContent.findViewById<ImageButton>(R.id.btnRefresh).setOnClickListener {
            listener?.onRefresh(); close()
        }

        // 八项文字菜单
        drawerContent.findViewById<TextView>(R.id.menuHistory).setOnClickListener {
            listener?.onHistory(); close()
        }
        drawerContent.findViewById<TextView>(R.id.menuDownloads).setOnClickListener {
            listener?.onDownloads(); close()
        }
        drawerContent.findViewById<TextView>(R.id.menuBookmarks).setOnClickListener {
            listener?.onShowBookmarks(); close()
        }
        drawerContent.findViewById<TextView>(R.id.menuAddBookmark).setOnClickListener {
            listener?.onAddBookmark(); close()
        }
        drawerContent.findViewById<TextView>(R.id.menuIncognito).setOnClickListener {
            incognitoOn = !incognitoOn
            applyToggleVisual(menuIncognito, incognitoOn)
            listener?.onToggleIncognito(incognitoOn)
        }
        drawerContent.findViewById<TextView>(R.id.menuNightMode).setOnClickListener {
            nightModeOn = !nightModeOn
            applyToggleVisual(menuNightMode, nightModeOn)
            listener?.onToggleNightMode(nightModeOn)
        }
        drawerContent.findViewById<TextView>(R.id.menuTranslate).setOnClickListener {
            listener?.onTranslate(); close()
        }
        drawerContent.findViewById<TextView>(R.id.menuOffline).setOnClickListener {
            listener?.onOffline(); close()
        }

        // 昵称点击 → 编辑
        tvNickname.setOnClickListener { showNicknameEditor() }
    }

    /** 开关项：开启时文字用强调色并追加" (已开启)" */
    private fun applyToggleVisual(tv: TextView, on: Boolean) {
        val base = if (tv === menuNightMode) context.getString(R.string.menu_night_mode)
        else context.getString(R.string.menu_incognito)
        tv.text = if (on) "$base ${context.getString(R.string.menu_state_on)}" else base
        tv.setTextColor(
            if (on) context.getColor(R.color.accent) else context.getColor(R.color.glass_text)
        )
    }

    /** 简洁的昵称编辑弹窗（用 AlertDialog + EditText，避免额外布局文件） */
    private fun showNicknameEditor() {
        val edit = android.widget.EditText(context).apply {
            setText(Prefs.nickname)
            setSelection(text.length)
            hint = context.getString(R.string.drawer_default_nickname)
            setPadding(48, 36, 48, 36)
        }
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle(R.string.drawer_edit_nickname)
            .setView(edit)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = edit.text.toString().trim().ifBlank { Prefs.DEFAULT_NICKNAME }
                tvNickname.text = name
                listener?.onNicknameChanged(name)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /** 外部刷新昵称显示（例如设置页改动后） */
    fun setNickname(name: String) {
        tvNickname.text = name
    }

    /** 外部同步开关状态（例如夜间模式从设置页变更后） */
    fun syncToggles(incognito: Boolean, night: Boolean) {
        incognitoOn = incognito
        nightModeOn = night
        applyToggleVisual(menuIncognito, incognitoOn)
        applyToggleVisual(menuNightMode, nightModeOn)
    }

    fun setListener(l: Listener?) {
        listener = l
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        // 抽屉只占左半宽（内容自身宽度 280dp 已限制上限）
        val halfWidth = (right - left) / 2
        val contentWidth = drawerContent.measuredWidth.coerceAtMost(halfWidth)
        drawerContent.layout(0, 0, contentWidth, bottom - top)
        scrim.layout(0, 0, right - left, bottom - top)
        if (!isOpen) {
            drawerContent.translationX = -contentWidth.toFloat()
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