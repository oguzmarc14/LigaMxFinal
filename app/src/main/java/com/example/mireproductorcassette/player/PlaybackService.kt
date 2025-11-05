package com.example.mireproductorcassette.player

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import com.example.mireproductorcassette.R
import com.example.mireproductorcassette.ui.MainActivity

class PlaybackService : Service() {

    companion object {
        const val ACTION_PLAY_INDEX = "com.example.mireproductorcassette.PLAY_INDEX"
        const val ACTION_PLAY = "com.example.mireproductorcassette.PLAY"
        const val ACTION_PAUSE = "com.example.mireproductorcassette.PAUSE"
        const val ACTION_NEXT = "com.example.mireproductorcassette.NEXT"
        const val ACTION_PREV = "com.example.mireproductorcassette.PREV"
        const val ACTION_STOP = "com.example.mireproductorcassette.STOP"
        const val ACTION_PING = "com.example.mireproductorcassette.PING"
        const val ACTION_STATE = "com.example.mireproductorcassette.STATE"

        const val EXTRA_INDEX = "extra_index"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ARTIST = "extra_artist"
        const val EXTRA_IS_PLAYING = "extra_is_playing"
        const val EXTRA_DURATION = "extra_duration"
        const val EXTRA_POSITION = "extra_position"

        private const val CHANNEL_ID = "playback_channel"
        private const val NOTIF_ID = 1003
    }

    private lateinit var player: ExoPlayer
    private var index = -1

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()

        val attrs = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
        player.setAudioAttributes(attrs, true)

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) { updateUi() }
            override fun onIsPlayingChanged(isPlaying: Boolean) { updateUi() }
            override fun onPlayerError(error: PlaybackException) { updateUi() }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { updateUi() }
        })

        createChannel()
        startForeground(NOTIF_ID, buildNotification("Sin reproducción", false))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_INDEX -> {
                val i = intent.getIntExtra(EXTRA_INDEX, -1)
                if (i in PlaylistRepo.songs.indices) { index = i; playCurrent() }
            }
            ACTION_PLAY -> { if (!player.isPlaying) player.play(); updateUi() }
            ACTION_PAUSE -> { if (player.isPlaying) player.pause(); updateUi() }
            ACTION_NEXT -> {
                val list: List<Song> = PlaylistRepo.songs
                if (list.isNotEmpty()) { index = if (index < 0) 0 else (index + 1) % list.size; playCurrent() }
            }
            ACTION_PREV -> {
                val list: List<Song> = PlaylistRepo.songs
                if (list.isNotEmpty()) { index = if (index < 0) 0 else (index - 1 + list.size) % list.size; playCurrent() }
            }
            ACTION_STOP -> { player.stop(); updateUi() }
            ACTION_PING -> updateUi()
        }
        return START_STICKY
    }

    private fun playCurrent() {
        val list: List<Song> = PlaylistRepo.songs
        val s: Song = list[index]
        player.stop()
        player.clearMediaItems()
        player.setMediaItem(MediaItem.fromUri(s.uri))
        player.prepare()
        player.play()
        updateUi()
    }

    private fun updateUi() {
        val current: Song? = PlaylistRepo.songs.getOrNull(index)
        val title = current?.title ?: "Sin reproducción"
        val artist = current?.artist ?: ""
        val isPlaying = player.isPlaying
        val duration = player.duration.takeIf { it > 0 } ?: 0L
        val position = player.currentPosition.takeIf { it > 0 } ?: 0L

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(title, isPlaying))

        sendBroadcast(Intent(ACTION_STATE).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_ARTIST, artist)
            putExtra(EXTRA_IS_PLAYING, isPlaying)
            putExtra(EXTRA_DURATION, duration)
            putExtra(EXTRA_POSITION, position)
        })

        com.example.mireproductorcassette.widget.MusicWidget.updateAll(this, title, isPlaying)
    }

    private fun createChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ch = NotificationChannel(CHANNEL_ID, "Reproducción", NotificationManager.IMPORTANCE_LOW)
        nm.createNotificationChannel(ch)
    }

    private fun buildNotification(title: String, playing: Boolean): Notification {
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        fun act(a: String, req: Int) = PendingIntent.getService(
            this, req, Intent(this, PlaybackService::class.java).setAction(a), PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(if (playing) "Reproduciendo" else "Pausado")
            .setContentIntent(openApp)
            .addAction(0, "Prev", act(ACTION_PREV, 10))
            .addAction(0, if (playing) "Pausar" else "Reproducir", act(if (playing) ACTION_PAUSE else ACTION_PLAY, 11))
            .addAction(0, "Next", act(ACTION_NEXT, 12))
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }
}
