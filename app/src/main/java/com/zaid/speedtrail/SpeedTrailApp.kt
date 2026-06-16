package com.zaid.speedtrail

import android.app.Application
import org.osmdroid.config.Configuration

class SpeedTrailApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // osmdroid wajib punya userAgent unik agar tidak diblokir server tile OSM.
        Configuration.getInstance().userAgentValue = packageName
    }
}
