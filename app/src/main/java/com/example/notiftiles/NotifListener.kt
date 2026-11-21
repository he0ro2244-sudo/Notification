package com.example.notiftiles

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
        changed()
    }

    override fun onListenerDisconnected() {
        instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        Prefs.addSeen(this, sbn.packageName)
        changed()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        changed()
    }

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

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
