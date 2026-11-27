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
        "sans-serif-thin" to "Thin",
        "sans-serif-condensed" to "Condensed",
        "sans-serif-condensed-light" to "Condensed light",
        "sans-serif-smallcaps" to "Small caps",
        "serif-monospace" to "Typewriter",
        "sans-serif-black" to "Black",
        "serif" to "Serif",
        "monospace" to "Mono",
        "casual" to "Casual",
        "cursive" to "Cursive"
    )

    // Shapes for the clock / weather / date and fast-dial shapes
    val SHAPES = listOf(
        "0" to "Round",
        "1" to "Rounded square",
        "2" to "Square",
        "3" to "Teardrop",
        "4" to "Leaf",
        "5" to "Diamond",
        "6" to "Hexagon",
        "7" to "Octagon"
    )

    fun shapeRes(ctx: Context): Int = when (Prefs.shape(ctx)) {
        1 -> R.drawable.nt_squircle_bg
        2 -> R.drawable.nt_shape_square
        3 -> R.drawable.nt_shape_teardrop
        4 -> R.drawable.nt_shape_leaf
        5 -> R.drawable.nt_shape_diamond
        6 -> R.drawable.nt_shape_hexagon
        7 -> R.drawable.nt_shape_octagon
        else -> R.drawable.nt_circle_bg
    }

    fun scale(ctx: Context): Float = Prefs.fontScale(ctx) / 100f

    // Emoji that can replace the built-in icons / words (see EmojiPicker)
    val EMOJI_NOTE = listOf(
        "📝", "🗒️", "📒", "📓", "📔", "📋",
        "📌", "📍", "✍️", "🖊️", "💡", "⭐",
        "❤️", "🔖", "➕", "🧾", "📜", "📃",
        "📄", "🗓️", "🏷️", "🌟", "💬", "📎",
        "🎯", "🔔", "🧠", "🖋️", "📑", "🗃️",
        "📅", "📆", "📁", "📂", "🗄️", "📰",
        "🗞️", "📚", "📖", "📕", "📗", "📘",
        "📙", "✒️", "🖍️", "🖌️", "🎨", "🔍",
        "🔎", "✅", "☑️", "✔️", "📊", "📈",
        "🏆", "🥇", "🎓", "💼", "🔐", "🔑",
        "🧩", "🚀", "🌈", "🌸", "🌺", "🌻",
        "🍀", "🦋", "🐝", "🐱", "🐼", "🦊",
        "🦄", "🔥", "⚡", "💎", "👑", "🎁",
        "🎈", "🎉", "✨", "💫", "😊", "😎",
        "🤩", "🥰", "🧡", "💛", "💚", "💙",
        "💜", "🖤", "🤍", "🆕", "📣", "🔆",
        "🕯️", "🪶", "📮", "🧷", "📐"
    )
    val EMOJI_MIC = listOf(
        "🎙️", "🎤", "🎧", "🔴", "⏺️", "📢",
        "🔊", "🗣️", "🎵", "📻", "🎚️", "📣",
        "🎶", "🎼", "🎬", "🎹", "🥁", "🎸",
        "📼", "🔉", "🔈", "📡", "🎞️", "👂",
        "🛑", "▶️", "💭", "🫧", "🪩", "🔔",
        "🔇", "📺", "📹", "🎥", "📽️",
        "🪕", "🎷", "🎺", "🎻", "🥳", "🎉",
        "🎊", "🎈", "🎭", "🌈", "⚡", "🔥",
        "✨", "💫", "🌟", "⭐", "💎", "🚀",
        "🛸", "👽", "🤖", "🦜", "🐦", "🕊️",
        "🐬", "🦄", "🐱", "🐼", "🐶", "🦋",
        "🌸", "🌺", "🍀", "❤️", "💜", "💙",
        "💚", "💛", "🧡", "🖤", "🤍", "😎",
        "🤩", "😍", "🥰", "😮", "🗨️", "💬",
        "🗯️", "📞", "☎️", "📲", "🎯", "🏆"
    )
    val EMOJI_RESET = listOf(
        "🔄", "🔁", "♻️", "🧹", "✨", "🗑️",
        "💫", "⚡", "🌀", "🔃", "🧽", "🪄",
        "🌈", "🔥", "💥", "🔂", "⏳", "🕐",
        "🌟", "💎", "🍀", "🌊", "🎈", "🎉",
        "🧿", "⬆️", "🔝", "🆕", "🚀", "🔰",
        "♾️", "🔙", "🔚", "🔛", "🔜", "↩️",
        "↪️", "⤴️", "⤵️", "🔀", "⏪", "⏩",
        "🔆", "🔅", "💡", "🧲", "🪐", "🌍",
        "🌎", "☀️", "🌙", "⭐", "☄️", "🛸",
        "🧬", "🔮", "🎲", "🎰", "🎡", "🎢",
        "🎠", "🎯", "🏁", "🪁", "🪂", "🦋",
        "🐝", "🌸", "🌺", "🌻", "🍃", "🌿",
        "☘️", "🌱", "🍂", "🍁", "🌪️", "❄️",
        "💧", "🫧", "⭕", "🔵", "🟢", "🟣",
        "💖", "💗", "😊", "😎", "🤩"
    )
    val EMOJI_CLEAR = listOf(
        "🧹", "🗑️", "✅", "✔️", "🧼", "🪄",
        "✨", "❌", "🧽", "💨", "🚮", "📭",
        "🫧", "🗂️", "📥", "🔥", "💣", "🌪️",
        "🧻", "🚿", "🛁", "🪣", "🌬️", "🙈",
        "🚫", "⛔", "☑️", "🏁", "🎯", "🆑",
        "🧴", "🪥", "🚰", "💦", "💧", "🌧️",
        "☔", "🌈", "🌟", "🪐", "☄️", "💫",
        "🎆", "🎇", "🧨", "🪦", "⚰️", "📦",
        "📤", "🗃️", "🗄️", "🚀", "🛸", "🪁",
        "🍃", "🌿", "🍂", "🍁", "🪶", "🕊️",
        "🦋", "🐝", "🐱", "🐼", "🦄", "🌸",
        "🌺", "🌻", "🍀", "😎", "🤩", "😌",
        "🥳", "🙌", "👏", "👌", "✌️", "🤞",
        "💪", "🏆", "🥇", "🎉", "🎊", "🎈",
        "💯", "🆗", "⭕", "🟢"
    )
    val EMOJI_EDIT = listOf(
        "✏️", "🖍️", "🖊️", "🖌️", "🎨", "🛠️",
        "⚙️", "🔧", "✨", "🪄", "📐", "🧩",
        "🎛️", "🔮", "🧪", "🪛", "🔨", "🧰",
        "🪚", "📏", "🖋️", "🧵", "🎭", "🎪",
        "💎", "🌸", "🦄", "🐱", "🐼", "🍭",
        "✂️", "📎", "🖇️", "📌", "📍", "🔗",
        "🔖", "🏷️", "🧷", "🧶", "🪡", "👑",
        "💄", "💅", "🪞", "🛋️", "🪑", "🏗️",
        "🧱", "⚒️", "🗜️", "⛏️", "🔦", "🕯️",
        "💡", "🎬", "🎞️", "📸", "🌟", "⭐",
        "⚡", "🔥", "🚀", "🌈", "🌺", "🌻",
        "🍀", "🦋", "🐝", "🦊", "🐶", "🦁",
        "🐯", "🐸", "🐵", "🍓", "🍒", "🍉",
        "🍩", "🍪", "🧁", "🍿", "🎁", "🎀",
        "🎊", "🎉", "🎈", "🥳", "😎", "🤩",
        "❤️", "💜", "💙", "💚"
    )

    val EMOJI_GEAR = listOf(
        "⚙️", "🔧", "🛠️", "🎛️", "🎚️", "🧰",
        "🔩", "⛭", "🪛", "🧭", "🔑", "🗝️",
        "📐", "🧪", "🔬", "💡", "🧩", "☰",
        "⋮", "☸️", "🛡️", "🎡", "🌐", "🏠",
        "👤", "🪜", "🧱", "⛓️", "🔗",
        "🧷", "🪝", "🔭", "📡", "🛰️", "🚀",
        "🛸", "🔋", "🔌", "💻", "🖥️", "⌨️",
        "🖱️", "💾", "📟", "📲", "☎️", "🕹️",
        "🎮", "🧬", "⚗️", "🧫", "🔎", "🔍",
        "🗺️", "⏱️", "⏲️", "⌚", "🔰", "♻️",
        "🌟", "⭐", "✨", "⚡", "🔥", "🌈",
        "🍀", "💎", "🦄", "🐱", "🐼", "🦊",
        "😎", "🤖", "👾", "🧠", "❤️", "💜",
        "💙", "💚"
    )
    val EMOJI_INBOX = listOf(
        "📥", "📬", "📨", "📩", "✉️", "💌",
        "📧", "📮", "🔔", "💬", "🗨️", "📱",
        "🗂️", "📫", "🕊️", "🦜", "📡", "🧲",
        "🪄", "⭐", "❤️", "🌈", "🌟", "🍀",
        "🔥", "📤", "📦", "📪", "📭", "🗳️",
        "🗞️", "📰", "📞", "☎️", "📲", "💻",
        "📟", "🦅", "🦉", "🐦", "🦢", "🐧",
        "🐬", "🦋", "🐝", "🐱", "🐼", "🦊",
        "🦄", "🌸", "🌺", "🌻", "🍃", "☘️",
        "✨", "💫", "⚡", "💎", "👑", "🎁",
        "🎀", "🎈", "🎉", "🥳", "😊", "😎",
        "🤩", "🥰", "💖", "💗", "💙", "💚",
        "💛", "🧡", "💜", "🖤", "🤍", "🚀",
        "🛸", "🌍"
    )

    fun withEmoji(emoji: String, text: String): String = if (emoji.isEmpty()) text else "$emoji $text"

    /** "Inbox", or "<emoji> Inbox" when an emoji is chosen. */
    fun inboxLabel(ctx: Context, empty: Boolean = false): String = withEmoji(Prefs.emojiFor(ctx, "inbox", empty), "Inbox")

    /** "+ Note" with the built-in icon, or just the emoji (no word) when an emoji is chosen. */
    fun noteLabel(ctx: Context, empty: Boolean = false): String = Prefs.emojiFor(ctx, "note", empty).let { if (it.isEmpty()) "+ Note" else it }

    /** An emoji drawn into a square picture, for the widget's image buttons (mic, refresh). */
    fun emojiBitmap(ctx: Context, emoji: String, dpSize: Float): Bitmap? {
        if (emoji.isEmpty()) return null
        return try {
            val px = (dpSize * ctx.resources.displayMetrics.density).toInt().coerceIn(32, 192)
            val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            p.textSize = px * 0.78f
            p.textAlign = Paint.Align.CENTER
            val fm = p.fontMetrics
            Canvas(bmp).drawText(emoji, px / 2f, px / 2f - (fm.ascent + fm.descent) / 2f, p)
            bmp
        } catch (e: Exception) {
            null
        }
    }

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

    /** Letter case chosen in Look & colors (widget text only; never used for clock / date patterns). */
    fun caseOf(ctx: Context, text: String): String = when (Prefs.textCase(ctx)) {
        1 -> text.uppercase()
        2 -> text.lowercase()
        3 -> text.split(" ").joinToString(" ") { w -> w.replaceFirstChar { it.uppercaseChar() } }
        else -> text
    }

    /** Widget text with font + size. */
    fun setText(rv: RemoteViews, ctx: Context, id: Int, text: String, baseSp: Float, designedBold: Boolean) {
        rv.setTextViewText(id, span(ctx, caseOf(ctx, text), designedBold))
        rv.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, baseSp * scale(ctx))
    }

    /** Font + size for every text in a normal view tree (panel, search box). Safe to call repeatedly. */
    fun applyTree(v: View, ctx: Context) {
        if (v is TextView) {
            var o = v.getTag(R.id.nt_orig) as? FloatArray
            if (o == null) {
                val oc = v.currentTextColor
                o = floatArrayOf(v.textSize, if (v.typeface?.isBold == true) 1f else 0f, (oc ushr 16).toFloat(), (oc and 0xFFFF).toFloat(), 0f)
                v.setTag(R.id.nt_orig, o)
            }
            val g = v.getTag(R.id.nt_grp) as? String
            val gs = if (g == null) 1f else Prefs.grpSize(ctx, g) / 100f
            v.setTextSize(TypedValue.COMPLEX_UNIT_PX, o[0] * scale(ctx) * gs)
            v.typeface = Typeface.create(Prefs.fontFamily(ctx), styleFor(ctx, o[1] == 1f))
            // Separate colour per part of the panel (restored to the original when set back to automatic)
            val gc = if (g == null) 0 else Prefs.grpColor(ctx, g)
            if (gc != 0) {
                v.setTextColor(gc)
                o[4] = 1f
            } else if (o[4] == 1f) {
                v.setTextColor((o[2].toInt() shl 16) or o[3].toInt())
                o[4] = 0f
            }
        }
        if (v is ViewGroup) {
            for (i in 0 until v.childCount) applyTree(v.getChildAt(i), ctx)
        }
    }

    /**
     * A "3D" version of one of the built-in icons: the icon in [color] with a light top, a darker bottom and a
     * soft shadow underneath. Used for the widget's mic, settings and refresh icons when 3D icons are on.
     */
    fun icon3d(ctx: Context, resId: Int, color: Int, dpSize: Float): Bitmap? {
        return try {
            val d = ctx.getDrawable(resId)?.mutate() ?: return null
            val px = (dpSize * ctx.resources.displayMetrics.density).toInt().coerceIn(32, 192)
            val pad = (px * 0.08f).toInt()
            val glyph = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
            d.setBounds(pad, pad, px - pad, px - pad)
            d.setTint(color or 0xFF000000.toInt())
            d.draw(Canvas(glyph))

            val out = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
            val oc = Canvas(out)

            // soft drop shadow
            val blur = Paint(Paint.ANTI_ALIAS_FLAG)
            blur.maskFilter = android.graphics.BlurMaskFilter(px * 0.04f, android.graphics.BlurMaskFilter.Blur.NORMAL)
            val off = IntArray(2)
            val shadow = glyph.extractAlpha(blur, off)
            val sp = Paint(Paint.ANTI_ALIAS_FLAG)
            sp.color = 0xAA000000.toInt()
            oc.drawBitmap(shadow, off[0] + px * 0.03f, off[1] + px * 0.05f, sp)

            // light on top, dark at the bottom, only where the icon is
            val lit = glyph.copy(Bitmap.Config.ARGB_8888, true)
            val gp = Paint(Paint.ANTI_ALIAS_FLAG)
            gp.shader = android.graphics.LinearGradient(
                0f, pad.toFloat(), 0f, (px - pad).toFloat(),
                intArrayOf(0xB3FFFFFF.toInt(), 0x00FFFFFF, 0x66000000),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            gp.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_ATOP)
            Canvas(lit).drawRect(0f, 0f, px.toFloat(), px.toFloat(), gp)
            oc.drawBitmap(lit, 0f, 0f, null)
            out
        } catch (e: Exception) {
            null
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
        val layers = ArrayList<Drawable>()
        layers.add(base)
        val bmp = bitmap(ctx, "panel")
        if (bmp != null) {
            val img = CropDrawable(bmp, radiusPx.toFloat())
            img.setAlpha(Prefs.imgAlpha(ctx, "panel") * 255 / 100)
            layers.add(img)
        }
        if (Prefs.borderPanel(ctx) && Prefs.borderWidthOf(ctx, "panel") > 0) {
            layers.add(GradientDrawable().apply {
                setColor(0)
                cornerRadius = radiusPx.toFloat()
                setStroke(
                    (Prefs.borderWidthOf(ctx, "panel") * ctx.resources.displayMetrics.density).toInt().coerceAtLeast(1),
                    borderColorOf(ctx, "panel", contrast(color) and 0xFFFFFF or 0x99000000.toInt())
                )
            })
        }
        return if (layers.size == 1) base else LayerDrawable(layers.toTypedArray())
    }

    /** Border colour: the chosen one, or [auto] when set to automatic. */
    fun borderColor(ctx: Context, auto: Int): Int = Prefs.borderColor(ctx).let { if (it != 0) it else auto }

    /** Border colour of one border ("panel", "tiles", "widget"): its own pick, or [auto] when automatic. */
    fun borderColorOf(ctx: Context, kind: String, auto: Int): Int =
        Prefs.borderColorOf(ctx, kind).let { if (it != 0) it else auto }

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
