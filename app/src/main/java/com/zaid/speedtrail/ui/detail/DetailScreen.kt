package com.zaid.speedtrail.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaid.speedtrail.ui.components.RouteMapView
import com.zaid.speedtrail.ui.components.SpeedChart
import com.zaid.speedtrail.util.Formatters
import kotlin.math.roundToInt

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    tripId: Long,
    onBack: () -> Unit,
    viewModel: DetailViewModel = viewModel(),
) {
    LaunchedEffect(tripId) { viewModel.load(tripId) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Perjalanan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleColorMode() }) {
                        Icon(Icons.Default.Palette, contentDescription = "Mode warna")
                    }
                },
            )
        },
    ) { padding ->
        if (state.loading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        val trip = state.trip
        val avg = trip?.avgSpeedMps ?: 0.0

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                RouteMapView(
                    points = state.points,
                    colorMode = state.colorMode,
                    avgSpeedMps = avg,
                    slowdowns = state.slowdowns,
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.colorMode == ColorMode.RELATIVE,
                        onClick = { if (state.colorMode != ColorMode.RELATIVE) viewModel.toggleColorMode() },
                        label = { Text("Relatif") },
                    )
                    FilterChip(
                        selected = state.colorMode == ColorMode.ABSOLUTE,
                        onClick = { if (state.colorMode != ColorMode.ABSOLUTE) viewModel.toggleColorMode() },
                        label = { Text("Absolut") },
                    )
                }
            }

            item {
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
                            modifier = Modifier.fillMaxWidth().height(140.dp).padding(top = 12.dp),
                        )
                    }
                }
            }

            item {
                trip?.let {
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
            }

            item {
                Text(
                    "Zona perlambatan (${state.slowdowns.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (state.slowdowns.isEmpty()) {
                item { Text("Tidak ada perlambatan signifikan terdeteksi.") }
            } else {
                items(state.slowdowns) { zone ->
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
