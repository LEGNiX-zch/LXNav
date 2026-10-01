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
import com.legnix.lxnav.util.SearchEngine

/**
 * 搜索栏：输入网址或搜索词，右侧搜索图标。
 *
 * - 点击搜索框或键盘回车 → 触发搜索
 * - URL 直接加载，关键词走 Prefs.engine 选定的搜索引擎
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
        val padding = (12 * resources.displayMetrics.density).toInt()

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
            setTextColor(Color.parseColor("#FF1A1A1A"))
            setHintTextColor(Color.parseColor("#8A8A8A8A"))
            textSize = 12f
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_GO or EditorInfo.IME_ACTION_SEARCH
            inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or InputType.TYPE_TEXT_VARIATION_URI
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEARCH) {
                    triggerSearch()
                    true
                } else false
            }
        }

        val searchIcon = ImageView(context).apply {
            setImageResource(R.drawable.ic_search)
            layoutParams = LinearLayout.LayoutParams(
                (22 * resources.displayMetrics.density).toInt(),
                (22 * resources.displayMetrics.density).toInt()
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
        val url = SearchEngine.toUrl(input)
        onSearch?.invoke(url)
        editText.text.clear()
    }

    fun setText(text: String) {
        editText.setText(text)
    }
}
