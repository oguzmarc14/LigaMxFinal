package com.example.mireproductorcassette.ui

import android.content.*
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mireproductorcassette.databinding.ActivityMainBinding
import com.example.mireproductorcassette.player.PlaybackService
import com.example.mireproductorcassette.player.PlaylistRepo
import com.example.mireproductorcassette.player.Song
import com.example.mireproductorcassette.widget.MusicWidget
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: SongAdapter
    private var isPlaying = false

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == PlaybackService.ACTION_STATE) {
                isPlaying = intent.getBooleanExtra(PlaybackService.EXTRA_IS_PLAYING, false)
                val title = intent.getStringExtra(PlaybackService.EXTRA_TITLE) ?: "Sin reproducción"
                val artist = intent.getStringExtra(PlaybackService.EXTRA_ARTIST) ?: ""
                val duration = intent.getLongExtra(PlaybackService.EXTRA_DURATION, 0L)
                val position = intent.getLongExtra(PlaybackService.EXTRA_POSITION, 0L)
                binding.nowTitle.text = title
                binding.nowArtist.text = artist
                binding.nowDuration.text = fmt(position) + " / " + fmt(duration)
                binding.btnPlayPause.setImageResource(
                    if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
                )
            }
        }
    }

    private fun fmt(ms: Long): String {
        if (ms <= 0) return "00:00"
        val m = TimeUnit.MILLISECONDS.toMinutes(ms)
        val s = TimeUnit.MILLISECONDS.toSeconds(ms) - m * 60
        return String.format("%02d:%02d", m, s)
    }

    private val pickAudio = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
        val title = queryDisplayName(uri) ?: "Audio"
        val song = Song(title = title, artist = "", uri = uri.toString())
        val pos = PlaylistRepo.songs.size
        PlaylistRepo.songs.add(song)
        adapter.notifyItemInserted(pos)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = SongAdapter(PlaylistRepo.songs) { index, _ ->
            val i = Intent(this, PlaybackService::class.java).apply {
                action = PlaybackService.ACTION_PLAY_INDEX
                putExtra(PlaybackService.EXTRA_INDEX, index)
            }
            startForegroundService(i)
        }
        binding.recyclerSongs.layoutManager = LinearLayoutManager(this)
        binding.recyclerSongs.adapter = adapter

        binding.btnPlayPause.setOnClickListener {
            val act = if (isPlaying) PlaybackService.ACTION_PAUSE else PlaybackService.ACTION_PLAY
            isPlaying = !isPlaying
            binding.btnPlayPause.setImageResource(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            )
            startService(Intent(this, PlaybackService::class.java).setAction(act))
        }
        binding.btnNext.setOnClickListener {
            startService(Intent(this, PlaybackService::class.java).setAction(PlaybackService.ACTION_NEXT))
        }
        binding.btnPrev.setOnClickListener {
            startService(Intent(this, PlaybackService::class.java).setAction(PlaybackService.ACTION_PREV))
        }

        binding.fabAdd.setOnClickListener { pickAudio.launch(arrayOf("audio/*")) }
        binding.fabAdd.setOnLongClickListener {
            requestPinWidget()
            true
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(PlaybackService.ACTION_STATE)
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            registerReceiver(stateReceiver, filter, RECEIVER_EXPORTED)
        } else {
            @Suppress("DEPRECATION") registerReceiver(stateReceiver, filter)
        }
        startService(Intent(this, PlaybackService::class.java).setAction(PlaybackService.ACTION_PING))
    }

    override fun onStop() {
        try { unregisterReceiver(stateReceiver) } catch (_: Exception) { }
        super.onStop()
    }

    private fun requestPinWidget() {
        val awm = AppWidgetManager.getInstance(this)
        if (awm.isRequestPinAppWidgetSupported) {
            val provider = ComponentName(this, MusicWidget::class.java)
            val success = awm.requestPinAppWidget(provider, null, null)
            if (!success) {
                Toast.makeText(this, "No se pudo solicitar el widget al launcher.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, getString(com.example.mireproductorcassette.R.string.add_widget_help), Toast.LENGTH_LONG).show()
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        var name: String? = null
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(uri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                name = cursor.getString(0)
            }
        } finally { cursor?.close() }
        return name?.substringBeforeLast('.')
    }
}
