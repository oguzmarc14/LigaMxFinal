package com.example.mireproductorcassette.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.mireproductorcassette.R
import com.example.mireproductorcassette.player.PlaybackService

class MusicWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(id, buildViews(context, "Sin reproducción", false))
        }
    }

    companion object {
        fun updateAll(context: Context, title: String, playing: Boolean) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, MusicWidget::class.java))
            for (id in ids) {
                manager.updateAppWidget(id, buildViews(context, title, playing))
            }
        }

        private fun buildViews(context: Context, title: String, playing: Boolean): RemoteViews {
            val v = RemoteViews(context.packageName, R.layout.widget_music)
            v.setTextViewText(R.id.wTitle, title)
            v.setImageViewResource(R.id.wPlayPause, if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)

            val prev = PendingIntent.getService(context, 3,
                Intent(context, PlaybackService::class.java).setAction(PlaybackService.ACTION_PREV),
                PendingIntent.FLAG_IMMUTABLE)

            val playPause = PendingIntent.getService(context, 1,
                Intent(context, PlaybackService::class.java).setAction(
                    if (playing) PlaybackService.ACTION_PAUSE else PlaybackService.ACTION_PLAY
                ),
                PendingIntent.FLAG_IMMUTABLE)

            val next = PendingIntent.getService(context, 4,
                Intent(context, PlaybackService::class.java).setAction(PlaybackService.ACTION_NEXT),
                PendingIntent.FLAG_IMMUTABLE)

            v.setOnClickPendingIntent(R.id.wPrev, prev)
            v.setOnClickPendingIntent(R.id.wPlayPause, playPause)
            v.setOnClickPendingIntent(R.id.wNext, next)
            return v
        }
    }
}
