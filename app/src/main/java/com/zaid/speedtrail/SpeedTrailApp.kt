package com.zaid.speedtrail

import android.app.Application
import org.osmdroid.config.Configuration

class SpeedTrailApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Muat konfigurasi osmdroid (menyiapkan path cache tile) lalu set userAgent unik
        // agar request tile tidak ditolak server OSM. Keduanya wajib sebelum MapView dibuat.
        val prefs = getSharedPreferences("osmdroid", MODE_PRIVATE)
        Configuration.getInstance().load(this, prefs)
        Configuration.getInstance().userAgentValue = packageName
    }
}
