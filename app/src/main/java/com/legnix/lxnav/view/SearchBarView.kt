package com.legnix.lxnav.view

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.text.InputType
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import com.legnix.lxnav.R
import com.legnix.lxnav.data.Prefs

/**
 * 搜索栏：输入网址或搜索词，右侧搜索图标。
 *
 * - 输入 URL（含 .） → 直接加载
 * - 输入搜索词 → 走搜索引擎（默认百度）
 * - 搜索图标点击 / 键盘确认 → 触发加载
 */
class SearchBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var onSearch: ((url: String) -> Unit)? = null

    private val editText: EditText

    init {
        // 需求 5：搜索框换用自适应亮/暗的液态玻璃胶囊背景
        setBackgroundResource(R.drawable.bg_search_capsule_glass)
        val padding = (10 * resources.displayMetrics.density).toInt()

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            setPadding(padding, 0, padding, 0)
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        editText = EditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            background = null
            hint = "搜索或输入网址"
            // 需求 5：亮色主题下文字为黑，暗色主题下保持白
            applyTextColor(this)
            textSize = 12f
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_GO
            inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or InputType.TYPE_TEXT_VARIATION_URI
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    triggerSearch()
                    true
                } else false
            }
        }

        val searchIcon = ImageView(context).apply {
            setImageResource(R.drawable.ic_search)
            // 需求 5：图标颜色与文字保持一致（亮色黑 / 暗色白）
            setColorFilter(if (isNightMode()) Color.WHITE else Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(
                (24 * resources.displayMetrics.density).toInt(),
                (24 * resources.displayMetrics.density).toInt()
            )
            setOnClickListener { triggerSearch() }
        }

        row.addView(editText)
        row.addView(searchIcon)
        addView(row)
    }

    private fun triggerSearch() {
        val input = editText.text.toString().trim()
        if (input.isEmpty()) return
        val url = toUrl(input)
        onSearch?.invoke(url)
        editText.text.clear()
    }

    /** 将用户输入转换为 URL */
    private fun toUrl(input: String): String {
        // 包含 :// 或以 . 开头(含域名特征) → 当 URL
        return if (input.contains("://") || (input.contains(".") && !input.contains(" "))) {
            if (input.startsWith("http")) input else "https://$input"
        } else {
            // 需求 10：搜索词 → 使用设置页选中的搜索引擎
            Prefs.searchEngine.queryUrl + java.net.URLEncoder.encode(input, "UTF-8")
        }
    }

    /** 需求 5：亮色黑字 / 暗色白字 */
    private fun applyTextColor(et: EditText) {
        if (isNightMode()) {
            et.setTextColor(Color.WHITE)
            et.setHintTextColor(Color.parseColor("#80FFFFFF"))
        } else {
            et.setTextColor(Color.BLACK)
            et.setHintTextColor(Color.parseColor("#80000000"))
        }
    }

    private fun isNightMode(): Boolean {
        val mask = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return mask == Configuration.UI_MODE_NIGHT_YES
    }

    /** 主题切换后刷新文字/图标颜色（宿主 onResume 调用） */
    fun refreshTextColor() {
        applyTextColor(editText)
    }

    fun setText(text: String) {
        editText.setText(text)
    }
}
