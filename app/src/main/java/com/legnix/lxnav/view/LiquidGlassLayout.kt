package com.legnix.lxnav.view

import android.content.Context
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.RequiresApi

/**
 * 液态玻璃容器。
 *
 * - API 26-30：依靠半透明渐变 + 描边 drawable 模拟玻璃质感（无模糊，省性能）。
 * - API 31+ ：自动叠加 RenderEffect 硬件背景模糊，增强真实液态玻璃效果。
 *
 * 用法：把本 View 作为背景层放在内容下方，设置 background 为 bg_liquid_glass，
 * 再调用 enableBlur() 即可在 31+ 开启模糊。
 */
class LiquidGlassLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    /** 模糊半径（px），仅 API 31+ 生效 */
    var blurRadius: Float = 25f
        set(value) {
            field = value
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                applyBlur()
            }
        }

    /** 是否启用硬件模糊（仅 API 31+ 有意义） */
    var blurEnabled: Boolean = true
        set(value) {
            field = value
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                applyBlur()
            }
        }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            applyBlur()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun applyBlur() {
        setRenderEffect(
            if (blurEnabled) {
                RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP)
            } else {
                null
            }
        )
    }
}
