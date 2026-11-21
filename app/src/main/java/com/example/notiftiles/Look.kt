package com.example.notiftiles

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.media.ExifInterface
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.RemoteViews
import android.widget.TextView
import java.io.File

// Fonts (family, weight, italic, size) and background images, shared by the widget and the screens.
object Look {

    val FONTS = listOf(
        "sans-serif" to "Default",
        "sans-serif-medium" to "Medium",
        "sans-serif-light" to "Light",
        "sans-serif-condensed" to "Condensed",
        "sans-serif-black" to "Black",
        "serif" to "Serif",
        "monospace" to "Mono",
        "casual" to "Casual",
        "cursive" to "Cursive"
    )

    fun scale(ctx: Context): Float = Prefs.fontScale(ctx) / 100f

    private fun boldFor(ctx: Context, designed: Boolean): Boolean = when (Prefs.boldMode(ctx)) {
        1 -> true
        2 -> false
        else -> designed
    }

    private fun styleFor(ctx: Context, designedBold: Boolean): Int =
        (if (boldFor(ctx, designedBold)) Typeface.BOLD else 0) or
            (if (Prefs.italic(ctx)) Typeface.ITALIC else 0)

    /** Text with the chosen font family, weight and italic baked in (for the home-screen widget). */
    fun span(ctx: Context, text: String, designedBold: Boolean): CharSequence {
        val s = SpannableString(text)
        val len = s.length
        if (len == 0) return s
        val family = Prefs.fontFamily(ctx)
        if (family != "sans-serif") s.setSpan(TypefaceSpan(family), 0, len, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        val style = styleFor(ctx, designedBold)
        if (style != 0) s.setSpan(StyleSpan(style), 0, len, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        return s
    }

    /** Widget text with font + size. */
    fun setText(rv: RemoteViews, ctx: Context, id: Int, text: String, baseSp: Float, designedBold: Boolean) {
        rv.setTextViewText(id, span(ctx, text, designedBold))
        rv.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, baseSp * scale(ctx))
    }

    /** Font + size for every text in a normal view tree (panel, search box). Safe to call repeatedly. */
    fun applyTree(v: View, ctx: Context) {
        if (v is TextView) {
            var o = v.getTag(R.id.nt_orig) as? FloatArray
            if (o == null) {
                o = floatArrayOf(v.textSize, if (v.typeface?.isBold == true) 1f else 0f)
                v.setTag(R.id.nt_orig, o)
            }
            v.setTextSize(TypedValue.COMPLEX_UNIT_PX, o[0] * scale(ctx))
            v.typeface = Typeface.create(Prefs.fontFamily(ctx), styleFor(ctx, o[1] == 1f))
        }
        if (v is ViewGroup) {
            for (i in 0 until v.childCount) applyTree(v.getChildAt(i), ctx)
        }
    }

    // ---- Background images (kind: widget, dial, panel) ----

    private val cache = HashMap<String, Pair<Long, Bitmap>>()

    private fun file(ctx: Context, name: String) = File(ctx.filesDir, "img_$name.jpg")

    fun hasImage(ctx: Context, name: String): Boolean = file(ctx, name).exists()

    fun clearImage(ctx: Context, name: String) {
        file(ctx, name).delete()
        cache.remove(name)
    }

    fun clearAllImages(ctx: Context) {
        for (n in listOf("widget", "dial", "panel")) clearImage(ctx, n)
    }

    fun bitmap(ctx: Context, name: String): Bitmap? {
        val f = file(ctx, name)
        if (!f.exists()) return null
        val m = f.lastModified()
        cache[name]?.let { if (it.first == m) return it.second }
        val b = try {
            BitmapFactory.decodeFile(f.path)
        } catch (e: Exception) {
            null
        } ?: return null
        cache[name] = m to b
        return b
    }

    /** Copies a picked picture into the app, shrunk so the widget can carry it. Blocking. */
    fun importImage(ctx: Context, uri: Uri, name: String): Boolean {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            val big = maxOf(bounds.outWidth, bounds.outHeight)
            while (big / sample > 2000) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            var bmp = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return false
            val orient = try {
                ctx.contentResolver.openInputStream(uri)?.use {
                    ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)
                } ?: 1
            } catch (e: Exception) {
                1
            }
            val deg = when (orient) {
                6 -> 90f
                3 -> 180f
                8 -> 270f
                else -> 0f
            }
            if (deg != 0f) {
                val m = Matrix()
                m.postRotate(deg)
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            }
            val longest = maxOf(bmp.width, bmp.height)
            if (longest > 900) {
                val f = 900f / longest
                bmp = Bitmap.createScaledBitmap(
                    bmp,
                    (bmp.width * f).toInt().coerceAtLeast(1),
                    (bmp.height * f).toInt().coerceAtLeast(1),
                    true
                )
            }
            file(ctx, name).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            cache.remove(name)
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Rounded panel colour, with the chosen picture on top when there is one. */
    fun panelBackground(ctx: Context, color: Int, radiusPx: Int): Drawable {
        val base = GradientDrawable().apply {
            setColor(color)
            cornerRadius = radiusPx.toFloat()
        }
        val bmp = bitmap(ctx, "panel") ?: return base
        val img = CropDrawable(bmp, radiusPx.toFloat())
        img.setAlpha(Prefs.imgAlpha(ctx, "panel") * 255 / 100)
        return LayerDrawable(arrayOf(base, img))
    }

    fun contrast(color: Int): Int =
        if (Color.luminance(color) < 0.5f) 0xFFF2F2F5.toInt() else 0xFF1B1B1F.toInt()
}

// A picture centre-cropped to fill its bounds, with rounded corners.
class CropDrawable(private val bmp: Bitmap, private val radius: Float) : Drawable() {
    private val bmpShader = BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val rect = RectF()

    init {
        paint.shader = bmpShader
    }

    override fun onBoundsChange(b: Rect) {
        rect.set(b)
        if (b.width() <= 0 || b.height() <= 0) return
        val s = maxOf(b.width().toFloat() / bmp.width, b.height().toFloat() / bmp.height)
        val m = Matrix()
        m.setScale(s, s)
        m.postTranslate(
            b.left + (b.width() - bmp.width * s) / 2f,
            b.top + (b.height() - bmp.height * s) / 2f
        )
        bmpShader.setLocalMatrix(m)
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRoundRect(rect, radius, radius, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
