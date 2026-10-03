package com.legnix.lxnav.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.legnix.lxnav.MainActivity
import com.legnix.lxnav.R

/**
 * 需求 9：启动页。
 *
 * 展示 LEGNIX 品牌 Logo，约 900ms 后自动跳转主页；点击任意处可提前跳过。
 */
class SplashActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var navigated = false

    private val goHome = Runnable { navigateToHome() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // 点击可跳过
        findViewById<android.view.View>(R.id.splashLogo).setOnClickListener { navigateToHome() }

        handler.postDelayed(goHome, 900L)
    }

    private fun navigateToHome() {
        if (navigated) return
        navigated = true
        handler.removeCallbacks(goHome)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(goHome)
    }
}