package com.legnix.lxnav.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.widget.ImageView
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * 图片工具（本轮功能迭代新增）。
 *
 * - 需求 2：把用户选中的图片裁剪为圆形并缓存到应用私有目录，供抽屉头像使用。
 * - 需求 11：把用户选中的背景图复制到应用私有目录，供主页背景使用。
 */
object ImageUtils {

    private const val AVATAR_SIZE = 256   // 头像输出尺寸（px）
    private const val CACHE_DIR = "img_cache"

    /** 缓存的圆形头像文件名 */
    private const val AVATAR_FILE = "avatar_round.png"

    /** 缓存的背景图文件名 */
    private const val BG_FILE = "custom_bg"

    private fun cacheDir(context: Context): File {
        val dir = File(context.filesDir, CACHE_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * 需求 2：读取 [uri] 指向的图片，居中方形裁剪后裁成圆形 PNG，
     * 写入应用私有目录并返回其绝对路径。失败返回 null。
     */
    fun saveRoundAvatar(context: Context, uri: Uri): String? {
        return try {
            val src = decodeSampled(context, uri, AVATAR_SIZE) ?: return null
            val square = centerSquare(src)
            val output = Bitmap.createBitmap(AVATAR_SIZE, AVATAR_SIZE, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            // 圆形裁剪：先用 SRC 画圆，再用 SRC_IN 贴图
            canvas.drawCircle(AVATAR_SIZE / 2f, AVATAR_SIZE / 2f, AVATAR_SIZE / 2f, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            val dst = Rect(0, 0, AVATAR_SIZE, AVATAR_SIZE)
            canvas.drawBitmap(square, null, dst, paint)
            paint.xfermode = null

            val file = File(cacheDir(context), AVATAR_FILE)
            FileOutputStream(file).use { out ->
                output.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            if (square !== src) square.recycle()
            src.recycle()
            output.recycle()
            file.absolutePath
        } catch (t: Throwable) {
            null
        }
    }

    /**
     * 需求 11：把 [uri] 指向的图片复制到应用私有目录，返回其绝对路径。
     * 返回的路径可用于设置 View 背景，不受来源 URI 权限生命周期影响。
     */
    fun saveCustomBackground(context: Context, uri: Uri): String? {
        return try {
            val dir = cacheDir(context)
            // 使用固定文件名 + 时间戳后缀，避免 Glide/ImageView 缓存旧图
            val file = File(dir, "$BG_FILE-${System.currentTimeMillis()}")
            // 清理旧背景文件
            dir.listFiles { f -> f.name.startsWith(BG_FILE) }?.forEach { old ->
                if (old.absolutePath != file.absolutePath) old.delete()
            }
            context.contentResolver.openInputStream(uri)?.use { input: InputStream ->
                FileOutputStream(file).use { out -> input.copyTo(out) }
            } ?: return null
            file.absolutePath
        } catch (t: Throwable) {
            null
        }
    }

    /** 采样解码，避免 OOM（长边不超过 [targetSize] 的 2 倍）。 */
    private fun decodeSampled(context: Context, uri: Uri, targetSize: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
        if (opts.outWidth <= 0 || opts.outHeight <= 0) return null

        var sample = 1
        val minSide = minOf(opts.outWidth, opts.outHeight)
        while (minSide / sample > targetSize * 2) sample *= 2

        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        }
    }

    /** 居中裁剪为正方形。 */
    private fun centerSquare(src: Bitmap): Bitmap {
        val side = minOf(src.width, src.height)
        val x = (src.width - side) / 2
        val y = (src.height - side) / 2
        if (side == src.width && side == src.height) return src
        return Bitmap.createBitmap(src, x, y, side, side)
    }

    /**
     * 便捷方法：若 [path] 存在则显示图片，否则恢复 [fallbackRes]（默认占位图）。
     */
    fun applyAvatar(imageView: ImageView, path: String, fallbackRes: Int) {
        if (path.isNotBlank() && File(path).exists()) {
            try {
                val bmp = BitmapFactory.decodeFile(path)
                if (bmp != null) {
                    imageView.setImageBitmap(bmp)
                    return
                }
            } catch (_: Throwable) {
            }
        }
        imageView.setImageResource(fallbackRes)
    }
}
