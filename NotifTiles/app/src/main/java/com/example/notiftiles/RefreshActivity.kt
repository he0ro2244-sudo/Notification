package com.example.notiftiles

import android.app.Activity
import android.content.Intent
import android.os.Bundle

// Behind the small refresh icon on the widget. Refreshes the message counts and the online weather only.
// It never opens the phone's weather app: that happens only when you tap the weather circle.
class RefreshActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sendBroadcast(Intent(this, TilesWidget::class.java).setAction(TilesWidget.ACTION_REFRESH_ALL))
        finish()
        overridePendingTransition(0, 0)
    }
}
