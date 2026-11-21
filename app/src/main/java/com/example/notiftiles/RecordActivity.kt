package com.example.notiftiles

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast

// Invisible activity behind the widget's mic.
// Helper ON: start the phone's Recorder recording. Helper OFF (and the built-in switch on): record inside this app;
// tap the mic again (or "Stop & save" in the notification) to save.
class RecordActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) return
        if (Recorder.useBuiltin(this)) {
            builtin()
            return
        }
        val go = {
            if (!Recorder.startRecording(this)) {
                Toast.makeText(this, "No recorder app found", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
        if (Recorder.needsPick(this)) Recorder.pickDialog(this, { go() }, { finish() }) else go()
    }

    private fun builtin() {
        if (RecordService.running()) {
            RecordService.stop(this)
            finish()
            return
        }
        val need = ArrayList<String>()
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            need.add(Manifest.permission.RECORD_AUDIO)
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            need.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (need.isNotEmpty()) {
            requestPermissions(need.toTypedArray(), 77)
            return
        }
        RecordService.start(this)
        finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 77) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                RecordService.start(this)
            } else {
                Toast.makeText(this, "Allow the microphone to record inside this app", Toast.LENGTH_LONG).show()
            }
            finish()
        }
    }
}
