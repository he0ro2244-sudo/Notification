package com.example.notiftiles

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

// Behind the small refresh icon on the widget. Refreshes everything, and when the phone-weather option is on
// it opens the phone's weather app for a moment: the helper copies the temperature and closes the app again.
class RefreshActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sendBroadcast(Intent(this, TilesWidget::class.java).setAction(TilesWidget.ACTION_REFRESH_ALL))
        if (Weather.useApp(this)) {
            if (RecorderTapService.enabled(this)) {
                val launch = Weather.findApp(packageManager)
                if (launch != null) {
                    RecorderTapService.autoCloseUntil = System.currentTimeMillis() + 9000L
                    try {
                        startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (e: Exception) {
                        RecorderTapService.autoCloseUntil = 0L
                    }
                } else {
                    Toast.makeText(this, "No weather app found on this phone", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Switch on the Notif Tiles Accessibility helper to copy your phone's weather", Toast.LENGTH_LONG).show()
            }
        }
        finish()
        overridePendingTransition(0, 0)
    }
}
