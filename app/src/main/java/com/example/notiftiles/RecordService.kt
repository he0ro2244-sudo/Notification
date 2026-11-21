package com.example.notiftiles

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.media.MediaRecorder
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.MediaStore
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Records audio the moment you tap the mic. Saved in Recordings/NotifTiles (m4a).
class RecordService : Service() {

    private var rec: MediaRecorder? = null
    private var uri: Uri? = null
    private var pfd: ParcelFileDescriptor? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            finishRecording()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        if (rec != null) return START_NOT_STICKY
        startForeground(NOTE_ID, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        if (!begin()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun notification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Voice recording", NotificationManager.IMPORTANCE_LOW))
        val stop = PendingIntent.getService(
            this, 1, Intent(this, RecordService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val open = PendingIntent.getActivity(
            this, 2, Intent(this, PanelActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Recording…")
            .setContentText("Tap Stop to save")
            .setOngoing(true)
            .setUsesChronometer(true)
            .setContentIntent(open)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, android.R.drawable.ic_media_pause), "Stop & save", stop
                ).build()
            )
            .build()
    }

    private fun begin(): Boolean {
        return try {
            val name = "Rec " + SimpleDateFormat("yyyy-MM-dd HH.mm.ss", Locale.US).format(Date())
            val cv = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, "$name.m4a")
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                put(MediaStore.Audio.Media.RELATIVE_PATH, "Recordings/NotifTiles")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val u = contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cv) ?: return fail("Could not create the file")
            uri = u
            val p = contentResolver.openFileDescriptor(u, "w") ?: return fail("Could not open the file")
            pfd = p
            val r = MediaRecorder(this)
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(128000)
            r.setAudioSamplingRate(44100)
            r.setOutputFile(p.fileDescriptor)
            r.prepare()
            r.start()
            rec = r
            startedAt = SystemClock.elapsedRealtime()
            true
        } catch (e: Exception) {
            fail("Could not start recording")
        }
    }

    private fun fail(msg: String): Boolean {
        toast(msg)
        cleanup(delete = true)
        return false
    }

    private fun finishRecording() {
        val r = rec ?: return
        var ok = true
        try {
            r.stop()
        } catch (e: RuntimeException) {
            ok = false // stopped too quickly: nothing recorded
        }
        cleanup(delete = !ok)
        toast(if (ok) "Recording saved (Recordings/NotifTiles)" else "Recording was too short")
    }

    private fun cleanup(delete: Boolean) {
        try { rec?.release() } catch (e: Exception) { }
        rec = null
        try { pfd?.close() } catch (e: Exception) { }
        pfd = null
        val u = uri
        uri = null
        startedAt = 0L
        if (u != null) {
            try {
                if (delete) {
                    contentResolver.delete(u, null, null)
                } else {
                    val cv = ContentValues()
                    cv.put(MediaStore.Audio.Media.IS_PENDING, 0)
                    contentResolver.update(u, cv, null, null)
                }
            } catch (e: Exception) { }
        }
    }

    private fun toast(m: String) {
        Handler(Looper.getMainLooper()).post { Toast.makeText(applicationContext, m, Toast.LENGTH_LONG).show() }
    }

    override fun onDestroy() {
        finishRecording()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "nt_recording"
        private const val NOTE_ID = 4242
        const val ACTION_STOP = "com.example.notiftiles.STOP_RECORDING"

        @Volatile
        var startedAt = 0L

        fun running(): Boolean = startedAt > 0L

        fun start(c: Context) {
            c.startForegroundService(Intent(c, RecordService::class.java))
        }

        fun stop(c: Context) {
            c.startService(Intent(c, RecordService::class.java).setAction(ACTION_STOP))
        }
    }
}
