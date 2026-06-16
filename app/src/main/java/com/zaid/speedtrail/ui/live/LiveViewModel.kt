package com.zaid.speedtrail.ui.live

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.zaid.speedtrail.service.LocationTrackingService
import com.zaid.speedtrail.service.TrackingState

class LiveViewModel(app: Application) : AndroidViewModel(app) {

    val stats = TrackingState.stats

    fun start() = LocationTrackingService.send(getApplication(), LocationTrackingService.ACTION_START)
    fun pause() = LocationTrackingService.send(getApplication(), LocationTrackingService.ACTION_PAUSE)
    fun resume() = LocationTrackingService.send(getApplication(), LocationTrackingService.ACTION_RESUME)
    fun stop() = LocationTrackingService.send(getApplication(), LocationTrackingService.ACTION_STOP)
}
