package com.zaid.speedtrail.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.zaid.speedtrail.MainActivity
import com.zaid.speedtrail.R
import com.zaid.speedtrail.data.model.TrackPoint
import com.zaid.speedtrail.data.model.Trip
import com.zaid.speedtrail.data.repository.TripRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Foreground service yang merekam lokasi & kecepatan ke database selama perjalanan.
 * Dikendalikan lewat Intent action: START / PAUSE / RESUME / STOP.
 */
class LocationTrackingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: TripRepository
    private lateinit var fused: FusedLocationProviderClient

    private var tripId: Long = -1L
    private var startTime: Long = 0L
    private var distance = 0.0
    private var maxSpeed = 0f
    private var speedSum = 0.0
    private var pointCount = 0
    private var lastLocation: Location? = null
    private var paused = false

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            if (paused) return
            result.lastLocation?.let { onNewLocation(it) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = TripRepository.get(this)
        fused = LocationServices.getFusedLocationProviderClient(this)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_PAUSE -> pauseTracking()
            ACTION_RESUME -> resumeTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    private fun startTracking() {
        if (tripId != -1L) return // sudah berjalan
        startForeground(NOTIF_ID, buildNotification())

        startTime = System.currentTimeMillis()
        scope.launch {
            tripId = repository.startTrip(startTime)
            TrackingState.update {
                LiveStats(status = TrackingStatus.RECORDING, tripId = tripId)
            }
            requestLocationUpdates()
        }
    }

    private fun requestLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(1000L)
            .setMinUpdateDistanceMeters(0f)
            .build()
        try {
            fused.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        } catch (_: SecurityException) {
            // Izin lokasi belum diberikan — UI bertanggung jawab meminta sebelum START.
            stopTracking()
        }
    }

    private fun onNewLocation(loc: Location) {
        val speed = if (loc.hasSpeed()) loc.speed else 0f
        lastLocation?.let { prev ->
            distance += prev.distanceTo(loc)
        }
        lastLocation = loc
        if (speed > maxSpeed) maxSpeed = speed
        speedSum += speed
        pointCount++

        val point = TrackPoint(
            tripId = tripId,
            latitude = loc.latitude,
            longitude = loc.longitude,
            speedMps = speed,
            timestamp = loc.time.takeIf { it > 0 } ?: System.currentTimeMillis(),
            accuracyM = if (loc.hasAccuracy()) loc.accuracy else 0f,
            altitudeM = if (loc.hasAltitude()) loc.altitude else 0.0,
            bearingDeg = if (loc.hasBearing()) loc.bearing else 0f,
        )
        scope.launch { repository.addPoint(point) }

        val durationSec = (System.currentTimeMillis() - startTime) / 1000
        TrackingState.update {
            it.copy(
                status = TrackingStatus.RECORDING,
                tripId = tripId,
                currentSpeedMps = speed,
                maxSpeedMps = maxSpeed,
                avgSpeedMps = if (pointCount > 0) speedSum / pointCount else 0.0,
                distanceMeters = distance,
                durationSec = durationSec,
                lastLat = loc.latitude,
                lastLon = loc.longitude,
                accuracyM = point.accuracyM,
                pointCount = pointCount,
            )
        }
    }

    private fun pauseTracking() {
        paused = true
        TrackingState.update { it.copy(status = TrackingStatus.PAUSED) }
    }

    private fun resumeTracking() {
        paused = false
        lastLocation = null // hindari lompatan jarak setelah jeda
        TrackingState.update { it.copy(status = TrackingStatus.RECORDING) }
    }

    private fun stopTracking() {
        fused.removeLocationUpdates(locationCallback)
        if (tripId != -1L) {
            val endTime = System.currentTimeMillis()
            val finished = Trip(
                id = tripId,
                startTime = startTime,
                endTime = endTime,
                distanceMeters = distance,
                durationSec = (endTime - startTime) / 1000,
                avgSpeedMps = if (pointCount > 0) speedSum / pointCount else 0.0,
                maxSpeedMps = maxSpeed.toDouble(),
            )
            // runBlocking singkat: pastikan tersimpan sebelum service mati.
            runBlocking { repository.finishTrip(finished) }
        }
        tripId = -1L
        TrackingState.reset()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.tracking_notification_title))
            .setContentText(getString(R.string.tracking_notification_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .build()
    }

    private fun startForeground(id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(id, notification)
        }
    }

    private fun createChannel() {
        val mgr = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.tracking_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        mgr.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.zaid.speedtrail.START"
        const val ACTION_PAUSE = "com.zaid.speedtrail.PAUSE"
        const val ACTION_RESUME = "com.zaid.speedtrail.RESUME"
        const val ACTION_STOP = "com.zaid.speedtrail.STOP"

        private const val CHANNEL_ID = "tracking_channel"
        private const val NOTIF_ID = 42

        fun send(context: Context, action: String) {
            val intent = Intent(context, LocationTrackingService::class.java).setAction(action)
            context.startForegroundService(intent)
        }
    }
}
