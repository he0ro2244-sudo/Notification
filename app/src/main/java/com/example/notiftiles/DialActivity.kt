package com.example.notiftiles

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Toast

// Invisible activity behind the widget's fast-dial circles.
// Empty slot (or extra "pick") -> choose a contact. Filled slot -> call it.
class DialActivity : Activity() {

    private var slot = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        slot = intent.getIntExtra("slot", 0).coerceIn(0, Prefs.DIAL_SLOTS - 1)
        if (savedInstanceState != null) return
        val number = Prefs.dialNumber(this, slot)
        if (number.isEmpty() || intent.getBooleanExtra("pick", false)) {
            pick()
        } else {
            call(number)
        }
    }

    private fun pick() {
        try {
            startActivityForResult(
                Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI),
                REQ_PICK
            )
        } catch (e: Exception) {
            Toast.makeText(this, "No contacts app found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun call(number: String) {
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            go(Intent.ACTION_CALL, number)
        } else {
            requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), REQ_PERM)
        }
    }

    private fun go(action: String, number: String) {
        try {
            startActivity(Intent(action, Uri.fromParts("tel", number, null)))
        } catch (e: Exception) {
            Toast.makeText(this, "Could not start the call", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, perms, results)
        val number = Prefs.dialNumber(this, slot)
        if (code != REQ_PERM || number.isEmpty()) {
            finish()
            return
        }
        val ok = results.isNotEmpty() && results[0] == PackageManager.PERMISSION_GRANTED
        // If the permission is refused, still open the dialer with the number filled in.
        go(if (ok) Intent.ACTION_CALL else Intent.ACTION_DIAL, number)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_PICK && resultCode == RESULT_OK && data?.data != null) {
            try {
                contentResolver.query(
                    data.data!!,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    ),
                    null, null, null
                )?.use { c ->
                    if (c.moveToFirst()) {
                        val name = c.getString(0) ?: ""
                        val num = (c.getString(1) ?: "").filter { it.isDigit() || it == '+' || it == '#' || it == '*' }
                        if (num.isNotEmpty()) {
                            Prefs.setDial(this, slot, name, num)
                            TilesWidget.refresh(applicationContext)
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Could not read that contact", Toast.LENGTH_SHORT).show()
            }
        }
        finish()
    }

    companion object {
        private const val REQ_PICK = 1
        private const val REQ_PERM = 2
    }
}
