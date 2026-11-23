package com.example.notiftiles

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
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

        // One view per item. Each item is added into the row it sits in (top row or Inbox row), see addItems.
        val ITEM_VIEW = mapOf(
            "clock" to R.id.wclock, "weather" to R.id.wweather, "date" to R.id.wdate, "maps" to R.id.wloc,
            "merge" to R.id.wmerge, "title" to R.id.hdr_title, "count" to R.id.count, "mic" to R.id.wmic,
            "note" to R.id.wnote, "clear" to R.id.wclear, "edit" to R.id.wedit, "gear" to R.id.wset
        )
        private val ITEM_LAYOUT = mapOf(
            "clock" to R.layout.item_clock, "weather" to R.layout.item_weather, "date" to R.layout.item_date,
            "maps" to R.layout.item_where, "title" to R.layout.item_title, "count" to R.layout.item_count,
            "mic" to R.layout.item_mic, "note" to R.layout.item_note, "clear" to R.layout.item_clear,
            "edit" to R.layout.item_edit, "gear" to R.layout.item_gear
        )

        /** Free layout: every item that can be placed anywhere, and the view that stands for it. */
        val FREE_VIEW: Map<String, Int> = ITEM_VIEW + mapOf(
            "d0" to R.id.dial0, "d1" to R.id.dial1, "d2" to R.id.dial2, "d3" to R.id.dial3, "d4" to R.id.dial4,
            "q0" to R.id.quick0, "q1" to R.id.quick1, "q2" to R.id.quick2, "q3" to R.id.quick3, "q4" to R.id.quick4
        )
        private val FREE_LAYOUT = mapOf(
            "clock" to R.layout.item_clock, "weather" to R.layout.item_weather, "date" to R.layout.item_date,
            "maps" to R.layout.item_free_where, "merge" to R.layout.item_merge, "title" to R.layout.item_free_title,
            "count" to R.layout.item_count, "mic" to R.layout.item_mic, "note" to R.layout.item_note,
            "clear" to R.layout.item_clear, "edit" to R.layout.item_edit, "gear" to R.layout.item_gear,
            "d0" to R.layout.item_dial0, "d1" to R.layout.item_dial1, "d2" to R.layout.item_dial2,
            "d3" to R.layout.item_dial3, "d4" to R.layout.item_dial4,
            "q0" to R.layout.item_quick0, "q1" to R.layout.item_quick1, "q2" to R.layout.item_quick2,
            "q3" to R.layout.item_quick3, "q4" to R.layout.item_quick4
        )
        private val W_CLOCK = intArrayOf(R.id.wclock)
        private val W_WEATHER = intArrayOf(R.id.wweather)
        private val W_DATE = intArrayOf(R.id.wdate)
        private val W_MERGE = intArrayOf(R.id.wmerge)
        // The Where to? box (one view; it sits where the Move screen put it)
        private val W_LOC = intArrayOf(R.id.wloc)
        private val W_PIN = intArrayOf(R.id.wpin)
        private val W_WW = intArrayOf(R.id.ww)

        private val BLOCK_LAYOUT = mapOf(
            "top" to R.layout.widget_blk_top, "dial" to R.layout.widget_blk_dial,
            "quick" to R.layout.widget_blk_quick, "head" to R.layout.widget_blk_header
        )
        private fun bs(ctx: Context, key: String): Float = Prefs.blockScale(ctx, key) / 100f
        private fun isc(ctx: Context, key: String): Float = Prefs.itemScale(ctx, key) / 100f
        private fun wst(ctx: Context, key: String): Float = Prefs.itemW(ctx, key) / 100f
        private fun hst(ctx: Context, key: String): Float = Prefs.itemH(ctx, key) / 100f

        private fun clickAll(rv: RemoteViews, ids: IntArray, pi: PendingIntent) {
            for (id in ids) rv.setOnClickPendingIntent(id, pi)
        }

        private val DIAL_CIRCLE = intArrayOf(R.id.dc0, R.id.dc1, R.id.dc2, R.id.dc3, R.id.dc4)
        private val DIAL_NAME = intArrayOf(R.id.dn0, R.id.dn1, R.id.dn2, R.id.dn3, R.id.dn4)
        private val DIAL_BOX = intArrayOf(R.id.dial0, R.id.dial1, R.id.dial2, R.id.dial3, R.id.dial4)
        private val Q_CIRCLE = intArrayOf(R.id.qc0, R.id.qc1, R.id.qc2, R.id.qc3, R.id.qc4)
        private val Q_ICON = intArrayOf(R.id.qi0, R.id.qi1, R.id.qi2, R.id.qi3, R.id.qi4)
        private val Q_NAME = intArrayOf(R.id.qn0, R.id.qn1, R.id.qn2, R.id.qn3, R.id.qn4)
        private val Q_BOX = intArrayOf(R.id.quick0, R.id.quick1, R.id.quick2, R.id.quick3, R.id.quick4)

        // App icons are drawn once and kept for ten minutes: the widget is redrawn on every notification
        private val iconCache = HashMap<String, Pair<Long, android.graphics.Bitmap>>()

        private fun appIconBitmap(ctx: Context, pkg: String): android.graphics.Bitmap? {
            val now = System.currentTimeMillis()
            synchronized(iconCache) {
                val e = iconCache[pkg]
                if (e != null && now - e.first < 10 * 60 * 1000L) return e.second
            }
            val b = drawIcon(ctx, pkg)
            if (b != null) synchronized(iconCache) {
                if (iconCache.size > 24) iconCache.clear()
                iconCache[pkg] = Pair(now, b)
            }
            return b
        }

        private fun drawIcon(ctx: Context, pkg: String): android.graphics.Bitmap? = try {
            val d = ctx.packageManager.getApplicationIcon(pkg)
            val b = android.graphics.Bitmap.createBitmap(96, 96, android.graphics.Bitmap.Config.ARGB_8888)
            d.setBounds(0, 0, 96, 96)
            d.draw(android.graphics.Canvas(b))
            b
        } catch (e: Exception) {
            null
        }

        fun build(ctx: Context, id: Int): RemoteViews {
            val rv = RemoteViews(ctx.packageName, R.layout.widget_tiles)
            // The sections above the tiles are added in the order chosen in Arrange (they must exist before anything below is set on them).
            // Clear first: the widget host re-applies these steps on every refresh and would otherwise stack a second copy.
            rv.removeAllViews(R.id.blocks)
            rv.removeAllViews(R.id.canvas)
            val free = Prefs.freeOn(ctx)
            if (free) {
                // Free layout: no rows and no sections, every item is added on its own and placed by applyFree
                addFree(rv, ctx)
            } else {
                for (key in Prefs.blockOrder(ctx)) rv.addView(R.id.blocks, RemoteViews(ctx.packageName, BLOCK_LAYOUT.getValue(key)))
                addItems(rv, ctx)
            }

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
            for (i in 0 until Prefs.QUICK_SLOTS) {
                val qi = Intent(ctx, QuickActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("slot", i)
                rv.setOnClickPendingIntent(
                    Q_BOX[i],
                    PendingIntent.getActivity(ctx, 60 + i, qi, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                )
            }
            val wx = Intent(ctx, OpenWeatherActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            clickAll(rv, W_WEATHER, PendingIntent.getActivity(ctx, 4, wx, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            val clock = Intent(ctx, OpenClockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            clickAll(rv, W_CLOCK + W_MERGE, PendingIntent.getActivity(ctx, 6, clock, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
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
            if (free) {
                // The title and the count used to be part of the Inbox row, which opened the panel
                clickAll(rv, intArrayOf(R.id.hdr_title, R.id.count),
                    PendingIntent.getActivity(ctx, 0, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
                applyFree(rv, ctx, id)
            }
            setDynamic(rv, ctx)
            return rv
        }

        /** Free layout: adds every item to the canvas (the views are placed by applyFree). */
        private fun addFree(rv: RemoteViews, ctx: Context) {
            for (key in Prefs.FREE_KEYS) rv.addView(R.id.canvas, RemoteViews(ctx.packageName, FREE_LAYOUT.getValue(key)))
        }

        /** Free layout: puts each item at its own place (dp from the top-left corner) and the message tiles under the chosen line. */
        private fun applyFree(rv: RemoteViews, ctx: Context, widgetId: Int) {
            val pos = Prefs.freePos(ctx)
            // The places were made for one widget width: when the widget is resized they are stretched to fit
            val curW = try {
                AppWidgetManager.getInstance(ctx).getAppWidgetOptions(widgetId).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0).toFloat()
            } catch (e: Exception) { 0f }
            val scale = Prefs.freeScaleFor(ctx, curW)
            for ((n, key) in Prefs.FREE_KEYS.withIndex()) {
                val id = FREE_VIEW.getValue(key)
                val p = pos[key]
                val x = if (p != null) p.x * scale else (8f + (n % 8) * 34f)
                val y = p?.y ?: (Prefs.freeGridTop(ctx) - 40f).coerceAtLeast(0f)
                rv.setViewLayoutMargin(id, RemoteViews.MARGIN_START, x, TypedValue.COMPLEX_UNIT_DIP)
                rv.setViewLayoutMargin(id, RemoteViews.MARGIN_TOP, y, TypedValue.COMPLEX_UNIT_DIP)
                if (key == "maps" && p != null && p.w > 0f) rv.setViewLayoutWidth(id, p.w * scale, TypedValue.COMPLEX_UNIT_DIP)
            }
            // The content area has an 8dp padding of its own
            rv.setViewLayoutMargin(R.id.gridwrap, RemoteViews.MARGIN_TOP, (Prefs.freeGridTop(ctx) - 8f).coerceAtLeast(0f), TypedValue.COMPLEX_UNIT_DIP)
        }

        /**
         * Puts every item into the row it sits in, in the order chosen with Move. The merged shape always exists in the
         * top row (hidden unless two or more of its items are on): it sits where the first of them is.
         */
        private fun addItems(rv: RemoteViews, ctx: Context) {
            val rows = Prefs.rows(ctx)
            val top = rows["top"] ?: emptyList()
            val merged = Prefs.merged(ctx).filter { it in top }
            val mergeAt = if (merged.size >= 2) top.indexOfFirst { it in merged } else top.size
            for ((i, key) in top.withIndex()) {
                if (i == mergeAt) rv.addView(R.id.top_row, RemoteViews(ctx.packageName, R.layout.item_merge))
                rv.addView(R.id.top_row, RemoteViews(ctx.packageName, itemLayoutFor(ctx, key)))
            }
            if (mergeAt == top.size) rv.addView(R.id.top_row, RemoteViews(ctx.packageName, R.layout.item_merge))
            for (key in rows["head"] ?: emptyList()) rv.addView(R.id.header, RemoteViews(ctx.packageName, itemLayoutFor(ctx, key)))
        }

        /** The Where to? box fills the rest of its row; with a chosen width it uses the fixed-width layout instead. */
        private fun itemLayoutFor(ctx: Context, key: String): Int =
            if (key == "maps" && Prefs.whereWidth(ctx) > 0) R.layout.item_free_where else ITEM_LAYOUT.getValue(key)

        // Show the real Google Maps icon from this phone; the built-in red pin stays if Maps isn't installed.
        private fun mapsIcon(rv: RemoteViews, ctx: Context) {
            val bmp = appIconBitmap(ctx, "com.google.android.apps.maps") ?: return // keep the default pin
            for (pin in W_PIN) rv.setImageViewBitmap(pin, bmp)
        }

        private fun txt(rv: RemoteViews, ctx: Context, id: Int, text: String, baseSp: Float, bold: Boolean) =
            Look.setText(rv, ctx, id, text, baseSp, bold)

        /** Sets the text of the merged shape (it carries the weather as plain text, so it is redone when the weather changes). */
        private fun mergedFormats(rv: RemoteViews, ctx: Context) {
            val mk = mergedItems(ctx)
            if (mk.isEmpty()) return
            val cf = Prefs.clockFmt(ctx)
            val f12 = if (cf == 2) "HH:mm" else "h:mm"
            val f24 = if (cf == 1) "h:mm" else "HH:mm"
            val dateFmt = Prefs.DATE_FORMATS[Prefs.dateFmt(ctx)].first
            val sep = if (Prefs.mergeHoriz(ctx)) " · " else "\n"
            val p12 = ArrayList<String>()
            val p24 = ArrayList<String>()
            for (k in Prefs.order(ctx)) if (k in mk) when (k) {
                "clock" -> { p12.add(f12); p24.add(f24) }
                "date" -> { p12.add(dateFmt); p24.add(dateFmt) }
                else -> { val wt = lit(Weather.text(ctx)); p12.add(wt); p24.add(wt) }
            }
            // Colours: a colour picked for "merged shape" colours all of it; otherwise every part keeps its own colour
            // (clock, weather, date) and a part with no colour follows the circle text colour.
            val keys = Prefs.order(ctx).filter { it in mk }
            val whole = Prefs.partColor(ctx, "merge")
            fun coloured(parts: List<String>): CharSequence {
                val sb = SpannableStringBuilder(Look.span(ctx, parts.joinToString(sep), true))
                if (whole == 0) {
                    var pos = 0
                    for ((i, part) in parts.withIndex()) {
                        val pc = Prefs.partColor(ctx, keys[i])
                        if (pc != 0 && part.isNotEmpty()) sb.setSpan(ForegroundColorSpan(pc), pos, pos + part.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                        pos += part.length + sep.length
                    }
                }
                return sb
            }
            for (mid in W_MERGE) {
                rv.setCharSequence(mid, "setFormat12Hour", coloured(p12))
                rv.setCharSequence(mid, "setFormat24Hour", coloured(p24))
                if (whole != 0) rv.setTextColor(mid, whole)
            }
        }

        fun setWeather(rv: RemoteViews, ctx: Context) {
            for (w in W_WEATHER) txt(rv, ctx, w, Weather.text(ctx), 13f * isc(ctx, "w_weather"), true)
            mergedFormats(rv, ctx)
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

            // ---- Border around the whole widget (overlay on top of everything; off by default)
            val bw = Prefs.borderWidthOf(ctx, "widget")
            if (Prefs.borderWidget(ctx) && bw > 0) {
                val auto = (Look.contrast(Prefs.widgetBgArgb(ctx)) and 0xFFFFFF) or 0x99000000.toInt()
                val res = when (bw.coerceIn(1, 6)) {
                    1 -> R.drawable.nt_wborder_1
                    2 -> R.drawable.nt_wborder_2
                    3 -> R.drawable.nt_wborder_3
                    4 -> R.drawable.nt_wborder_4
                    5 -> R.drawable.nt_wborder_5
                    else -> R.drawable.nt_wborder_6
                }
                rv.setInt(R.id.wborder, "setBackgroundResource", res)
                rv.setColorStateList(R.id.wborder, "setBackgroundTintList", ColorStateList.valueOf(Look.borderColorOf(ctx, "widget", auto)))
                rv.setViewVisibility(R.id.wborder, View.VISIBLE)
            } else {
                rv.setViewVisibility(R.id.wborder, View.GONE)
            }
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
            val hsc = bs(ctx, "head")
            txt(rv, ctx, R.id.hdr_title, "Inbox", 16f * hsc * isc(ctx, "w_title"), true)
            rv.setTextColor(R.id.hdr_title, Prefs.partColor(ctx, "title").let { if (it != 0) it else main })
            txt(rv, ctx, R.id.wnote, "+ Note", 13f * hsc * isc(ctx, "w_note"), true)
            txt(rv, ctx, R.id.wclear, if (Prefs.clearMode(ctx) == 1) "Read all" else "Clear all", 13f * hsc * isc(ctx, "w_clear"), false)
            val ic = Prefs.iconColor(ctx)
            rv.setTextColor(R.id.wclear, Prefs.partColor(ctx, "clear").let { if (it != 0) it else if (ic != 0) ic else sub })
            rv.setInt(R.id.wset, "setColorFilter", Prefs.partColor(ctx, "gear").let { if (it != 0) it else if (ic != 0) ic else sub })
            val gs = Prefs.gearSize(ctx).toFloat() * hsc * isc(ctx, "w_gear")
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
            val rs = Prefs.refreshSize(ctx).toFloat() * isc(ctx, "w_refresh")
            rv.setViewLayoutWidth(R.id.wrefresh, rs + 12f, TypedValue.COMPLEX_UNIT_DIP)
            rv.setViewLayoutHeight(R.id.wrefresh, rs + 12f, TypedValue.COMPLEX_UNIT_DIP)

            // ---- Clock / weather / date circles and the Where to? box
            val circ = Prefs.circleColor(ctx)
            val circleTint = ColorStateList.valueOf(if (circ != 0) circ else ctx.getColor(R.color.nt_circle))
            val circleText = if (ov != 0) ov else if (circ != 0) Look.contrast(circ) else ctx.getColor(R.color.nt_text)
            val circleSub = if (ov != 0 || circ != 0) Feed.alpha(circleText, 0xB3) else ctx.getColor(R.color.nt_sub)
            val shapeRes = Look.shapeRes(ctx)
            val topSize = Prefs.topSize(ctx).toFloat() * bs(ctx, "top")
            val clockFmt = Prefs.clockFmt(ctx)
            val f12 = if (clockFmt == 2) "HH:mm" else "h:mm"
            val f24 = if (clockFmt == 1) "h:mm" else "HH:mm"
            val dateFmt = Prefs.DATE_FORMATS[Prefs.dateFmt(ctx)].first
            run {
                val slot = 0 // one view per item now (see addItems)
                for ((id, ik) in listOf(W_CLOCK[slot] to "w_clock", W_WEATHER[slot] to "w_weather", W_DATE[slot] to "w_date")) {
                    rv.setColorStateList(id, "setBackgroundTintList", circleTint)
                    rv.setInt(id, "setBackgroundResource", shapeRes)
                    rv.setTextColor(id, circleText)
                    rv.setViewLayoutWidth(id, topSize * isc(ctx, ik) * wst(ctx, ik), TypedValue.COMPLEX_UNIT_DIP)
                    rv.setViewLayoutHeight(id, topSize * isc(ctx, ik) * hst(ctx, ik), TypedValue.COMPLEX_UNIT_DIP)
                }
                for ((k, ids) in listOf("clock" to W_CLOCK, "weather" to W_WEATHER, "date" to W_DATE)) {
                    val pc = Prefs.partColor(ctx, k)
                    if (pc != 0) rv.setTextColor(ids[slot], pc)
                }
                val clock = W_CLOCK[slot]
                rv.setCharSequence(clock, "setFormat12Hour", Look.span(ctx, f12, true))
                rv.setCharSequence(clock, "setFormat24Hour", Look.span(ctx, f24, true))
                rv.setTextViewTextSize(clock, TypedValue.COMPLEX_UNIT_SP, 14f * Look.scale(ctx) * isc(ctx, "w_clock"))
                val date = W_DATE[slot]
                rv.setCharSequence(date, "setFormat12Hour", Look.span(ctx, dateFmt, true))
                rv.setCharSequence(date, "setFormat24Hour", Look.span(ctx, dateFmt, true))
                rv.setTextViewTextSize(date, TypedValue.COMPLEX_UNIT_SP, 12f * Look.scale(ctx) * isc(ctx, "w_date"))
                // The merged shape: the chosen parts in the top-row order, one under the other.
                val mid = W_MERGE[slot]
                rv.setColorStateList(mid, "setBackgroundTintList", circleTint)
                rv.setInt(mid, "setBackgroundResource", shapeRes)
                rv.setTextColor(mid, Prefs.partColor(ctx, "merge").let { if (it != 0) it else circleText })
                rv.setInt(mid, "setMinHeight", Math.round(topSize * isc(ctx, "w_merge") * hst(ctx, "w_merge") * ctx.resources.displayMetrics.density))
                if (Prefs.itemW(ctx, "w_merge") != 100) {
                    val baseW = if (Prefs.mergeHoriz(ctx)) 160f else 70f
                    rv.setViewLayoutWidth(mid, baseW * isc(ctx, "w_merge") * wst(ctx, "w_merge"), TypedValue.COMPLEX_UNIT_DIP)
                }
                rv.setTextViewTextSize(mid, TypedValue.COMPLEX_UNIT_SP, 13f * Look.scale(ctx) * isc(ctx, "w_merge"))
            }
            for (i in W_LOC.indices) {
                if (!Prefs.freeOn(ctx) && Prefs.whereWidth(ctx) > 0)
                    rv.setViewLayoutWidth(W_LOC[i], Prefs.whereWidth(ctx).toFloat(), TypedValue.COMPLEX_UNIT_DIP)
                rv.setColorStateList(W_LOC[i], "setBackgroundTintList", circleTint)
                rv.setViewLayoutHeight(W_LOC[i], 40f * bs(ctx, "top") * isc(ctx, "w_where") * hst(ctx, "w_where"), TypedValue.COMPLEX_UNIT_DIP)
            }
            // While Google Maps is navigating this box becomes a one-tap red "Exit navigation"
            val navigating = LocationAuto.navNotification() != null
            val whereText = if (navigating) "Exit navigation" else "Where to?"
            for (w in W_WW) txt(rv, ctx, w, whereText, 12f * bs(ctx, "top") * isc(ctx, "w_where"), false)
            val wc = Prefs.partColor(ctx, "where")
            val whereColor = if (navigating) 0xFFE53935.toInt() else if (wc != 0) wc else circleSub
            for (w in W_WW) rv.setTextColor(w, whereColor)

            // ---- Fast-dial circles
            val dark = Feed.isDark(ctx)
            val filled = Feed.style("com.whatsapp")
            val empty = Feed.style("")
            val dc = Prefs.dialCircle(ctx)
            val free = Prefs.freeOn(ctx)
            val dsize = Prefs.dialSize(ctx).toFloat() * bs(ctx, "dial")
            val qsize = Prefs.dialSize(ctx).toFloat() * bs(ctx, "quick")
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
                txt(rv, ctx, DIAL_CIRCLE[i], initial, 18f * isc(ctx, "d$i"), true)
                rv.setTextColor(DIAL_CIRCLE[i], fgc)
                rv.setColorStateList(DIAL_CIRCLE[i], "setBackgroundTintList", ColorStateList.valueOf(bgc))
                rv.setInt(DIAL_CIRCLE[i], "setBackgroundResource", shapeRes)
                rv.setViewLayoutWidth(DIAL_CIRCLE[i], dsize * isc(ctx, "d$i") * wst(ctx, "d$i"), TypedValue.COMPLEX_UNIT_DIP)
                rv.setViewLayoutHeight(DIAL_CIRCLE[i], dsize * isc(ctx, "d$i") * hst(ctx, "d$i"), TypedValue.COMPLEX_UNIT_DIP)
                txt(rv, ctx, DIAL_NAME[i], if (has) label else "Add", 10f, false)
                val dn = Prefs.partColor(ctx, "dialnames")
                rv.setTextColor(DIAL_NAME[i], if (ownText != 0) ownText else if (dn != 0) dn else sub)
                rv.setViewVisibility(DIAL_NAME[i], if (showNames) View.VISIBLE else View.GONE)
                if (free) {
                    // Each circle is on its own now: the strip colour (if one is set) becomes a plate behind each circle
                    val pdbg = Prefs.dialBg(ctx)
                    val plate = if (pdbg != 0) ((Prefs.dialBgAlpha(ctx) * 255 / 100) shl 24) or (pdbg and 0xFFFFFF) else 0
                    rv.setInt(DIAL_BOX[i], "setBackgroundResource", R.drawable.nt_dial_bg)
                    rv.setColorStateList(DIAL_BOX[i], "setBackgroundTintList", ColorStateList.valueOf(plate))
                    rv.setViewVisibility(DIAL_BOX[i], if (Prefs.show(ctx, "dial")) View.VISIBLE else View.GONE)
                }
            }
            // ---- Shortcuts row under the fast dial (off until switched on; filled slots + one "Add")
            val qOn = Prefs.quickOn(ctx)
            rv.setViewVisibility(R.id.quickwrap, if (qOn) View.VISIBLE else View.GONE)
            if (free && !qOn) for (b in Q_BOX) rv.setViewVisibility(b, View.GONE)
            if (qOn) {
                val qdbg = Prefs.dialBg(ctx)
                val qTint = if (qdbg != 0) ((Prefs.dialBgAlpha(ctx) * 255 / 100) shl 24) or (qdbg and 0xFFFFFF) else 0
                rv.setColorStateList(R.id.quickwrap, "setBackgroundTintList", ColorStateList.valueOf(qTint))
                var addShown = false
                for (i in 0 until Prefs.QUICK_SLOTS) {
                    val q = Prefs.quick(ctx, i)
                    val qs = qsize * isc(ctx, "q$i")
                    val qw = qs * wst(ctx, "q$i")
                    val qh = qs * hst(ctx, "q$i")
                    val qm = minOf(qw, qh)
                    val has = q != null
                    val show = has || !addShown
                    if (!has) addShown = true
                    rv.setViewVisibility(Q_BOX[i], if (show) View.VISIBLE else if (free) View.GONE else View.INVISIBLE)
                    if (free) {
                        val pdbg = Prefs.dialBg(ctx)
                        val plate = if (pdbg != 0) ((Prefs.dialBgAlpha(ctx) * 255 / 100) shl 24) or (pdbg and 0xFFFFFF) else 0
                        rv.setInt(Q_BOX[i], "setBackgroundResource", R.drawable.nt_dial_bg)
                        rv.setColorStateList(Q_BOX[i], "setBackgroundTintList", ColorStateList.valueOf(plate))
                    }
                    val st = if (has) filled else empty
                    val bgc = if (dc != 0) dc else st.bg(dark)
                    val letterC = Prefs.partColor(ctx, "dialletter")
                    val fgc = if (letterC != 0) letterC else if (dc != 0) Look.contrast(dc) else st.fg(dark)
                    val icon = if (q != null) appIconBitmap(ctx, q.pkg) else null
                    val initial = if (q == null) "+" else if (icon != null) "" else q.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "•"
                    txt(rv, ctx, Q_CIRCLE[i], initial, 18f * isc(ctx, "q$i"), true)
                    rv.setTextColor(Q_CIRCLE[i], fgc)
                    rv.setColorStateList(Q_CIRCLE[i], "setBackgroundTintList", ColorStateList.valueOf(bgc))
                    rv.setInt(Q_CIRCLE[i], "setBackgroundResource", shapeRes)
                    rv.setViewLayoutWidth(Q_CIRCLE[i], qw, TypedValue.COMPLEX_UNIT_DIP)
                    rv.setViewLayoutHeight(Q_CIRCLE[i], qh, TypedValue.COMPLEX_UNIT_DIP)
                    if (icon != null) {
                        rv.setImageViewBitmap(Q_ICON[i], icon)
                        rv.setViewLayoutWidth(Q_ICON[i], qm * 0.62f, TypedValue.COMPLEX_UNIT_DIP)
                        rv.setViewLayoutHeight(Q_ICON[i], qm * 0.62f, TypedValue.COMPLEX_UNIT_DIP)
                        rv.setViewVisibility(Q_ICON[i], View.VISIBLE)
                    } else {
                        rv.setViewVisibility(Q_ICON[i], View.GONE)
                    }
                    txt(rv, ctx, Q_NAME[i], q?.name ?: "Add", 10f, false)
                    val dn = Prefs.partColor(ctx, "dialnames")
                    rv.setTextColor(Q_NAME[i], if (dn != 0) dn else sub)
                    rv.setViewVisibility(Q_NAME[i], if (showNames) View.VISIBLE else View.GONE)
                }
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
            rv.setViewVisibility(R.id.wedit, if (Prefs.editBtn(ctx)) View.VISIBLE else View.GONE)
            rv.setTextViewTextSize(R.id.wedit, TypedValue.COMPLEX_UNIT_SP, 16f * bs(ctx, "head") * isc(ctx, "w_edit"))
            rv.setTextColor(R.id.wedit, Prefs.partColor(ctx, "edit").let { if (it != 0) it else if (Prefs.iconColor(ctx) != 0) Prefs.iconColor(ctx) else sub })
            rv.setOnClickPendingIntent(
                R.id.wedit,
                PendingIntent.getActivity(ctx, 70, Intent(ctx, EditMenuActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            )

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
                set3d(rv, ctx, R.id.wset, R.drawable.ic_gear, gearC, Prefs.gearSize(ctx).toFloat() * bs(ctx, "head") * isc(ctx, "w_gear"))
                set3d(rv, ctx, R.id.wrefresh, R.drawable.ic_refresh, refC, Prefs.refreshSize(ctx).toFloat() * isc(ctx, "w_refresh"))
                set3d(rv, ctx, R.id.wmic, if (rec) R.drawable.ic_stop else R.drawable.ic_mic, micC, Prefs.micSize(ctx).toFloat() * bs(ctx, "head") * isc(ctx, "w_mic"))
            } else {
                rv.setImageViewResource(R.id.wset, R.drawable.ic_gear)
                rv.setImageViewResource(R.id.wrefresh, R.drawable.ic_refresh)
            }
        }

        /** Items merged into one shape: only when 2 or more of the shown items in the top row are chosen. */
        private fun mergedItems(ctx: Context): Set<String> {
            val top = Prefs.rows(ctx)["top"] ?: emptyList()
            val free = Prefs.freeOn(ctx)
            val m = Prefs.merged(ctx).filter { (free || it in top) && Prefs.show(ctx, it) }.toSet()
            return if (m.size >= 2) m else emptySet()
        }

        private fun lit(s: String): String = "'" + s.replace("'", "''") + "'"

        // Which top-row items are shown, and the merged shape (it replaces its items while two or more of them are on).
        private fun applyLayout(rv: RemoteViews, ctx: Context) {
            val mergedKeys = mergedItems(ctx)
            rv.setViewVisibility(R.id.wclock, if (Prefs.show(ctx, "clock") && "clock" !in mergedKeys) View.VISIBLE else View.GONE)
            rv.setViewVisibility(R.id.wweather, if (Prefs.show(ctx, "weather") && "weather" !in mergedKeys) View.VISIBLE else View.GONE)
            rv.setViewVisibility(R.id.wdate, if (Prefs.show(ctx, "date") && "date" !in mergedKeys) View.VISIBLE else View.GONE)
            rv.setViewVisibility(R.id.wmerge, if (mergedKeys.isNotEmpty()) View.VISIBLE else View.GONE)
            rv.setViewVisibility(R.id.wloc, if (Prefs.show(ctx, "maps")) View.VISIBLE else View.GONE)
            rv.setViewVisibility(R.id.dialwrap, if (Prefs.show(ctx, "dial")) View.VISIBLE else View.GONE)
            val accent = Prefs.accent(ctx)
            rv.setTextColor(R.id.wnote, Prefs.partColor(ctx, "note").let { if (it != 0) it else accent })
            val recNow = RecordService.running()
            rv.setImageViewResource(R.id.wmic, if (recNow) R.drawable.ic_stop else R.drawable.ic_mic)
            rv.setInt(R.id.wmic, "setColorFilter", if (recNow) 0xFFE53935.toInt() else Prefs.partColor(ctx, "mic").let { if (it != 0) it else if (Prefs.iconColor(ctx) != 0) Prefs.iconColor(ctx) else accent })
            val ms = Prefs.micSize(ctx).toFloat() * bs(ctx, "head") * isc(ctx, "w_mic")
            rv.setViewLayoutWidth(R.id.wmic, ms + 12f, TypedValue.COMPLEX_UNIT_DIP)
            rv.setViewLayoutHeight(R.id.wmic, ms + 4f, TypedValue.COMPLEX_UNIT_DIP)
            rv.setColorStateList(R.id.count, "setBackgroundTintList", ColorStateList.valueOf(accent))
        }

        private fun setCount(rv: RemoteViews, ctx: Context) {
            val n = Feed.items(ctx).size
            txt(rv, ctx, R.id.count, n.toString(), 12f * isc(ctx, "w_count"), true)
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
