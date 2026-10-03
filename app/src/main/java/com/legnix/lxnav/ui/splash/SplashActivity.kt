package com.legnix.lxnav.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.legnix.lxnav.MainActivity
import com.legnix.lxnav.R
import com.legnix.lxnav.data.Prefs

/**
 * 需求 9：启动页。
 *
 * 展示 FLENX 品牌 Logo（双 X 造型不变），位于中下位置：
 *  - 淡入 + 呼吸缩放；
 *  - 约 1.4s 后平滑上移过渡到主页；
 *  - 点击任意处可提前跳过；
 *  - 当全局动画开关关闭时，直接硬切进入主页。
 */
class SplashActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var navigated = false

    private lateinit var logo: View

    private val goHome = Runnable { playExitAndNavigate() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        logo = findViewById(R.id.splashLogo)

        // 点击可跳过
        logo.setOnClickListener { playExitAndNavigate() }

        if (Prefs.animationsEnabled) {
            playEnterAnimation()
            handler.postDelayed(goHome, 1400L)
        } else {
            // 动画关闭：不播放任何动画，直接进入主页
            handler.postDelayed(goHome, 900L)
        }
    }

    /** 入场：淡入 + 呼吸缩放（轻微放大再回弹） */
    private fun playEnterAnimation() {
        logo.alpha = 0f
        logo.scaleX = 0.86f
        logo.scaleY = 0.86f
        logo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(600L)
            .setInterpolator(OvershootInterpolator(1.1f))
            .withEndAction {
                // 呼吸：轻微来回缩放
                if (navigated) return@withEndAction
                logo.animate()
                    .scaleX(1.04f).scaleY(1.04f)
                    .setDuration(450L)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction {
                        if (navigated) return@withEndAction
                        logo.animate()
                            .scaleX(1f).scaleY(1f)
                            .setDuration(450L)
                            .setInterpolator(AccelerateDecelerateInterpolator())
                            .start()
                    }
                    .start()
            }
            .start()
    }

    /** 出场：平滑上移 + 淡出，再进入主页 */
    private fun playExitAndNavigate() {
        if (navigated) return
        if (!Prefs.animationsEnabled) {
            navigateToHome()
            return
        }
        navigated = true
        handler.removeCallbacks(goHome)
        logo.animate()
            .translationY(-120f)
            .alpha(0f)
            .scaleX(0.96f)
            .scaleY(0.96f)
            .setDuration(360L)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction { finishNavigate() }
            .start()
    }

    private fun navigateToHome() {
        if (navigated) return
        navigated = true
        handler.removeCallbacks(goHome)
        finishNavigate()
    }

    private fun finishNavigate() {
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(0, 0)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(goHome)
    }
}