package com.legnix.lxnav.ui.settings

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.legnix.lxnav.R
import com.legnix.lxnav.data.Prefs
import com.legnix.lxnav.web.BrowserTab

/**
 * 设置页。
 *
 * 设置项：
 * - 最大标签数量（1/2/3/4）
 * - 主题切换（液态玻璃 / 深色 / 浅色）
 * - 默认网页缩放
 * - UA 切换（手机 / 桌面）
 * - 动画总开关
 * - 屏幕形状覆盖（自动 / 强制方形 / 强制圆形）
 * - 清除缓存、Cookie
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var tvZoom: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        tvZoom = findViewById(R.id.tvZoomValue)

        setupMaxTabs()
        setupTheme()
        setupZoom()
        setupUa()
        setupAnimations()
        setupScreenOverride()
        setupClearButtons()
    }

    private fun setupMaxTabs() {
        val current = Prefs.maxTabs
        val buttons = listOf(
            findViewById<Button>(R.id.btnTabs1) to 1,
            findViewById<Button>(R.id.btnTabs2) to 2,
            findViewById<Button>(R.id.btnTabs3) to 3,
            findViewById<Button>(R.id.btnTabs4) to 4
        )
        updateSelectedButtons(buttons, current.toString()) { (btn, value) ->
            Prefs.setMaxTabs(this, value)
            updateSelectedButtons(buttons, value.toString()) {}
        }
    }

    private fun setupTheme() {
        val current = Prefs.theme
        val buttons = listOf(
            findViewById<Button>(R.id.btnThemeGlass) to Prefs.Theme.LIQUID_GLASS,
            findViewById<Button>(R.id.btnThemeDark) to Prefs.Theme.DARK,
            findViewById<Button>(R.id.btnThemeLight) to Prefs.Theme.LIGHT
        )
        updateSelectedButtons(buttons, current.key) { (btn, value) ->
            Prefs.setTheme(this, value)
            updateSelectedButtons(buttons, value.key) {}
        }
    }

    private fun setupZoom() {
        updateZoomDisplay()
        findViewById<Button>(R.id.btnZoomMinus).setOnClickListener {
            val v = (Prefs.defaultZoom - 10).coerceIn(50, 200)
            Prefs.setDefaultZoom(this, v)
            updateZoomDisplay()
        }
        findViewById<Button>(R.id.btnZoomPlus).setOnClickListener {
            val v = (Prefs.defaultZoom + 10).coerceIn(50, 200)
            Prefs.setDefaultZoom(this, v)
            updateZoomDisplay()
        }
    }

    private fun updateZoomDisplay() {
        tvZoom.text = "${Prefs.defaultZoom}%"
    }

    private fun setupUa() {
        val current = Prefs.uaMode
        val buttons = listOf(
            findViewById<Button>(R.id.btnUaMobile) to Prefs.UaMode.MOBILE,
            findViewById<Button>(R.id.btnUaDesktop) to Prefs.UaMode.DESKTOP
        )
        updateSelectedButtons(buttons, current.key) { (btn, value) ->
            Prefs.setUaMode(this, value)
            updateSelectedButtons(buttons, value.key) {}
        }
    }

    @Suppress("DEPRECATION")
    private fun setupAnimations() {
        val sw = findViewById<android.widget.Switch>(R.id.switchAnimations)
        sw.isChecked = Prefs.animationsEnabled
        sw.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setAnimationsEnabled(this, isChecked)
        }
    }

    private fun setupScreenOverride() {
        val current = Prefs.screenOverride
        val buttons = listOf(
            findViewById<Button>(R.id.btnScreenAuto) to Prefs.ScreenOverride.AUTO,
            findViewById<Button>(R.id.btnScreenSquare) to Prefs.ScreenOverride.SQUARE,
            findViewById<Button>(R.id.btnScreenRound) to Prefs.ScreenOverride.ROUND
        )
        updateSelectedButtons(buttons, current.key) { (btn, value) ->
            Prefs.setScreenOverride(this, value)
            updateSelectedButtons(buttons, value.key) {}
        }
    }

    private fun setupClearButtons() {
        findViewById<Button>(R.id.btnClearCache).setOnClickListener {
            BrowserTab.clearCache(this)
            Toast.makeText(this, "缓存已清除", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnClearCookies).setOnClickListener {
            BrowserTab.clearCookies()
            Toast.makeText(this, "Cookie已清除", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 通用：更新按钮组选中状态，选中项高亮，未选中项半透明。
     * T 为按钮关联的值类型，用 toString() 与 selectedKey 比较。
     */
    private fun <T> updateSelectedButtons(
        buttons: List<Pair<Button, T>>,
        selectedKey: String,
        onClick: (Pair<Button, T>) -> Unit
    ) {
        buttons.forEach { (btn, value) ->
            val valueKey = when (value) {
                is Prefs.Theme -> value.key
                is Prefs.UaMode -> value.key
                is Prefs.ScreenOverride -> value.key
                else -> value.toString()
            }
            val isSelected = valueKey == selectedKey
            btn.alpha = if (isSelected) 1f else 0.4f
            btn.setOnClickListener {
                onClick(btn to value)
            }
        }
    }
}
