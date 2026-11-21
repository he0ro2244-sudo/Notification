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
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) appWidgetManager.updateAppWidget(id, build(context, id))
    }

    companion object {
        const val ACTION_CLEAR = "com.example.notiftiles.CLEAR_ALL"

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
            setCount(rv, ctx)
            return rv
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
            setCount(rv, ctx)
            for (id in ids) mgr.partiallyUpdateAppWidget(id, rv)
            mgr.notifyAppWidgetViewDataChanged(ids, R.id.grid)
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
