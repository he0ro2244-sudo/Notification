package com.example.notiftiles

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.util.TypedValue
import android.service.notification.StatusBarNotification
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService

class TilesWidget : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_CLEAR) {
            if (Prefs.clearMode(context) == 1) {
                Extras.markAllRead(context)
            } else {
                val svc = NotifListener.instance
                if (svc != null) {
                    val keys = Feed.items(context).filter { it.isClearable }.map { it.key }.toTypedArray()
                    if (keys.isNotEmpty()) svc.cancelNotifications(keys)
                }
            }
            refresh(context)
        }
        if (intent.action == ACTION_TICK) {
            Extras.tick(context)
            refresh(context)
        }
        if (intent.action == ACTION_PAGE_PREV || intent.action == ACTION_PAGE_NEXT) {
            val total = Arrange.tileCount(context, Feed.items(context))
            Arrange.move(context, "w", total, if (intent.action == ACTION_PAGE_NEXT) 1 else -1)
            refresh(context)
        }
        if (intent.action == ACTION_WEATHER) fetchWeatherAsync(context)
        if (intent.action == ACTION_REFRESH_ALL) {
            // Re-count messages, redraw everything and fetch fresh weather.
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, TilesWidget::class.java))
            for (id in ids) mgr.updateAppWidget(id, build(context, id))
            refresh(context)
            android.widget.Toast.makeText(context, "Refreshing…", android.widget.Toast.LENGTH_SHORT).show()
            fetchWeatherAsync(context, true)
        }
    }

    override fun onEnabled(context: Context) {
        scheduleWeather(context)
        Extras.tick(context)
    }

    override fun onDisabled(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        am.cancel(weatherPending(context))
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        scheduleWeather(context)
        Extras.tick(context)
        for (id in appWidgetIds) appWidgetManager.updateAppWidget(id, build(context, id))
        fetchWeatherAsync(context)
    }

    private fun fetchWeatherAsync(context: Context, tell: Boolean = false) {
        val app = context.applicationContext
        val pr = goAsync()
        Thread {
            try {
                val ok = Weather.fetch(app)
                refresh(app)
                if (tell) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        android.widget.Toast.makeText(
                            app,
                            if (ok) "Refreshed: " + Weather.summary(app) else "Could not reach the weather service",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } finally {
                pr.finish()
            }
        }.start()
    }

    companion object {
        private const val WEATHER_EVERY_MS = 15 * 60 * 1000L

        private fun weatherPending(ctx: Context): PendingIntent {
            val i = Intent(ctx, TilesWidget::class.java).setAction(ACTION_WEATHER)
            return PendingIntent.getBroadcast(ctx, 99, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        /** Refreshes the weather about every 15 minutes, even when nothing else updates the widget. */
        fun scheduleWeather(ctx: Context) {
            try {
                val am = ctx.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                am.setInexactRepeating(
                    android.app.AlarmManager.ELAPSED_REALTIME,
                    android.os.SystemClock.elapsedRealtime() + WEATHER_EVERY_MS,
                    WEATHER_EVERY_MS,
                    weatherPending(ctx)
                )
            } catch (e: Exception) {
                // widget still updates on its normal schedule
            }
        }

        const val ACTION_CLEAR = "com.example.notiftiles.CLEAR_ALL"
        const val ACTION_REFRESH_ALL = "com.example.notiftiles.REFRESH_ALL"
        const val ACTION_WEATHER = "com.example.notiftiles.REFRESH_WEATHER"
        const val ACTION_TICK = "com.example.notiftiles.TICK"
        const val ACTION_PAGE_PREV = "com.example.notiftiles.PAGE_PREV"
        const val ACTION_PAGE_NEXT = "com.example.notiftiles.PAGE_NEXT"

        private val W_CLOCK = intArrayOf(R.id.wclock0, R.id.wclock1, R.id.wclock2)
        private val W_WEATHER = intArrayOf(R.id.wweather0, R.id.wweather1, R.id.wweather2)
        private val W_DATE = intArrayOf(R.id.wdate0, R.id.wdate1, R.id.wdate2)
        private val W_LOC = intArrayOf(R.id.wlocA, R.id.wlocB)
        private val W_PIN = intArrayOf(R.id.wpinA, R.id.wpinB)

        private fun clickAll(rv: RemoteViews, ids: IntArray, pi: PendingIntent) {
            for (id in ids) rv.setOnClickPendingIntent(id, pi)
        }

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
            val rec = Intent(ctx, RecordActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            rv.setOnClickPendingIntent(
                R.id.wmic,
                PendingIntent.getActivity(ctx, 40, rec, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            )
            val clear = Intent(ctx, TilesWidget::class.java).setAction(ACTION_CLEAR)
            rv.setOnClickPendingIntent(
                R.id.wclear,
                PendingIntent.getBroadcast(ctx, 3, clear, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            )
            val pagePrev = Intent(ctx, TilesWidget::class.java).setAction(ACTION_PAGE_PREV)
            rv.setOnClickPendingIntent(
                R.id.wprev,
                PendingIntent.getBroadcast(ctx, 50, pagePrev, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            )
            val pageNext = Intent(ctx, TilesWidget::class.java).setAction(ACTION_PAGE_NEXT)
            rv.setOnClickPendingIntent(
                R.id.wnext,
                PendingIntent.getBroadcast(ctx, 51, pageNext, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
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
            clickAll(rv, W_WEATHER, PendingIntent.getActivity(ctx, 4, wx, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            val clock = Intent(ctx, OpenClockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            clickAll(rv, W_CLOCK, PendingIntent.getActivity(ctx, 6, clock, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            mapsIcon(rv, ctx)
            val look = Intent(ctx, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            rv.setOnClickPendingIntent(
                R.id.wset,
                PendingIntent.getActivity(ctx, 8, look, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            )
            val all = Intent(ctx, RefreshActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            rv.setOnClickPendingIntent(
                R.id.wrefresh,
                PendingIntent.getActivity(ctx, 11, all, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            )
            val cal = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            clickAll(rv, W_DATE, PendingIntent.getActivity(ctx, 7, cal, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            val where = Intent(ctx, DestinationActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("exit_if_nav", true)
            clickAll(rv, W_LOC, PendingIntent.getActivity(ctx, 5, where, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
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
                for (pin in W_PIN) rv.setImageViewBitmap(pin, bmp)
            } catch (e: Exception) {
                // keep the default pin
            }
        }

        private fun txt(rv: RemoteViews, ctx: Context, id: Int, text: String, baseSp: Float, bold: Boolean) =
            Look.setText(rv, ctx, id, text, baseSp, bold)

        fun setWeather(rv: RemoteViews, ctx: Context) {
            for (w in W_WEATHER) txt(rv, ctx, w, Weather.text(ctx), 13f, true)
            val upd = Weather.updatedText(ctx)
            if (Weather.showUpdated(ctx) && upd.isNotEmpty()) {
                rv.setViewVisibility(R.id.wupdated, android.view.View.VISIBLE)
                rv.setTextViewText(R.id.wupdated, Look.span(ctx, upd, false))
                rv.setTextViewTextSize(R.id.wupdated, TypedValue.COMPLEX_UNIT_SP, 10f * Look.scale(ctx) * Prefs.updSize(ctx) / 100f)
            } else {
                rv.setTextViewText(R.id.wupdated, "")
                rv.setViewVisibility(R.id.wupdated, android.view.View.GONE)
            }
        }

        /** Full redraw of every widget (not a partial update), for switches that must take effect at once. */
        fun rebuildAll(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, TilesWidget::class.java))
            if (ids.isEmpty()) return
            for (id in ids) mgr.updateAppWidget(id, build(ctx, id))
            mgr.notifyAppWidgetViewDataChanged(ids, R.id.grid)
        }

        private fun bgImage(rv: RemoteViews, ctx: Context, viewId: Int, kind: String) {
            val bmp = Look.bitmap(ctx, kind)
            if (bmp != null) {
                rv.setImageViewBitmap(viewId, bmp)
                rv.setInt(viewId, "setImageAlpha", Prefs.imgAlpha(ctx, kind) * 255 / 100)
                rv.setViewVisibility(viewId, View.VISIBLE)
            } else {
                rv.setViewVisibility(viewId, View.GONE)
            }
        }

        private fun setDynamic(rv: RemoteViews, ctx: Context) {
            setCount(rv, ctx)
            setWeather(rv, ctx)
            applyLayout(rv, ctx)

            // ---- Backgrounds: colour + opacity, then the optional pictures
            rv.setColorStateList(R.id.root, "setBackgroundTintList", ColorStateList.valueOf(Prefs.widgetBgArgb(ctx)))
            bgImage(rv, ctx, R.id.wbgimg, "widget")
            bgImage(rv, ctx, R.id.dialimg, "dial")
            val dbg = Prefs.dialBg(ctx)
            val dialTint = if (dbg != 0) ((Prefs.dialBgAlpha(ctx) * 255 / 100) shl 24) or (dbg and 0xFFFFFF) else 0
            rv.setColorStateList(R.id.dialwrap, "setBackgroundTintList", ColorStateList.valueOf(dialTint))

            // ---- Text colours (a picked text colour wins over the automatic light/dark choice)
            val custom = Prefs.widgetBg(ctx)
            val lightText = custom != 0 && android.graphics.Color.luminance(custom) < 0.5f
            val darkText = custom != 0 && !lightText
            val ov = Prefs.textColor(ctx)
            val main = if (ov != 0) ov else if (lightText) 0xFFF2F2F5.toInt() else if (darkText) 0xFF1B1B1F.toInt() else ctx.getColor(R.color.nt_text)
            val sub = if (ov != 0) Feed.alpha(ov, 0xB3) else if (lightText) 0xFFB4B4BC.toInt() else if (darkText) 0xFF55555C.toInt() else ctx.getColor(R.color.nt_sub)
            txt(rv, ctx, R.id.hdr_title, "Inbox", 16f, true)
            rv.setTextColor(R.id.hdr_title, Prefs.partColor(ctx, "title").let { if (it != 0) it else main })
            txt(rv, ctx, R.id.wnote, "+ Note", 13f, true)
            txt(rv, ctx, R.id.wclear, if (Prefs.clearMode(ctx) == 1) "Read all" else "Clear all", 13f, false)
            val ic = Prefs.iconColor(ctx)
            rv.setTextColor(R.id.wclear, Prefs.partColor(ctx, "clear").let { if (it != 0) it else if (ic != 0) ic else sub })
            rv.setInt(R.id.wset, "setColorFilter", Prefs.partColor(ctx, "gear").let { if (it != 0) it else if (ic != 0) ic else sub })
            val gs = Prefs.gearSize(ctx).toFloat()
            rv.setViewLayoutWidth(R.id.wset, gs + 6f, TypedValue.COMPLEX_UNIT_DIP)
            rv.setViewLayoutHeight(R.id.wset, gs + 4f, TypedValue.COMPLEX_UNIT_DIP)
            txt(rv, ctx, R.id.empty_txt, Extras.quietText(ctx) ?: Prefs.emptyText(ctx), 13f, false)
            // Hide only the "Nothing new" wording; the snooze / quiet-hours message always stays visible.
            rv.setViewVisibility(
                R.id.empty_txt,
                if (Prefs.showEmptyText(ctx) || Extras.quietText(ctx) != null) android.view.View.VISIBLE else android.view.View.GONE
            )
            rv.setTextColor(R.id.empty_txt, Prefs.partColor(ctx, "empty").let { if (it != 0) it else sub })
            rv.setViewVisibility(R.id.wrefresh, if (Prefs.refreshShow(ctx)) android.view.View.VISIBLE else android.view.View.GONE)
            val rc = Prefs.partColor(ctx, "refresh").let { if (it != 0) it else if (ic != 0) ic else Feed.alpha(sub, 0x99) }
            rv.setInt(R.id.wrefresh, "setColorFilter", rc)
            rv.setTextColor(R.id.wupdated, Prefs.partColor(ctx, "updated").let { if (it != 0) it else Feed.alpha(sub, 0x99) })
            val rs = Prefs.refreshSize(ctx).toFloat()
            rv.setViewLayoutWidth(R.id.wrefresh, rs + 12f, TypedValue.COMPLEX_UNIT_DIP)
            rv.setViewLayoutHeight(R.id.wrefresh, rs + 12f, TypedValue.COMPLEX_UNIT_DIP)

            // ---- Clock / weather / date circles and the Where to? box
            val circ = Prefs.circleColor(ctx)
            val circleTint = ColorStateList.valueOf(if (circ != 0) circ else ctx.getColor(R.color.nt_circle))
            val circleText = if (ov != 0) ov else if (circ != 0) Look.contrast(circ) else ctx.getColor(R.color.nt_text)
            val circleSub = if (ov != 0 || circ != 0) Feed.alpha(circleText, 0xB3) else ctx.getColor(R.color.nt_sub)
            val shapeRes = Look.shapeRes(ctx)
            val topSize = Prefs.topSize(ctx).toFloat()
            val clockFmt = Prefs.clockFmt(ctx)
            val f12 = if (clockFmt == 2) "HH:mm" else "h:mm"
            val f24 = if (clockFmt == 1) "h:mm" else "HH:mm"
            val dateFmt = Prefs.DATE_FORMATS[Prefs.dateFmt(ctx)].first
            for (slot in 0 until 3) {
                for (id in intArrayOf(W_CLOCK[slot], W_WEATHER[slot], W_DATE[slot])) {
                    rv.setColorStateList(id, "setBackgroundTintList", circleTint)
                    rv.setInt(id, "setBackgroundResource", shapeRes)
                    rv.setTextColor(id, circleText)
                    rv.setViewLayoutWidth(id, topSize, TypedValue.COMPLEX_UNIT_DIP)
                    rv.setViewLayoutHeight(id, topSize, TypedValue.COMPLEX_UNIT_DIP)
                }
                for ((k, ids) in listOf("clock" to W_CLOCK, "weather" to W_WEATHER, "date" to W_DATE)) {
                    val pc = Prefs.partColor(ctx, k)
                    if (pc != 0) rv.setTextColor(ids[slot], pc)
                }
                val clock = W_CLOCK[slot]
                rv.setCharSequence(clock, "setFormat12Hour", Look.span(ctx, f12, true))
                rv.setCharSequence(clock, "setFormat24Hour", Look.span(ctx, f24, true))
                rv.setTextViewTextSize(clock, TypedValue.COMPLEX_UNIT_SP, 14f * Look.scale(ctx))
                val date = W_DATE[slot]
                rv.setCharSequence(date, "setFormat12Hour", Look.span(ctx, dateFmt, true))
                rv.setCharSequence(date, "setFormat24Hour", Look.span(ctx, dateFmt, true))
                rv.setTextViewTextSize(date, TypedValue.COMPLEX_UNIT_SP, 12f * Look.scale(ctx))
            }
            for (i in 0 until 2) {
                rv.setColorStateList(W_LOC[i], "setBackgroundTintList", circleTint)
            }
            // While Google Maps is navigating this box becomes a one-tap red "Exit navigation"
            val navigating = LocationAuto.navNotification() != null
            val whereText = if (navigating) "Exit navigation" else "Where to?"
            txt(rv, ctx, R.id.wwA, whereText, 12f, false)
            txt(rv, ctx, R.id.wwB, whereText, 12f, false)
            val wc = Prefs.partColor(ctx, "where")
            val whereColor = if (navigating) 0xFFE53935.toInt() else if (wc != 0) wc else circleSub
            rv.setTextColor(R.id.wwA, whereColor)
            rv.setTextColor(R.id.wwB, whereColor)

            // ---- Fast-dial circles
            val dark = Feed.isDark(ctx)
            val filled = Feed.style("com.whatsapp")
            val empty = Feed.style("")
            val dc = Prefs.dialCircle(ctx)
            val dsize = Prefs.dialSize(ctx).toFloat()
            val showNames = Prefs.dialNames(ctx)
            for (i in 0 until Prefs.DIAL_SLOTS) {
                val name = Prefs.dialName(ctx, i)
                val has = Prefs.dialNumber(ctx, i).isNotEmpty()
                val st = if (has) filled else empty
                val label = if (name.isNotBlank()) name else if (has) Prefs.dialNumber(ctx, i) else ""
                val initial = if (name.isNotBlank()) name.trim().first().uppercaseChar().toString() else if (has) "☎" else "+"
                val own = Prefs.dialCircleOf(ctx, i)
                val bgc = if (own != 0) own else if (dc != 0) dc else st.bg(dark)
                val autoFg = if (own != 0) Look.contrast(own) else if (dc != 0) Look.contrast(dc) else st.fg(dark)
                val ownText = Prefs.dialTextOf(ctx, i)
                val letterC = Prefs.partColor(ctx, "dialletter")
                val fgc = if (ownText != 0) ownText else if (letterC != 0) letterC else autoFg
                txt(rv, ctx, DIAL_CIRCLE[i], initial, 18f, true)
                rv.setTextColor(DIAL_CIRCLE[i], fgc)
                rv.setColorStateList(DIAL_CIRCLE[i], "setBackgroundTintList", ColorStateList.valueOf(bgc))
                rv.setInt(DIAL_CIRCLE[i], "setBackgroundResource", shapeRes)
                rv.setViewLayoutWidth(DIAL_CIRCLE[i], dsize, TypedValue.COMPLEX_UNIT_DIP)
                rv.setViewLayoutHeight(DIAL_CIRCLE[i], dsize, TypedValue.COMPLEX_UNIT_DIP)
                txt(rv, ctx, DIAL_NAME[i], if (has) label else "Add", 10f, false)
                val dn = Prefs.partColor(ctx, "dialnames")
                rv.setTextColor(DIAL_NAME[i], if (ownText != 0) ownText else if (dn != 0) dn else sub)
                rv.setViewVisibility(DIAL_NAME[i], if (showNames) View.VISIBLE else View.GONE)
            }
            applyItems(rv, ctx, sub)
        }

        private fun vis(on: Boolean, off: Int): Int = if (on) View.VISIBLE else off

        private fun set3d(rv: RemoteViews, ctx: Context, id: Int, res: Int, color: Int, dp: Float) {
            val bmp = Look.icon3d(ctx, res, color, dp)
            if (bmp != null) {
                rv.setImageViewBitmap(id, bmp)
                rv.setInt(id, "setColorFilter", 0)
            } else {
                rv.setImageViewResource(id, res)
            }
        }

        // Show / hide switches, the page bar and 3D icons. Everything is shown and flat unless changed in Look & colors.
        private fun applyItems(rv: RemoteViews, ctx: Context, sub: Int) {
            rv.setViewVisibility(R.id.hdr_title, vis(Prefs.showItem(ctx, "w_title"), View.INVISIBLE))
            rv.setViewVisibility(R.id.wmic, vis(Prefs.showItem(ctx, "w_mic"), View.GONE))
            rv.setViewVisibility(R.id.wnote, vis(Prefs.showItem(ctx, "w_note"), View.GONE))
            rv.setViewVisibility(R.id.wclear, vis(Prefs.showItem(ctx, "w_clear"), View.GONE))
            rv.setViewVisibility(R.id.wset, vis(Prefs.showItem(ctx, "w_gear"), View.GONE))

            // ---- Pages (‹ 1/3 ›)
            val pages = Prefs.pagesOn(ctx)
            rv.setViewVisibility(R.id.pagebar, if (pages) View.VISIBLE else View.GONE)
            if (pages) {
                val total = Arrange.tileCount(ctx, Feed.items(ctx))
                val n = Arrange.pageCount(ctx, total)
                val now = Arrange.pageNow(ctx, "w", total)
                val pc = Prefs.partColor(ctx, "pages").let { if (it != 0) it else Prefs.accent(ctx) }
                val pn = Arrange.pageName(ctx, "w")
                txt(rv, ctx, R.id.wpage, (if (pn != null) "$pn  " else "") + "${now + 1}/$n", 12f, false)
                rv.setTextColor(R.id.wpage, pc)
                rv.setTextColor(R.id.wprev, pc)
                rv.setTextColor(R.id.wnext, pc)
            }

            // ---- 3D icons
            val ic = Prefs.iconColor(ctx)
            if (Prefs.icons3d(ctx)) {
                val gearC = Prefs.partColor(ctx, "gear").let { if (it != 0) it else if (ic != 0) ic else sub }
                val refC = Prefs.partColor(ctx, "refresh").let { if (it != 0) it else if (ic != 0) ic else sub }
                val rec = RecordService.running()
                val micC = if (rec) 0xFFE53935.toInt() else Prefs.partColor(ctx, "mic").let { if (it != 0) it else if (ic != 0) ic else Prefs.accent(ctx) }
                set3d(rv, ctx, R.id.wset, R.drawable.ic_gear, gearC, Prefs.gearSize(ctx).toFloat())
                set3d(rv, ctx, R.id.wrefresh, R.drawable.ic_refresh, refC, Prefs.refreshSize(ctx).toFloat())
                set3d(rv, ctx, R.id.wmic, if (rec) R.drawable.ic_stop else R.drawable.ic_mic, micC, Prefs.micSize(ctx).toFloat())
            } else {
                rv.setImageViewResource(R.id.wset, R.drawable.ic_gear)
                rv.setImageViewResource(R.id.wrefresh, R.drawable.ic_refresh)
            }
        }

        // Which top-row items are shown, in which order, and the accent colour.
        private fun applyLayout(rv: RemoteViews, ctx: Context) {
            val order = Prefs.order(ctx)
            val groups = mapOf("clock" to W_CLOCK, "weather" to W_WEATHER, "date" to W_DATE)
            for (slot in 0 until 3) {
                for ((key, ids) in groups) {
                    val on = order[slot] == key && Prefs.show(ctx, key)
                    rv.setViewVisibility(ids[slot], if (on) View.VISIBLE else View.GONE)
                }
            }
            val maps = Prefs.show(ctx, "maps")
            val first = Prefs.pillFirst(ctx)
            rv.setViewVisibility(R.id.wlocA, if (maps && first) View.VISIBLE else View.GONE)
            rv.setViewVisibility(R.id.wlocB, if (maps && !first) View.VISIBLE else View.GONE)
            rv.setViewVisibility(R.id.dialwrap, if (Prefs.show(ctx, "dial")) View.VISIBLE else View.GONE)
            val accent = Prefs.accent(ctx)
            rv.setTextColor(R.id.wnote, Prefs.partColor(ctx, "note").let { if (it != 0) it else accent })
            val recNow = RecordService.running()
            rv.setImageViewResource(R.id.wmic, if (recNow) R.drawable.ic_stop else R.drawable.ic_mic)
            rv.setInt(R.id.wmic, "setColorFilter", if (recNow) 0xFFE53935.toInt() else Prefs.partColor(ctx, "mic").let { if (it != 0) it else if (Prefs.iconColor(ctx) != 0) Prefs.iconColor(ctx) else accent })
            val ms = Prefs.micSize(ctx).toFloat()
            rv.setViewLayoutWidth(R.id.wmic, ms + 12f, TypedValue.COMPLEX_UNIT_DIP)
            rv.setViewLayoutHeight(R.id.wmic, ms + 4f, TypedValue.COMPLEX_UNIT_DIP)
            rv.setColorStateList(R.id.count, "setBackgroundTintList", ColorStateList.valueOf(accent))
        }

        private fun setCount(rv: RemoteViews, ctx: Context) {
            val n = Feed.items(ctx).size
            txt(rv, ctx, R.id.count, n.toString(), 12f, true)
            rv.setViewVisibility(R.id.count, if (n > 0 && Prefs.showItem(ctx, "w_count")) View.VISIBLE else View.GONE)
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
                    setWeather(r2, app)
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
    private var tiles: List<Tile> = emptyList()
    private var shortcuts: List<String> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        // One tile per chat (or per app when grouped), in the chosen order; only the current page when pages are on.
        tiles = Arrange.slice(ctx, "w", Arrange.tiles(ctx, Feed.items(ctx), 4))
        shortcuts = Arrange.shortcuts(ctx, "w")
    }

    override fun onDestroy() {}

    // Endless scrolling: the list repeats when you reach its end (only with 3 or more tiles, and not in pages mode)
    private fun endless(): Boolean = Prefs.loop(ctx) && Prefs.scrollMode(ctx) == 0 && tiles.size >= 3

    override fun getCount(): Int = if (endless()) tiles.size * 30 else tiles.size + shortcuts.size

    // My pages: an app shortcut tile (tap opens the app).
    private fun shortcutView(pkg: String): RemoteViews {
        val dark = Feed.isDark(ctx)
        val st = Feed.style(ctx, pkg)
        val bg = st.bg(dark)
        val fg = st.fg(dark)
        val rv = RemoteViews(ctx.packageName, R.layout.widget_tile)
        rv.setColorStateList(R.id.tile_root, "setBackgroundTintList", ColorStateList.valueOf(bg))
        Look.setText(rv, ctx, R.id.appname, Feed.label(ctx, pkg), 11f, false)
        rv.setTextColor(R.id.appname, Prefs.partColor(ctx, "tapp").let { if (it != 0) it else Feed.alpha(fg, 0xCC) })
        Look.setText(rv, ctx, R.id.title, "Shortcut", 13f, true)
        rv.setTextColor(R.id.title, Prefs.partColor(ctx, "ttitle").let { if (it != 0) it else fg })
        Look.setText(rv, ctx, R.id.text, "Tap to open", 12f, false)
        rv.setTextColor(R.id.text, Prefs.partColor(ctx, "ttext").let { if (it != 0) it else Feed.alpha(fg, 0xDD) })
        rv.setViewVisibility(R.id.cnt, View.GONE)
        rv.setViewVisibility(R.id.appname, if (Prefs.showItem(ctx, "t_app")) View.VISIBLE else View.INVISIBLE)
        rv.setViewVisibility(R.id.title, if (Prefs.showItem(ctx, "t_title")) View.VISIBLE else View.GONE)
        rv.setViewVisibility(R.id.text, if (Prefs.showItem(ctx, "t_text")) View.VISIBLE else View.GONE)
        rv.setOnClickFillInIntent(R.id.tile_root, Intent().putExtra("launch", pkg))
        return rv
    }

    override fun getViewAt(position: Int): RemoteViews? {
        if (!endless() && position >= tiles.size) {
            val pkg = shortcuts.getOrNull(position - tiles.size) ?: return null
            return shortcutView(pkg)
        }
        if (tiles.isEmpty()) return null
        val tile = tiles[position % tiles.size]
        val sbn = tile.sbn
        val dark = Feed.isDark(ctx)
        val st = Feed.style(ctx, sbn.packageName)
        val bg = st.bg(dark)
        val fg = st.fg(dark)

        val rv = RemoteViews(ctx.packageName, R.layout.widget_tile)
        rv.setColorStateList(R.id.tile_root, "setBackgroundTintList", ColorStateList.valueOf(bg))
        Look.setText(rv, ctx, R.id.appname, Feed.label(ctx, sbn.packageName), 11f, false)
        rv.setTextColor(R.id.appname, Prefs.partColor(ctx, "tapp").let { if (it != 0) it else Feed.alpha(fg, 0xCC) })
        Look.setText(rv, ctx, R.id.cnt, tile.count.toString(), 11f, true)
        rv.setTextColor(R.id.cnt, bg)
        rv.setColorStateList(R.id.cnt, "setBackgroundTintList", ColorStateList.valueOf(fg))
        Look.setText(rv, ctx, R.id.title, tile.title, 13f, true)
        rv.setTextColor(R.id.title, Prefs.partColor(ctx, "ttitle").let { if (it != 0) it else fg })
        Look.setText(rv, ctx, R.id.text, tile.text, 12f, false)
        rv.setTextColor(R.id.text, Prefs.partColor(ctx, "ttext").let { if (it != 0) it else Feed.alpha(fg, 0xDD) })

        // Show / hide the parts of a tile
        val showApp = Prefs.showItem(ctx, "t_app")
        val showCnt = Prefs.showItem(ctx, "t_cnt")
        rv.setViewVisibility(R.id.appname, if (showApp) View.VISIBLE else View.INVISIBLE)
        rv.setViewVisibility(R.id.cnt, if (showCnt) View.VISIBLE else View.GONE)
        rv.setViewVisibility(R.id.tile_head, if (showApp || showCnt) View.VISIBLE else View.GONE)
        rv.setViewVisibility(R.id.title, if (Prefs.showItem(ctx, "t_title")) View.VISIBLE else View.GONE)
        rv.setViewVisibility(R.id.text, if (Prefs.showItem(ctx, "t_text")) View.VISIBLE else View.GONE)

        rv.setOnClickFillInIntent(R.id.tile_root, Intent().putExtra("key", sbn.key))
        return rv
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long {
        if (!endless() && position >= tiles.size) return ("sc" + shortcuts.getOrNull(position - tiles.size)).hashCode().toLong()
        if (tiles.isEmpty()) return position.toLong()
        return tiles[position % tiles.size].sbn.key.hashCode().toLong()
    }
    override fun hasStableIds(): Boolean = false
}
