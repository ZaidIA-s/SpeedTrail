package com.zaid.speedtrail.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaid.speedtrail.ui.components.RouteMapView
import com.zaid.speedtrail.ui.components.SpeedChart
import com.zaid.speedtrail.util.Formatters
import com.zaid.speedtrail.util.SpeedColorMapper
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    tripId: Long,
    onBack: () -> Unit,
    viewModel: DetailViewModel = viewModel(),
) {
    LaunchedEffect(tripId) { viewModel.load(tripId) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val trip = state.trip
    val avg = trip?.avgSpeedMps ?: 0.0
    var recenter by remember { mutableIntStateOf(0) }
    var selectedPointIndex by remember { mutableIntStateOf(0) }
    val scaffoldState = rememberBottomSheetScaffoldState()

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 150.dp,
        sheetContent = {
            InfoPanel(state = state, avg = avg, onToggleColorMode = viewModel::toggleColorMode)
        },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            RouteMapView(
                points = state.points,
                colorMode = state.colorMode,
                avgSpeedMps = avg,
                slowdowns = state.slowdowns,
                recenterKey = recenter,
                selectedPointIndex = selectedPointIndex.coerceIn(0, (state.points.size - 1).coerceAtLeast(0)),
                modifier = Modifier.matchParentSize(),
            )

            // Bar atas melayang di atas peta.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                FilledTonalIconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                }
                FilledTonalIconButton(onClick = { viewModel.toggleColorMode() }) {
                    Icon(Icons.Default.Palette, contentDescription = "Mode warna")
                }
            }

            // Atribusi wajib untuk tile CARTO/OSM.
            Text(
                text = "© OpenStreetMap, © CARTO",
                style = MaterialTheme.typography.labelSmall,
                color = Color.DarkGray,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, bottom = 158.dp)
                    .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )

            // Tombol fokus ulang, diangkat agar tidak tertutup sheet yang mengintip.
            FilledTonalIconButton(
                onClick = { recenter++ },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 166.dp),
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Fokuskan ke jalur")
            }

            // Overlay slider timeline + status berkendara
            if (state.points.size >= 2) {
                val safeIdx = selectedPointIndex.coerceIn(0, state.points.lastIndex)
                val pt = state.points[safeIdx]
                val elapsed = (pt.timestamp - (state.trip?.startTime ?: pt.timestamp)) / 1000L
                val speedColor = when (state.colorMode) {
                    ColorMode.ABSOLUTE -> SpeedColorMapper.absolute(pt.speedMps)
                    ColorMode.RELATIVE -> SpeedColorMapper.relative(pt.speedMps, avg)
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 158.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Status berkendara
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.93f),
                                RoundedCornerShape(12.dp),
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        TimelineStatItem(
                            label = "Kecepatan",
                            value = Formatters.speedKmh(pt.speedMps),
                            valueColor = speedColor,
                        )
                        TimelineStatItem(
                            label = "Berlalu",
                            value = Formatters.duration(elapsed),
                        )
                        TimelineStatItem(
                            label = "Titik",
                            value = "${safeIdx + 1}/${state.points.size}",
                        )
                    }
                    // Slider timeline
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.93f),
                                RoundedCornerShape(12.dp),
                            )
                            .padding(horizontal = 4.dp),
                    ) {
                        Slider(
                            value = safeIdx.toFloat(),
                            onValueChange = {
                                selectedPointIndex = it.roundToInt().coerceIn(0, state.points.lastIndex)
                            },
                            valueRange = 0f..state.points.lastIndex.toFloat(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoPanel(
    state: DetailState,
    avg: Double,
    onToggleColorMode: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Ringkasan — terlihat saat sheet mengintip (collapsed).
        state.trip?.let {
            Text(
                "${Formatters.distance(it.distanceMeters)} · ${Formatters.duration(it.durationSec)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                Formatters.dateTime(it.startTime),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.colorMode == ColorMode.RELATIVE,
                onClick = { if (state.colorMode != ColorMode.RELATIVE) onToggleColorMode() },
                label = { Text("Relatif") },
            )
            FilterChip(
                selected = state.colorMode == ColorMode.ABSOLUTE,
                onClick = { if (state.colorMode != ColorMode.ABSOLUTE) onToggleColorMode() },
                label = { Text("Absolut") },
            )
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Kecepatan sepanjang perjalanan", fontWeight = FontWeight.SemiBold)
                Text(
                    "Warna sama dengan jalur di peta",
                    style = MaterialTheme.typography.bodySmall,
                )
                SpeedChart(
                    points = state.points,
                    colorMode = state.colorMode,
                    avgSpeedMps = avg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .padding(top = 12.dp),
                )
            }
        }

        state.trip?.let {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Stat("Tanggal", Formatters.dateTime(it.startTime))
                    Stat("Jarak", Formatters.distance(it.distanceMeters))
                    Stat("Durasi", Formatters.duration(it.durationSec))
                    Stat("Kecepatan rata-rata", "${(it.avgSpeedMps * 3.6).roundToInt()} km/j")
                    Stat("Kecepatan maksimum", "${(it.maxSpeedMps * 3.6).roundToInt()} km/j")
                }
            }
        }

        Text(
            "Zona perlambatan (${state.slowdowns.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        if (state.slowdowns.isEmpty()) {
            Text("Tidak ada perlambatan signifikan terdeteksi.")
        } else {
            state.slowdowns.forEach { zone ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Min ${(zone.minSpeedMps * 3.6).roundToInt()} km/j · " +
                                "rata ${(zone.avgSpeedMps * 3.6).roundToInt()} km/j",
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "Durasi ${Formatters.duration(zone.durationSec)} · " +
                                "@ ${"%.5f".format(zone.centerLat)}, ${"%.5f".format(zone.centerLon)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TimelineStatItem(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = Color.Unspecified,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
