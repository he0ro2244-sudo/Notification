package com.example.notiftiles

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.service.notification.StatusBarNotification
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService

class TilesWidget : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_CLEAR) {
            val svc = NotifListener.instance
            if (svc != null) {
                val keys = Feed.items(context).filter { it.isClearable }.map { it.key }.toTypedArray()
                if (keys.isNotEmpty()) svc.cancelNotifications(keys)
            }
            refresh(context)
        }
        if (intent.action == ACTION_WEATHER) fetchWeatherAsync(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) appWidgetManager.updateAppWidget(id, build(context, id))
        fetchWeatherAsync(context)
    }

    private fun fetchWeatherAsync(context: Context) {
        val app = context.applicationContext
        val pr = goAsync()
        Thread {
            try {
                Weather.fetch(app)
                refresh(app)
            } finally {
                pr.finish()
            }
        }.start()
    }

    companion object {
        const val ACTION_CLEAR = "com.example.notiftiles.CLEAR_ALL"
        const val ACTION_WEATHER = "com.example.notiftiles.REFRESH_WEATHER"

        private val DIAL_CIRCLE = intArrayOf(R.id.dc0, R.id.dc1, R.id.dc2, R.id.dc3, R.id.dc4)
        private val DIAL_NAME = intArrayOf(R.id.dn0, R.id.dn1, R.id.dn2, R.id.dn3, R.id.dn4)
        private val DIAL_BOX = intArrayOf(R.id.dial0, R.id.dial1, R.id.dial2, R.id.dial3, R.id.dial4)

        fun build(ctx: Context, id: Int): RemoteViews {
            val rv = RemoteViews(ctx.packageName, R.layout.widget_tiles)

            val svc = Intent(ctx, TilesService::class.java)
            svc.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            svc.data = Uri.parse(svc.toUri(Intent.URI_INTENT_SCHEME))
            rv.setRemoteAdapter(R.id.grid, svc)
            rv.setEmptyView(R.id.grid, R.id.empty)

            val open = Intent(ctx, PanelActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            rv.setOnClickPendingIntent(
                R.id.header,
                PendingIntent.getActivity(ctx, 0, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            )
            val newNote = Intent(ctx, PanelActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("key", "note:new")
            rv.setOnClickPendingIntent(
                R.id.wnote,
                PendingIntent.getActivity(ctx, 2, newNote, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            )
            val clear = Intent(ctx, TilesWidget::class.java).setAction(ACTION_CLEAR)
            rv.setOnClickPendingIntent(
                R.id.wclear,
                PendingIntent.getBroadcast(ctx, 3, clear, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            )
            val template = Intent(ctx, PanelActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            rv.setPendingIntentTemplate(
                R.id.grid,
                PendingIntent.getActivity(ctx, 1, template, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            )
            for (i in 0 until Prefs.DIAL_SLOTS) {
                val dial = Intent(ctx, DialActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("slot", i)
                rv.setOnClickPendingIntent(
                    DIAL_BOX[i],
                    PendingIntent.getActivity(ctx, 10 + i, dial, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                )
            }
            val wx = Intent(ctx, OpenWeatherActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            rv.setOnClickPendingIntent(
                R.id.wweather,
                PendingIntent.getActivity(ctx, 4, wx, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            )
            val clock = Intent(ctx, OpenClockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            rv.setOnClickPendingIntent(
                R.id.wclock,
                PendingIntent.getActivity(ctx, 6, clock, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            )
            mapsIcon(rv, ctx)
            val look = Intent(ctx, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            rv.setOnClickPendingIntent(
                R.id.wset,
                PendingIntent.getActivity(ctx, 8, look, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            )
            val cal = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            rv.setOnClickPendingIntent(
                R.id.wdate,
                PendingIntent.getActivity(ctx, 7, cal, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            )
            val where = Intent(ctx, DestinationActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            rv.setOnClickPendingIntent(
                R.id.wloc,
                PendingIntent.getActivity(ctx, 5, where, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            )
            setDynamic(rv, ctx)
            return rv
        }

        // Show the real Google Maps icon from this phone; the built-in red pin stays if Maps isn't installed.
        private fun mapsIcon(rv: RemoteViews, ctx: Context) {
            try {
                val d = ctx.packageManager.getApplicationIcon("com.google.android.apps.maps")
                val size = 96
                val bmp = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bmp)
                d.setBounds(0, 0, size, size)
                d.draw(canvas)
                rv.setImageViewBitmap(R.id.wpin, bmp)
            } catch (e: Exception) {
                // keep the default pin
            }
        }

        private fun setDynamic(rv: RemoteViews, ctx: Context) {
            setCount(rv, ctx)
            rv.setTextViewText(R.id.wweather, Weather.text(ctx))
            // Background colour + opacity chosen in Look & colors.
            rv.setColorStateList(R.id.root, "setBackgroundTintList", ColorStateList.valueOf(Prefs.widgetBgArgb(ctx)))
            val custom = Prefs.widgetBg(ctx)
            val lightText = custom != 0 && android.graphics.Color.luminance(custom) < 0.5f
            val darkText = custom != 0 && !lightText
            val main = if (lightText) 0xFFF2F2F5.toInt() else if (darkText) 0xFF1B1B1F.toInt() else ctx.getColor(R.color.nt_text)
            val sub = if (lightText) 0xFFB4B4BC.toInt() else if (darkText) 0xFF55555C.toInt() else ctx.getColor(R.color.nt_sub)
            rv.setTextColor(R.id.hdr_title, main)
            rv.setTextColor(R.id.wclear, sub)
            rv.setTextColor(R.id.wset, sub)
            rv.setTextColor(R.id.empty, sub)
            for (n in DIAL_NAME) rv.setTextColor(n, sub)
            val dark = Feed.isDark(ctx)
            val filled = Feed.style("com.whatsapp")
            val empty = Feed.style("")
            for (i in 0 until Prefs.DIAL_SLOTS) {
                val name = Prefs.dialName(ctx, i)
                val has = Prefs.dialNumber(ctx, i).isNotEmpty()
                val st = if (has) filled else empty
                val label = if (name.isNotBlank()) name else if (has) Prefs.dialNumber(ctx, i) else ""
                val initial = if (name.isNotBlank()) name.trim().first().uppercaseChar().toString() else if (has) "☎" else "+"
                rv.setTextViewText(DIAL_CIRCLE[i], initial)
                rv.setTextColor(DIAL_CIRCLE[i], st.fg(dark))
                rv.setColorStateList(DIAL_CIRCLE[i], "setBackgroundTintList", ColorStateList.valueOf(st.bg(dark)))
                rv.setTextViewText(DIAL_NAME[i], if (has) label else "Add")
            }
        }

        private fun setCount(rv: RemoteViews, ctx: Context) {
            val n = Feed.items(ctx).size
            rv.setTextViewText(R.id.count, n.toString())
            rv.setViewVisibility(R.id.count, if (n > 0) View.VISIBLE else View.GONE)
        }

        fun refresh(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, TilesWidget::class.java))
            if (ids.isEmpty()) return
            val rv = RemoteViews(ctx.packageName, R.layout.widget_tiles)
            setDynamic(rv, ctx)
            for (id in ids) mgr.partiallyUpdateAppWidget(id, rv)
            mgr.notifyAppWidgetViewDataChanged(ids, R.id.grid)
            if (Weather.stale(ctx)) {
                val app = ctx.applicationContext
                Thread {
                    Weather.fetch(app)
                    val r2 = RemoteViews(app.packageName, R.layout.widget_tiles)
                    r2.setTextViewText(R.id.wweather, Weather.text(app))
                    for (id in ids) mgr.partiallyUpdateAppWidget(id, r2)
                }.start()
            }
        }
    }
}

class TilesService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = TilesFactory(applicationContext)
}

class TilesFactory(private val ctx: Context) : RemoteViewsService.RemoteViewsFactory {
    private var items: List<StatusBarNotification> = emptyList()
    private var counts: Map<String, Int> = emptyMap()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        items = Feed.items(ctx)
        counts = items.groupingBy { it.packageName }.eachCount()
    }

    override fun onDestroy() {}

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews? {
        val sbn = items.getOrNull(position) ?: return null
        val dark = Feed.isDark(ctx)
        val st = Feed.style(sbn.packageName)
        val bg = st.bg(dark)
        val fg = st.fg(dark)

        val rv = RemoteViews(ctx.packageName, R.layout.widget_tile)
        rv.setColorStateList(R.id.tile_root, "setBackgroundTintList", ColorStateList.valueOf(bg))
        rv.setTextViewText(R.id.appname, Feed.label(ctx, sbn.packageName))
        rv.setTextColor(R.id.appname, Feed.alpha(fg, 0xCC))
        rv.setTextViewText(R.id.cnt, (counts[sbn.packageName] ?: 1).toString())
        rv.setTextColor(R.id.cnt, bg)
        rv.setColorStateList(R.id.cnt, "setBackgroundTintList", ColorStateList.valueOf(fg))
        rv.setTextViewText(R.id.title, Feed.title(sbn))
        rv.setTextColor(R.id.title, fg)
        rv.setTextViewText(R.id.text, Feed.text(sbn))
        rv.setTextColor(R.id.text, Feed.alpha(fg, 0xDD))
        rv.setOnClickFillInIntent(R.id.tile_root, Intent().putExtra("key", sbn.key))
        return rv
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = items.getOrNull(position)?.key?.hashCode()?.toLong() ?: position.toLong()
    override fun hasStableIds(): Boolean = false
}
