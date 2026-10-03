package com.legnix.lxnav.ui.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.legnix.lxnav.BuildConfig
import com.legnix.lxnav.R

/**
 * 关于页。
 *
 * 展示 FLENX 简介、版本号、GitHub / QQ / 微信联系方式，以及持续迭代说明。
 */
class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        // 版本号
        val tvVersion = findViewById<TextView>(R.id.tvVersion)
        tvVersion.text = "版本 ${BuildConfig.VERSION_NAME}"

        // GitHub 链接（用外部浏览器打开）
        findViewById<Button>(R.id.btnGithub).setOnClickListener {
            openUrl("https://github.com/LEGNiX-zch/LXNav")
        }

        // QQ：无通用跳转协议，复制号码并提示
        findViewById<Button>(R.id.btnQq).setOnClickListener {
            copyToClipboard("1479350744", "QQ 号已复制")
        }

        // 微信：同上
        findViewById<Button>(R.id.btnWechat).setOnClickListener {
            copyToClipboard("zch20111015", "微信号已复制")
        }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(this, url, Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyToClipboard(text: String, tip: String) {
        val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText("lxnav", text))
        Toast.makeText(this, tip, Toast.LENGTH_SHORT).show()
    }
}
