package com.example.notiftiles

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import java.io.File
import java.io.FileOutputStream

/**
 * Pinned pic: a circle in the shortcuts row that shows the picture you chose when you press it.
 * The picture is copied (and shrunk) into the app's own folder, so it keeps working if the original is moved or deleted.
 */
object PinnedPic {
    private const val DIR = "pinned"
    private const val MAX_SIDE = 2048          // the big picture shown when pressed
    private const val THUMB_PX = 160           // the small icon in the circle
    private val thumbs = HashMap<String, Bitmap>()

    private fun dir(c: Context): File = File(c.filesDir, DIR).apply { mkdirs() }
    fun file(c: Context, id: String): File = File(dir(c), "$id.jpg")

    /** Copies the picture at [uri] into the app (shrunk to a sensible size). Returns its id, or null when it cannot be read. */
    fun save(c: Context, uri: Uri): String? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            c.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = c.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
            val id = "p" + System.currentTimeMillis()
            FileOutputStream(file(c, id)).use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            bmp.recycle()
            id
        } catch (e: Exception) {
            null
        }
    }

    /** The big picture, shrunk to fit [maxSide] pixels. */
    fun load(c: Context, id: String, maxSide: Int = MAX_SIDE): Bitmap? {
        val f = file(c, id)
        if (!f.exists()) return null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(f.path, bounds)
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
            BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample })
        } catch (e: Exception) { null }
    }

    /** The small round icon for the widget circle (cached, so the widget does not decode the picture on every redraw). */
    fun thumb(c: Context, id: String): Bitmap? {
        val f = file(c, id)
        val key = id + ":" + f.lastModified()
        synchronized(thumbs) { thumbs[key]?.let { return it } }
        val src = load(c, id, THUMB_PX * 2) ?: return null
        val side = minOf(src.width, src.height)
        val sq = Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
        val out = Bitmap.createBitmap(THUMB_PX, THUMB_PX, Bitmap.Config.ARGB_8888)
        val cv = Canvas(out)
        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        cv.drawCircle(THUMB_PX / 2f, THUMB_PX / 2f, THUMB_PX / 2f, p)
        p.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        cv.drawBitmap(sq, null, android.graphics.Rect(0, 0, THUMB_PX, THUMB_PX), p)
        if (sq !== src) src.recycle()
        synchronized(thumbs) {
            if (thumbs.size > 12) thumbs.clear()
            thumbs[key] = out
        }
        return out
    }

    fun delete(c: Context, id: String) {
        try { file(c, id).delete() } catch (e: Exception) { }
        synchronized(thumbs) { thumbs.keys.removeAll { it.startsWith("$id:") } }
    }

    /** Removes pictures no slot uses any more (called after a slot changes). */
    fun cleanUp(c: Context) {
        val used = HashSet<String>()
        for (i in 0 until Prefs.QUICK_SLOTS) Prefs.quick(c, i)?.let { if (it.type == "pic") used.add(it.target) }
        Prefs.pinPic(c)?.let { used.add(it) }   // the picture chosen for the Where to? pin
        dir(c).listFiles()?.forEach { f ->
            val id = f.name.removeSuffix(".jpg")
            if (id !in used) delete(c, id)
        }
    }
}

/** Shows a pinned picture full screen. Tap to close; pinch to zoom, drag to move when zoomed. */
class PinnedPicActivity : Activity() {
    private var scale = 1f
    private val m = Matrix()
    private lateinit var iv: ImageView

    @android.annotation.SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        val id = intent.getStringExtra("id") ?: ""
        val bmp = PinnedPic.load(this, id, 2048)
        val root = FrameLayout(this)
        root.setBackgroundColor(Color.argb(235, 0, 0, 0))
        if (bmp == null) {
            val tv = TextView(this)
            tv.text = "This picture is gone. Pick it again from Settings > Shortcuts row."
            tv.setTextColor(Color.WHITE)
            tv.gravity = Gravity.CENTER
            root.addView(tv, FrameLayout.LayoutParams(-1, -1))
            root.setOnClickListener { finish() }
            setContentView(root)
            return
        }
        iv = ImageView(this)
        iv.scaleType = ImageView.ScaleType.MATRIX
        iv.setImageBitmap(bmp)
        root.addView(iv, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
        iv.post { fit(bmp) }

        val det = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean {
                val f = (scale * d.scaleFactor).coerceIn(1f, 6f) / scale
                scale *= f
                m.postScale(f, f, d.focusX, d.focusY)
                iv.imageMatrix = m
                return true
            }
        })
        var lx = 0f; var ly = 0f; var moved = false
        iv.setOnTouchListener { _, ev ->
            det.onTouchEvent(ev)
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> { lx = ev.x; ly = ev.y; moved = false }
                MotionEvent.ACTION_MOVE -> if (!det.isInProgress && ev.pointerCount == 1) {
                    if (Math.abs(ev.x - lx) + Math.abs(ev.y - ly) > 12f) moved = true
                    if (scale > 1.01f) { m.postTranslate(ev.x - lx, ev.y - ly); iv.imageMatrix = m }
                    lx = ev.x; ly = ev.y
                } else { lx = ev.x; ly = ev.y; moved = true }
                MotionEvent.ACTION_UP -> if (!moved && scale <= 1.01f) finish()
            }
            true
        }
        // The picture opens with the panel's own open style (spring, slide, zoom...), unless animation is off
        if (Alive.style(this, "open") != "off") {
            iv.alpha = 0f; iv.scaleX = 0.88f; iv.scaleY = 0.88f
            iv.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(Alive.t(this, 300)).start()
        }
    }

    private fun fit(b: Bitmap) {
        val vw = iv.width.toFloat(); val vh = iv.height.toFloat()
        if (vw <= 0f || vh <= 0f) return
        val s = minOf(vw / b.width, vh / b.height)
        m.reset()
        m.postScale(s, s)
        m.postTranslate((vw - b.width * s) / 2f, (vh - b.height * s) / 2f)
        iv.imageMatrix = m
        scale = 1f
    }
}
