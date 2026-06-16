package com.zaid.speedtrail.ui.components

import android.graphics.Color as AndroidColor
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.zaid.speedtrail.data.model.TrackPoint
import com.zaid.speedtrail.ui.detail.ColorMode
import com.zaid.speedtrail.util.SlowdownZone
import com.zaid.speedtrail.util.SpeedColorMapper
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline

/**
 * Menampilkan jalur trip di peta OSM. Tiap segmen (2 titik berurutan) digambar
 * sebagai Polyline tersendiri agar warnanya bisa berbeda sesuai kecepatan.
 *
 * Untuk trip dengan ribuan titik, segmen di-downsample agar peta tetap lancar.
 *
 * @param recenterKey Naikkan nilainya untuk memfokuskan peta kembali ke jalur.
 *                    Selama nilai ini tetap, geser/zoom manual pengguna dipertahankan.
 */
@Composable
fun RouteMapView(
    points: List<TrackPoint>,
    colorMode: ColorMode,
    avgSpeedMps: Double,
    slowdowns: List<SlowdownZone>,
    modifier: Modifier = Modifier,
    recenterKey: Int = 0,
    maxSegments: Int = 1500,
) {
    // recenterKey terakhir yang sudah diterapkan; mencegah auto-fit mereset pan/zoom manual.
    val appliedKey = remember { intArrayOf(Int.MIN_VALUE) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                setUseDataConnection(true)
                // MAPNIK hanya punya tile sampai zoom 19; lebih dari itu peta jadi grid kosong.
                maxZoomLevel = 19.0
                minZoomLevel = 3.0
                // Peta ada di dalam LazyColumn: cegah parent mencuri gesture agar bisa geser 1 jari.
                setOnTouchListener { v, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE ->
                            v.parent?.requestDisallowInterceptTouchEvent(true)
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                            v.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                    false // biarkan MapView tetap memproses gesture
                }
            }
        },
        update = { map ->
            map.overlays.clear()
            if (points.size < 2) {
                map.invalidate()
                return@AndroidView
            }

            val step = (points.size / maxSegments).coerceAtLeast(1)

            var i = 0
            while (i + step < points.size) {
                val a = points[i]
                val b = points[i + step]
                val color = when (colorMode) {
                    ColorMode.ABSOLUTE -> SpeedColorMapper.absoluteArgb(a.speedMps)
                    ColorMode.RELATIVE -> SpeedColorMapper.relativeArgb(a.speedMps, avgSpeedMps)
                }
                val segment = Polyline(map).apply {
                    outlinePaint.color = color
                    outlinePaint.strokeWidth = 12f
                    setPoints(
                        listOf(
                            GeoPoint(a.latitude, a.longitude),
                            GeoPoint(b.latitude, b.longitude),
                        )
                    )
                }
                map.overlays.add(segment)
                i += step
            }

            // Tandai zona perlambatan dengan lingkaran merah transparan.
            slowdowns.forEach { zone ->
                val marker = Polyline(map).apply {
                    outlinePaint.color = AndroidColor.argb(180, 244, 67, 54)
                    outlinePaint.strokeWidth = 22f
                    setPoints(
                        listOf(
                            GeoPoint(zone.centerLat, zone.centerLon),
                            GeoPoint(zone.centerLat + 0.00005, zone.centerLon + 0.00005),
                        )
                    )
                }
                map.overlays.add(marker)
            }

            // Fokuskan ke seluruh jalur hanya saat pertama kali atau saat tombol fokus ditekan,
            // supaya geser/zoom manual pengguna tidak ikut ter-reset tiap recompose.
            if (appliedKey[0] != recenterKey) {
                appliedKey[0] = recenterKey
                val lats = points.map { it.latitude }
                val lons = points.map { it.longitude }
                val box = BoundingBox(lats.max(), lons.max(), lats.min(), lons.min())
                map.post {
                    map.zoomToBoundingBox(box.increaseByScale(1.3f), false, 64)
                    // Trip pendek bisa membuat zoom melebihi tile yang tersedia → batasi.
                    if (map.zoomLevelDouble > 18.0) map.controller.setZoom(18.0)
                }
            }
            map.invalidate()
        },
    )

    DisposableEffect(Unit) {
        onDispose { /* MapView dilepas otomatis oleh AndroidView */ }
    }
}
