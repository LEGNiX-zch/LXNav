package com.legnix.lxnav.ui.history

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.legnix.lxnav.R
import com.legnix.lxnav.data.HistoryStore
import com.legnix.lxnav.data.model.HistoryEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 历史记录页。
 *
 * 交互：
 * - 点击某条历史 → 把 URL 通过 setResult 返回给 MainActivity 加载并关闭本页；
 * - 顶部返回按钮 → 直接关闭；
 * - 顶部"清空" → 弹确认框，确认后清空并刷新列表与空态。
 */
class HistoryActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var emptyView: TextView

    private val entries = mutableListOf<HistoryEntry>()
    private lateinit var adapter: ArrayAdapter<HistoryEntry>

    /** 时间显示：今天只显示时分，其他显示月-日 */
    private val timeFmt = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    private val sameDayFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        listView = findViewById(R.id.listHistory)
        emptyView = findViewById(R.id.tvHistoryEmpty)

        findViewById<ImageButton>(R.id.btnHistoryBack).setOnClickListener {
            finish()
        }
        findViewById<TextView>(R.id.btnHistoryClear).setOnClickListener {
            confirmClear()
        }

        adapter = object : ArrayAdapter<HistoryEntry>(this, 0, entries) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context)
                    .inflate(R.layout.item_history, parent, false)
                val item = entries[position]
                view.findViewById<TextView>(R.id.historyTitle).text =
                    item.title.ifBlank { item.url }
                view.findViewById<TextView>(R.id.historyUrl).text = item.url
                view.findViewById<TextView>(R.id.historyTime).text = formatTime(item.visitedAt)
                return view
            }
        }
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val entry = entries.getOrNull(position) ?: return@setOnItemClickListener
            val data = Intent().putExtra("url", entry.url)
            setResult(Activity.RESULT_OK, data)
            finish()
        }

        reload()
    }

    private fun reload() {
        entries.clear()
        entries.addAll(HistoryStore.getAll(this))
        adapter.notifyDataSetChanged()

        val isEmpty = entries.isEmpty()
        emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        listView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun confirmClear() {
        if (entries.isEmpty()) {
            Toast.makeText(this, R.string.history_empty, Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setMessage(R.string.history_clear_confirm)
            .setNegativeButton(R.string.common_cancel, null)
            .setPositiveButton(R.string.history_clear) { _, _ ->
                HistoryStore.clear(this)
                reload()
                Toast.makeText(this, R.string.history_cleared, Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun formatTime(millis: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - millis
        return when {
            diff in 0..60_000L -> "刚刚"
            diff in 0..3_600_000L -> "${diff / 60_000}分钟前"
            isSameDay(millis, now) -> sameDayFmt.format(Date(millis))
            else -> timeFmt.format(Date(millis))
        }
    }

    private fun isSameDay(a: Long, b: Long): Boolean {
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return fmt.format(Date(a)) == fmt.format(Date(b))
    }
}
