package com.example.notiftiles

import android.app.Notification
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.service.notification.StatusBarNotification

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

    fun items(c: Context): List<StatusBarNotification> {
        val svc = NotifListener.instance ?: return emptyList()
        val enabled = Prefs.enabled(c)
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
            .filter { it.packageName in enabled && it.packageName != c.packageName }
            .filter { (it.notification.flags and Notification.FLAG_ONGOING_EVENT) == 0 }
            .filter {
                val summary = (it.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
                !summary || (it.packageName to it.notification.group) !in withKids
            }
            .sortedByDescending { it.postTime }
    }
}
