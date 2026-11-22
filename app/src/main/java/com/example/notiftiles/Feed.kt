package com.example.notiftiles

import android.app.Notification
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.service.notification.StatusBarNotification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Style(private val bgL: Long, private val fgL: Long, private val bgD: Long, private val fgD: Long) {
    fun bg(dark: Boolean): Int = (if (dark) bgD else bgL).toInt()
    fun fg(dark: Boolean): Int = (if (dark) fgD else fgL).toInt()
}

object Feed {
    private val DEFAULT = Style(0xFFEDEDF1, 0xFF1B1B1F, 0xFF2B2B31, 0xFFECECEF)
    private val WHATSAPP = Style(0xFFDCF8E6, 0xFF0B6B3A, 0xFF12372A, 0xFF7EE2A8)
    private val FACEBOOK = Style(0xFFDCE9FF, 0xFF1B4FA8, 0xFF172A4A, 0xFF9DC1FF)
    private val PHONE = Style(0xFFFFE0E0, 0xFFA32020, 0xFF4A1C1C, 0xFFFFA3A3)
    private val SMS = Style(0xFFD8F1F4, 0xFF0E6674, 0xFF123B42, 0xFF8FDDE8)
    private val GMAIL = Style(0xFFFFE4DE, 0xFFB3261E, 0xFF4A211C, 0xFFFFB4A8)
    private val OUTLOOK = Style(0xFFD9ECFB, 0xFF0F5C9E, 0xFF14304A, 0xFF8CC6F5)
    private val TEAMS = Style(0xFFE5E3FA, 0xFF4B3FA8, 0xFF2A274F, 0xFFBDB6FF)
    private val INSTAGRAM = Style(0xFFFBE0F0, 0xFFA31F6E, 0xFF4A1A38, 0xFFFFA6D6)
    private val SNAPCHAT = Style(0xFFFFF6C2, 0xFF7A6400, 0xFF4A4210, 0xFFFFE35C)

    val NOTES = Style(0xFFFFE8CC, 0xFF8A4B00, 0xFF4A3016, 0xFFFFC98A)

    fun style(pkg: String): Style = when {
        pkg.startsWith("com.whatsapp") -> WHATSAPP
        pkg.startsWith("com.facebook") -> FACEBOOK
        pkg.contains("dialer") || pkg.contains("incallui") || pkg.contains("telecom") ||
            pkg == "com.android.phone" || pkg == "com.android.contacts" -> PHONE
        pkg.contains("messaging") || pkg.contains("mms") -> SMS
        pkg == "com.google.android.gm" -> GMAIL
        pkg == "com.microsoft.office.outlook" -> OUTLOOK
        pkg.startsWith("com.microsoft.teams") -> TEAMS
        pkg == "com.instagram.android" -> INSTAGRAM
        pkg == "com.snapchat.android" -> SNAPCHAT
        else -> DEFAULT
    }

    fun isDark(c: Context): Boolean =
        (c.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    fun alpha(color: Int, a: Int): Int = (color and 0x00FFFFFF) or (a shl 24)

    fun title(n: StatusBarNotification): String =
        n.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()

    fun text(n: StatusBarNotification): String {
        val ex = n.notification.extras
        return (ex.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: ex.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
    }

    /**
     * Every message of the chat, oldest first, one line each like the app shows them:
     * "[10:54 PM] Name: text" (today), "[Yesterday, 10:54 PM] ..." or "[11/21, 10:54 PM] ..." (older); the style is
     * chosen in Look & colors. Falls back to inbox lines, then to the single text.
     */
    @Suppress("DEPRECATION")
    fun lines(c: Context, n: StatusBarNotification, unreadOnly: Boolean = false): List<String> {
        val ex = n.notification.extras
        val out = ArrayList<String>()
        try {
            val msgs = ArrayList<Notification.MessagingStyle.Message>()
            ex.getParcelableArray(Notification.EXTRA_HISTORIC_MESSAGES)?.let {
                msgs.addAll(Notification.MessagingStyle.Message.getMessagesFromBundleArray(it))
            }
            ex.getParcelableArray(Notification.EXTRA_MESSAGES)?.let {
                msgs.addAll(Notification.MessagingStyle.Message.getMessagesFromBundleArray(it))
            }
            val seen = HashSet<String>()
            val handledTs = if (unreadOnly) handledUntil(c, n) else 0L
            val anySender = msgs.any { it.senderPerson != null }
            for (m in msgs.sortedBy { it.timestamp }) {
                val t = m.text?.toString().orEmpty()
                if (t.isEmpty()) continue
                if (unreadOnly && (m.timestamp <= handledTs || isOwnMsg(n, m, anySender))) continue
                val who = m.senderPerson?.name?.toString().orEmpty()
                val st = stamp(c, m.timestamp)
                val line = (if (st.isNotEmpty()) "[$st] " else "") + (if (who.isNotEmpty()) "$who: " else "") + t
                if (seen.add(line)) out.add(line)
            }
        } catch (e: Exception) {
        }
        if (out.isNotEmpty()) return out
        val inbox = ex.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.map { it.toString() }?.filter { it.isNotEmpty() }.orEmpty()
        if (inbox.isNotEmpty()) return inbox
        val t = text(n)
        return if (t.isEmpty()) emptyList() else listOf(t)
    }

    /** The text in the brackets in front of a message, by the chosen style ("" = none). */
    private fun stamp(c: Context, ts: Long): String {
        val style = Prefs.stampStyle(c)
        if (style == 3) return ""
        val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ts))
        val full = SimpleDateFormat("M/d, h:mm a", Locale.getDefault()).format(Date(ts))
        if (style == 1) return full
        if (style == 2) return time
        if (android.text.format.DateUtils.isToday(ts)) return time
        if (android.text.format.DateUtils.isToday(ts + android.text.format.DateUtils.DAY_IN_MILLIS)) return "Yesterday, $time"
        return full
    }

    /**
     * The photo that came with the notification, if the app gives us one (a big picture, or the picture of a photo
     * message). Not the sender's avatar. Blocking: call it from a background thread. Null when there is none or when
     * the app does not let other apps read it.
     */
    @Suppress("DEPRECATION")
    fun picture(c: Context, n: StatusBarNotification, maxPx: Int): Bitmap? {
        val ex = n.notification.extras
        try {
            val raw = ex.get(Notification.EXTRA_PICTURE_ICON) ?: ex.get(Notification.EXTRA_PICTURE)
            if (raw is Bitmap) return if (looksLikeGlyph(raw)) null else raw
            if (raw is android.graphics.drawable.Icon) {
                val d = raw.loadDrawable(c)
                if (d != null) {
                    val w = d.intrinsicWidth
                    val h = d.intrinsicHeight
                    if (w > 0 && h > 0) {
                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        d.setBounds(0, 0, w, h)
                        d.draw(Canvas(bmp))
                        return if (looksLikeGlyph(bmp)) null else bmp
                    }
                }
            }
        } catch (e: Exception) {
        }
        try {
            val arr = ex.getParcelableArray(Notification.EXTRA_MESSAGES) ?: return null
            val msgs = Notification.MessagingStyle.Message.getMessagesFromBundleArray(arr)
            for (m in msgs.sortedByDescending { it.timestamp }) {
                val uri = m.dataUri ?: continue
                val mime = m.dataMimeType ?: continue
                if (!mime.startsWith("image/")) continue
                val b = decode(c, uri, maxPx)
                if (b != null) return b
            }
        } catch (e: Exception) {
        }
        return null
    }

    /** A photo or video that came with the message: [bmp] is the picture / first frame, [videoUri] plays the video. */
    class Media(val bmp: Bitmap?, val videoUri: Uri?, val isVideo: Boolean)

    /**
     * The newest photo or video of the chat notification, if the app lets us read it. Blocking: call from a
     * background thread. For a video with a readable file it gives its first frame and the file to play; when the app
     * only gives a still picture for a video, isVideo is true but videoUri is null.
     */
    @Suppress("DEPRECATION")
    fun media(c: Context, n: StatusBarNotification, maxPx: Int): Media? {
        try {
            val arr = n.notification.extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            if (arr != null) {
                val msgs = Notification.MessagingStyle.Message.getMessagesFromBundleArray(arr)
                val videoWord = Regex("🎥|video|فيديو", RegexOption.IGNORE_CASE)
                for (m in msgs.sortedByDescending { it.timestamp }) {
                    val uri = m.dataUri ?: continue
                    val mime = m.dataMimeType ?: continue
                    if (mime.startsWith("video/")) return Media(frame(c, uri, maxPx), uri, true)
                    if (mime.startsWith("image/")) {
                        val b = decode(c, uri, maxPx) ?: continue
                        return Media(b, null, videoWord.containsMatchIn(m.text?.toString().orEmpty()))
                    }
                }
            }
        } catch (e: Exception) {
        }
        val p = picture(c, n, maxPx) ?: return null
        return Media(p, null, false)
    }

    private fun frame(c: Context, uri: Uri, maxPx: Int): Bitmap? {
        val r = android.media.MediaMetadataRetriever()
        return try {
            r.setDataSource(c, uri)
            val f = r.getFrameAtTime(0, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            if (f != null && f.width > maxPx) Bitmap.createScaledBitmap(f, maxPx, f.height * maxPx / f.width, true) else f
        } catch (e: Exception) {
            null
        } finally {
            try {
                r.release()
            } catch (e: Exception) {
            }
        }
    }

    private fun decode(c: Context, uri: Uri, maxPx: Int): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options()
            bounds.inJustDecodeBounds = true
            c.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= maxPx && bounds.outHeight / (sample * 2) >= maxPx) sample *= 2
            val o = BitmapFactory.Options()
            o.inSampleSize = sample
            c.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Some apps (Snapchat) hand over a flat icon (a chat bubble) instead of the real photo. A real photo has many
     * colours and no see-through areas; an icon has few colours or lots of transparency.
     */
    private fun looksLikeGlyph(b: Bitmap): Boolean {
        if (b.width <= 0 || b.height <= 0) return true
        return try {
            val n = 24
            var clear = 0
            val colours = HashSet<Int>()
            for (iy in 0 until n) {
                for (ix in 0 until n) {
                    val px = b.getPixel(ix * (b.width - 1) / (n - 1), iy * (b.height - 1) / (n - 1))
                    if ((px ushr 24) < 40) clear++ else colours.add((px and 0xF0F0F0) or 0xFF000000.toInt())
                }
            }
            clear > n * n / 4 || colours.size <= 4
        } catch (e: Exception) {
            false
        }
    }

    private val PHOTO_WORDS = Regex(
        "(sent|shared)\\b.{0,14}\\b(photo|image|picture|video|snap|reel|story|gif|sticker)|^\\s*(📷|🖼|📹|🎥)|أرسل.*(صورة|فيديو)|صورة|فيديو",
        RegexOption.IGNORE_CASE
    )

    /** True when the newest message only says that a photo / video / snap was sent (the app gives no picture for it). */
    fun photoHint(c: Context, n: StatusBarNotification): Boolean {
        val last = lines(c, n).lastOrNull() ?: return false
        val body = last.replace(Regex("^\\[[^\\]]*\\]\\s*"), "")
        return PHOTO_WORDS.containsMatchIn(body)
    }

    // ---- What is new: decided by the time of the MESSAGES, never by when the app re-posted the notification ----
    // (Apps re-post a chat after every reply / read; that must not bring an answered message back.)

    @Suppress("DEPRECATION")
    private fun messageList(n: StatusBarNotification): List<Notification.MessagingStyle.Message> = try {
        val arr = n.notification.extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        if (arr == null) emptyList() else Notification.MessagingStyle.Message.getMessagesFromBundleArray(arr)
    } catch (e: Exception) {
        emptyList()
    }

    fun hasMessages(n: StatusBarNotification): Boolean = messageList(n).isNotEmpty()

    /** Time of the newest message; the post time when the app gives no message list (or no usable times). */
    fun newestTs(n: StatusBarNotification): Long {
        val t = messageList(n).maxOfOrNull { it.timestamp } ?: 0L
        return if (t > 0L) t else n.postTime
    }

    /** What to store in Prefs.markHandled when this chat is read: everything up to now, including its newest message. */
    fun handledStamp(n: StatusBarNotification): Long = maxOf(n.postTime, newestTs(n))

    /** Is [m] written by the phone's owner? (no sender = the owner, as in Android's own MessagingStyle) */
    @Suppress("DEPRECATION")
    private fun isOwnMsg(n: StatusBarNotification, m: Notification.MessagingStyle.Message, anySender: Boolean): Boolean {
        val p = m.senderPerson ?: return anySender
        val ex = n.notification.extras
        val name = p.name?.toString().orEmpty()
        val user = try {
            ex.get(Notification.EXTRA_MESSAGING_PERSON) as? android.app.Person
        } catch (e: Exception) {
            null
        }
        if (user != null) {
            val uk = user.key
            if (uk != null && uk == p.key) return true
            val un = user.name?.toString().orEmpty()
            if (un.isNotEmpty() && un == name) return true
        }
        val self = ex.getCharSequence(Notification.EXTRA_SELF_DISPLAY_NAME)?.toString().orEmpty()
        return name == "You" || name == "أنت" || (self.isNotEmpty() && name == self)
    }

    // ---- Done / answered: remembered per notification AND per conversation (an app may re-post under a new key) ----

    private fun convKey(n: StatusBarNotification): String? {
        val t = title(n)
        return if (t.isEmpty()) null else "conv:" + n.packageName + "|" + t
    }

    /** The time up to which this chat counts as done (read or answered). */
    fun handledUntil(c: Context, n: StatusBarNotification, map: Map<String, Long> = Prefs.handledMap(c)): Long {
        val a = map[n.key] ?: 0L
        val b = convKey(n)?.let { map[it] } ?: 0L
        return maxOf(a, b)
    }

    fun markDone(c: Context, n: StatusBarNotification, until: Long) {
        Prefs.markHandled(c, n.key, until)
        convKey(n)?.let { Prefs.markHandled(c, it, until) }
    }

    private class Sent(val pkg: String, val title: String, val text: String, val time: Long)

    private val sentList = java.util.concurrent.CopyOnWriteArrayList<Sent>()

    /** Remember a reply sent from the panel, so the app's re-post with that very text is never shown as new. */
    fun rememberSent(n: StatusBarNotification, text: String) {
        val now = System.currentTimeMillis()
        sentList.removeAll { now - it.time > 600000L }
        sentList.add(Sent(n.packageName, title(n), text.trim(), now))
    }

    private fun matchesSent(n: StatusBarNotification): Boolean {
        if (sentList.isEmpty()) return false
        val now = System.currentTimeMillis()
        sentList.removeAll { now - it.time > 600000L }
        val m = messageList(n).maxByOrNull { it.timestamp }
        val t = (m?.text?.toString() ?: text(n)).trim()
        if (t.isEmpty()) return false
        val ttl = title(n)
        return sentList.any {
            it.pkg == n.packageName && (it.title.isEmpty() || ttl.isEmpty() || it.title == ttl) &&
                (t == it.text || t.endsWith(": " + it.text))
        }
    }

    /** True when the newest message of the chat was written by the phone's owner (so there is nothing new to read). */
    fun ownLast(n: StatusBarNotification): Boolean {
        if (matchesSent(n)) return true
        val l = messageList(n)
        val last = l.maxByOrNull { it.timestamp } ?: return false
        return isOwnMsg(n, last, l.any { it.senderPerson != null })
    }

    /** All messages, one per line. */
    fun fullText(c: Context, n: StatusBarNotification): String =
        lines(c, n, true).ifEmpty { lines(c, n) }.joinToString("\n")

    /**
     * What this app gives us for a notification: the names and kinds of its fields and the type of each message's
     * attachment (never the texts). Shown by a long press on the photo box, to see whether any picture is offered.
     */
    @Suppress("DEPRECATION")
    fun debugDump(n: StatusBarNotification): String {
        val ex = n.notification.extras
        val sb = StringBuilder()
        sb.append(n.packageName).append('\n')
        for (k in ex.keySet().sorted()) {
            val v = try {
                ex.get(k)
            } catch (e: Exception) {
                null
            }
            val d = when (v) {
                null -> "null"
                is CharSequence -> "text(" + v.length + ")"
                is Bitmap -> "BITMAP " + v.width + "x" + v.height
                is android.graphics.drawable.Icon -> "ICON"
                is Array<*> -> "array(" + v.size + ")"
                else -> v.javaClass.simpleName
            }
            sb.append(k.removePrefix("android.")).append(": ").append(d).append('\n')
        }
        for (m in messageList(n)) {
            sb.append("message: mime=").append(m.dataMimeType).append(" uri=").append(if (m.dataUri == null) "none" else "yes").append('\n')
        }
        return sb.toString()
    }

    /** The latest [k] messages, one per line (newest at the bottom). */
    fun recentText(c: Context, n: StatusBarNotification, k: Int): String = lines(c, n, true).takeLast(k).joinToString("\n")

    fun label(c: Context, pkg: String): String = try {
        c.packageManager.getApplicationLabel(c.packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) {
        pkg
    }

    fun iconBitmap(c: Context, pkg: String, px: Int): Bitmap? = try {
        val d = c.packageManager.getApplicationIcon(pkg)
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        d.setBounds(0, 0, px, px)
        d.draw(Canvas(bmp))
        bmp
    } catch (e: Exception) {
        null
    }

    /** Tile colours of one app: the colour picked for it in Look & colors, else the built-in style. */
    fun style(c: Context, pkg: String): Style {
        val own = Prefs.appColor(c, pkg)
        if (own == 0) return style(pkg)
        val bg = own.toLong() and 0xFFFFFFFFL
        val fg = Look.contrast(own).toLong() and 0xFFFFFFFFL
        return Style(bg, fg, bg, fg)
    }

    /** [showAlways]: the panel passes true so it still lists messages hidden by snooze / quiet hours. */
    fun items(c: Context, showAlways: Boolean = false): List<StatusBarNotification> {
        val svc = NotifListener.instance ?: return emptyList()
        if (!showAlways && Extras.quietActive(c)) return emptyList()
        val enabled = Prefs.enabled(c)
        val muted = Prefs.muted(c)
        val pinned = Prefs.pinned(c)
        val handled = Prefs.handledMap(c)
        val all: List<StatusBarNotification> = try {
            svc.activeNotifications?.toList() ?: emptyList()
        } catch (e: Exception) {
            return emptyList()
        }
        val withKids = all
            .filter { (it.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0 }
            .map { it.packageName to it.notification.group }
            .toSet()
        return all
            .filter { it.packageName in enabled && it.packageName != c.packageName && it.packageName !in muted }
            .filter { (it.notification.flags and Notification.FLAG_ONGOING_EVENT) == 0 }
            .filter { newestTs(it) > handledUntil(c, it, handled) }
            .filter { !ownLast(it) }
            .filter { title(it).isNotEmpty() || text(it).isNotEmpty() }
            .filter {
                val summary = (it.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
                !summary || (it.packageName to it.notification.group) !in withKids
            }
            .sortedWith(order(c, pinned))
    }

    /** Pinned apps first, then the order chosen in Look & colors (newest first unless changed). */
    private fun order(c: Context, pinned: Set<String>): Comparator<StatusBarNotification> {
        val pinFirst = compareByDescending<StatusBarNotification> { it.packageName in pinned }
        return when (Prefs.sortMode(c)) {
            1 -> pinFirst.thenBy { it.postTime }
            2 -> {
                val mine = Prefs.appOrder(c)
                pinFirst
                    .thenBy { val i = mine.indexOf(it.packageName); if (i < 0) Int.MAX_VALUE else i }
                    .thenByDescending { it.postTime }
            }
            3 -> {
                val names = HashMap<String, String>()
                pinFirst
                    .thenBy { names.getOrPut(it.packageName) { label(c, it.packageName).lowercase() } }
                    .thenByDescending { it.postTime }
            }
            else -> pinFirst.thenByDescending { it.postTime }
        }
    }
}
