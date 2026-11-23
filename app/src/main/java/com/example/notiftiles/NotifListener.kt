package com.example.notiftiles

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotifListener : NotificationListenerService() {

    companion object {
        @Volatile var instance: NotifListener? = null
        @Volatile var onChange: (() -> Unit)? = null
    }

    override fun onListenerConnected() {
        instance = this
        try {
            activeNotifications?.forEach { Prefs.addSeen(this, it.packageName) }
        } catch (e: Exception) {
        }
        if (Prefs.locOwned(this)) watchLocation()
        Extras.tick(this)
        changed()
    }

    override fun onListenerDisconnected() {
        instance = null
        try {
            if (Access.enabled(this)) NotificationListenerService.requestRebind(ComponentName(this, NotifListener::class.java))
        } catch (e: Exception) {
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        Prefs.addSeen(this, sbn.packageName)
        if (sbn.packageName == LocationAuto.MAPS && (sbn.notification.flags and Notification.FLAG_ONGOING_EVENT) != 0 && Prefs.locOwned(this)) {
            Prefs.setLocSeen(this, true)
        }
        changed()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Google Maps navigation ended: switch Location off straight away instead of waiting for the next check.
        if (sbn.packageName == LocationAuto.MAPS && Prefs.locOwned(this) && Prefs.locSeen(this)) {
            handler.postDelayed({
                if (LocationAuto.navNotification() == null) LocationAuto.release(this)
            }, 1000)
        }
        changed()
    }

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    // Switches Location off again once the Google Maps navigation notification is gone (only when we switched it on).
    private var gone = 0
    private val locCheck = object : Runnable {
        override fun run() {
            val c = this@NotifListener
            if (!Prefs.locOwned(c)) return
            if (!LocationAuto.isOn(c) || !Prefs.autoLocationOff(c)) {
                Prefs.setLocOwned(c, false)
                return
            }
            var nav = true
            try {
                val list = c.activeNotifications
                if (list != null) {
                    nav = list.any {
                        it.packageName == LocationAuto.MAPS && (it.notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
                    }
                }
            } catch (e: Exception) {
            }
            if (nav) {
                Prefs.setLocSeen(c, true)
                gone = 0
            } else {
                gone++
                val seen = Prefs.locSeen(c)
                val age = System.currentTimeMillis() - Prefs.locSince(c)
                if ((seen && gone >= 2) || (!seen && age > 10 * 60 * 1000L)) {
                    LocationAuto.release(c)
                    return
                }
            }
            handler.postDelayed(this, 20000)
        }
    }

    fun watchLocation() {
        gone = 0
        handler.removeCallbacks(locCheck)
        handler.postDelayed(locCheck, 20000)
    }

    private fun refreshWidget() {
        try {
            TilesWidget.refresh(applicationContext)
        } catch (e: Exception) {
        }
    }

    private fun changed() {
        onChange?.invoke()
        refreshWidget()
        // The system can still list a just-removed notification for a moment: count again shortly after.
        handler.postDelayed({
            onChange?.invoke()
            refreshWidget()
        }, 800)
    }
}

object Access {
    fun enabled(c: Context): Boolean {
        val s = Settings.Secure.getString(c.contentResolver, "enabled_notification_listeners")
        return s?.contains(c.packageName) == true
    }

    fun rebindIfNeeded(c: Context) {
        if (enabled(c) && NotifListener.instance == null) {
            NotificationListenerService.requestRebind(ComponentName(c, NotifListener::class.java))
        }
    }
}
