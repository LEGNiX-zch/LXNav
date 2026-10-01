package com.legnix.lxnav.ui.bookmarks

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.legnix.lxnav.R
import com.legnix.lxnav.data.BookmarkStore
import com.legnix.lxnav.data.model.Bookmark

/**
 * 收藏列表页。
 * 点击收藏项 → 返回 URL 给 MainActivity 加载。
 */
class BookmarksActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookmarks)

        val list = findViewById<ListView>(R.id.bookmarksList)
        val emptyHint = findViewById<TextView>(R.id.emptyHint)

        val bookmarks = BookmarkStore.getAll(this)
        if (bookmarks.isEmpty()) {
            emptyHint.visibility = View.VISIBLE
            return
        }

        val adapter = object : ArrayAdapter<Bookmark>(this, 0, bookmarks) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context)
                    .inflate(R.layout.item_bookmark, parent, false)
                val item = bookmarks[position]
                view.findViewById<TextView>(R.id.bookmarkTitle).text = item.title
                view.findViewById<TextView>(R.id.bookmarkUrl).text = item.url
                return view
            }
        }
        list.adapter = adapter

        list.setOnItemClickListener { _, _, position, _ ->
            val bookmark = bookmarks[position]
            val data = Intent().putExtra("url", bookmark.url)
            setResult(Activity.RESULT_OK, data)
            finish()
        }
    }
}
