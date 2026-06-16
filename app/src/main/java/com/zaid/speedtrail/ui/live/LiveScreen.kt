package com.zaid.speedtrail.ui.live

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaid.speedtrail.service.TrackingStatus
import com.zaid.speedtrail.util.Formatters
import kotlin.math.roundToInt

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun LiveScreen(
    onOpenHistory: () -> Unit,
    viewModel: LiveViewModel = viewModel(),
) {
    val context = LocalContext.current
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    val permissions = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (granted) viewModel.start()
    }

    fun ensurePermissionThenStart() {
        val granted = ContextCompatChecks(context, Manifest.permission.ACCESS_FINE_LOCATION)
        if (granted) viewModel.start() else launcher.launch(permissions)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SpeedTrail") },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Default.History, contentDescription = "Riwayat")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Speedometer angka besar
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "${(stats.currentSpeedMps * 3.6).roundToInt()}",
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text("km/jam", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem("Jarak", Formatters.distance(stats.distanceMeters))
                StatItem("Durasi", Formatters.duration(stats.durationSec))
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem("Rata-rata", "${(stats.avgSpeedMps * 3.6).roundToInt()} km/j")
                StatItem("Maks", "${(stats.maxSpeedMps * 3.6).roundToInt()} km/j")
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Akurasi GPS: ±${stats.accuracyM.roundToInt()} m · ${stats.pointCount} titik",
                style = MaterialTheme.typography.bodySmall,
            )

            Spacer(Modifier.weight(1f))

            when (stats.status) {
                TrackingStatus.IDLE -> {
                    Button(
                        onClick = { ensurePermissionThenStart() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Mulai Perjalanan")
                    }
                }
                TrackingStatus.RECORDING, TrackingStatus.PAUSED -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = {
                                if (stats.status == TrackingStatus.PAUSED) viewModel.resume()
                                else viewModel.pause()
                            },
                            modifier = Modifier.weight(1f).height(56.dp),
                        ) {
                            val paused = stats.status == TrackingStatus.PAUSED
                            Icon(
                                if (paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = null,
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(if (paused) "Lanjut" else "Jeda")
                        }
                        Button(
                            onClick = { viewModel.stop() },
                            modifier = Modifier.weight(1f).height(56.dp),
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Selesai")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

/** Pengecekan izin sederhana tanpa import berantakan di body composable. */
private fun ContextCompatChecks(context: android.content.Context, permission: String): Boolean =
    context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
