package com.zaid.speedtrail.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zaid.speedtrail.data.model.TrackPoint
import com.zaid.speedtrail.data.model.Trip
import com.zaid.speedtrail.data.repository.TripRepository
import com.zaid.speedtrail.util.SlowdownDetector
import com.zaid.speedtrail.util.SlowdownZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ColorMode { ABSOLUTE, RELATIVE }

data class DetailState(
    val trip: Trip? = null,
    val points: List<TrackPoint> = emptyList(),
    val slowdowns: List<SlowdownZone> = emptyList(),
    val colorMode: ColorMode = ColorMode.RELATIVE,
    val loading: Boolean = true,
)

class DetailViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = TripRepository.get(app)
    private val _state = MutableStateFlow(DetailState())
    val state: StateFlow<DetailState> = _state.asStateFlow()

    fun load(tripId: Long) {
        viewModelScope.launch {
            val trip = repo.getTrip(tripId)
            val points = repo.getPoints(tripId)
            val zones = SlowdownDetector.detect(points)
            _state.value = DetailState(
                trip = trip,
                points = points,
                slowdowns = zones,
                loading = false,
            )
        }
    }

    fun toggleColorMode() {
        _state.value = _state.value.copy(
            colorMode = if (_state.value.colorMode == ColorMode.RELATIVE) ColorMode.ABSOLUTE
            else ColorMode.RELATIVE
        )
    }
}
