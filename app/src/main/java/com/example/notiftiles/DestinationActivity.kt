package com.example.notiftiles

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority

// Behind the "Where to?" pill: makes sure Location is on, then goes straight to Google Maps.
class DestinationActivity : Activity() {

    private fun locationOn(): Boolean =
        (getSystemService(Context.LOCATION_SERVICE) as LocationManager).isLocationEnabled

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) return
        if (locationOn()) {
            openMaps()
            return
        }
        // Location is off: show Google's one-tap "turn on location" prompt.
        try {
            val req = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10000L).build()
            val settings = LocationSettingsRequest.Builder().addLocationRequest(req).setAlwaysShow(true).build()
            LocationServices.getSettingsClient(this).checkLocationSettings(settings)
                .addOnSuccessListener { openMaps() }
                .addOnFailureListener { e ->
                    if (e is ResolvableApiException) {
                        try {
                            e.startResolutionForResult(this, REQ_LOCATION)
                        } catch (x: IntentSender.SendIntentException) {
                            fallback()
                        }
                    } else {
                        fallback()
                    }
                }
        } catch (e: Exception) {
            fallback()
        }
    }

    // No Google Play services prompt available: open the Location switch, then Maps when you come back.
    private fun fallback() {
        try {
            startActivityForResult(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), REQ_LOCATION)
        } catch (e: Exception) {
            openMaps()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_LOCATION) openMaps()
    }

    private fun openMaps() {
        try {
            val maps = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="))
                .setPackage("com.google.android.apps.maps")
            startActivity(maps)
        } catch (e: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps")))
            } catch (x: Exception) {
                // nothing to open
            }
        }
        finish()
    }

    companion object {
        private const val REQ_LOCATION = 3
    }
}
