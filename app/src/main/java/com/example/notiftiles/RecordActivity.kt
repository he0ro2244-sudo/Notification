package com.example.notiftiles

import android.app.Activity
import android.os.Bundle
import android.widget.Toast

// Invisible activity behind the widget's mic: start the phone's Recorder recording, then get out of the way.
class RecordActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) return
        val go = {
            if (!Recorder.startRecording(this)) {
                Toast.makeText(this, "No recorder app found", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
        if (Recorder.needsPick(this)) Recorder.pickDialog(this, { go() }, { finish() }) else go()
    }
}
