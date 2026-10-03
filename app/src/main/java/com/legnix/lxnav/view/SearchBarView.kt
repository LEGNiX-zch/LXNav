package com.legnix.lxnav.view

import android.content.Context
import android.graphics.Color
import android.text.InputType
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import com.legnix.lxnav.R

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
        setBackgroundResource(R.drawable.bg_capsule_glass)
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
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#80FFFFFF"))
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
            setColorFilter(Color.WHITE)
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
            // 搜索词 → 百度搜索
            "https://www.baidu.com/s?wd=" + java.net.URLEncoder.encode(input, "UTF-8")
        }
    }

    fun setText(text: String) {
        editText.setText(text)
    }
}
