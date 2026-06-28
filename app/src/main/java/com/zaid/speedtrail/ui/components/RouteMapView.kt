package com.zaid.speedtrail.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
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
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.advancedpolyline.ColorMapping
import org.osmdroid.views.overlay.advancedpolyline.PolychromaticPaintList

/**
 * Basemap minimalis CARTO Positron tanpa label — bersih, jadi rute berwarna yang menonjol.
 * Tidak butuh API key. Wajib menampilkan atribusi "© OpenStreetMap, © CARTO" (lihat DetailScreen).
 */
private val CartoPositronNoLabels = XYTileSource(
    "CartoPositronNoLabels", 0, 20, 256, ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/light_nolabels/",
        "https://b.basemaps.cartocdn.com/light_nolabels/",
        "https://c.basemaps.cartocdn.com/light_nolabels/",
        "https://d.basemaps.cartocdn.com/light_nolabels/",
    ),
    "© OpenStreetMap contributors, © CARTO",
)

/**
 * Menampilkan jalur trip di peta. Seluruh rute digambar sebagai SATU Polyline dengan
 * warna per-segmen (PolychromaticPaintList) sesuai kecepatan — jauh lebih ringan/mulus
 * dibanding ratusan overlay terpisah.
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
    selectedPointIndex: Int? = null,
    maxSegments: Int = 2000,
) {
    // recenterKey terakhir yang sudah diterapkan; mencegah auto-fit mereset pan/zoom manual.
    val appliedKey = remember { intArrayOf(Int.MIN_VALUE) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(CartoPositronNoLabels)
                setMultiTouchControls(true)
                setUseDataConnection(true)
                isTilesScaledToDpi = true
                maxZoomLevel = 19.0
                minZoomLevel = 3.0
                // Peta ada di dalam BottomSheetScaffold: cegah parent mencuri gesture (geser 1 jari).
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

            // Downsample agar jumlah titik wajar untuk trip panjang.
            val step = (points.size / maxSegments).coerceAtLeast(1)
            val sampled = ArrayList<TrackPoint>((points.size / step) + 2)
            var i = 0
            while (i < points.size) {
                sampled.add(points[i])
                i += step
            }
            if (sampled.last() !== points.last()) sampled.add(points.last())

            val colors = IntArray(sampled.size) { idx ->
                val sp = sampled[idx].speedMps
                when (colorMode) {
                    ColorMode.ABSOLUTE -> SpeedColorMapper.absoluteArgb(sp)
                    ColorMode.RELATIVE -> SpeedColorMapper.relativeArgb(sp, avgSpeedMps)
                }
            }

            val basePaint = Paint().apply {
                style = Paint.Style.STROKE
                strokeWidth = 16f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                isAntiAlias = true
            }
            val route = Polyline(map).apply {
                setPoints(sampled.map { GeoPoint(it.latitude, it.longitude) })
                outlinePaintLists.add(
                    PolychromaticPaintList(
                        basePaint,
                        ColorMapping { idx -> colors[idx.coerceIn(0, colors.lastIndex)] },
                        false,
                    )
                )
            }
            map.overlays.add(route)

            // Tandai zona perlambatan dengan titik merah transparan (jumlahnya sedikit).
            slowdowns.forEach { zone ->
                val marker = Polyline(map).apply {
                    outlinePaint.color = AndroidColor.argb(180, 244, 67, 54)
                    outlinePaint.strokeWidth = 26f
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                    setPoints(
                        listOf(
                            GeoPoint(zone.centerLat, zone.centerLon),
                            GeoPoint(zone.centerLat + 0.00004, zone.centerLon + 0.00004),
                        )
                    )
                }
                map.overlays.add(marker)
            }

            // Marker posisi terpilih dari slider timeline
            if (selectedPointIndex != null && selectedPointIndex in points.indices) {
                val pt = points[selectedPointIndex]
                val sz = 40
                val bmp = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888)
                val bc = Canvas(bmp)
                bc.drawCircle(sz / 2f, sz / 2f, sz / 2f - 3f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.WHITE
                    style = Paint.Style.FILL
                })
                bc.drawCircle(sz / 2f, sz / 2f, sz / 2f - 3f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.rgb(21, 101, 192)
                    style = Paint.Style.STROKE
                    strokeWidth = 5f
                })
                val selectedMarker = Marker(map).apply {
                    position = GeoPoint(pt.latitude, pt.longitude)
                    icon = BitmapDrawable(map.context.resources, bmp)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                }
                map.overlays.add(selectedMarker)
            }

            // Fokuskan ke jalur hanya saat pertama kali atau saat tombol fokus ditekan,
            // supaya geser/zoom manual pengguna tidak ikut ter-reset tiap recompose.
            if (appliedKey[0] != recenterKey) {
                appliedKey[0] = recenterKey
                val lats = points.map { it.latitude }
                val lons = points.map { it.longitude }
                val box = BoundingBox(lats.max(), lons.max(), lats.min(), lons.min())
                map.post {
                    map.zoomToBoundingBox(box.increaseByScale(1.3f), false, 80)
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
