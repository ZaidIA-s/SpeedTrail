package com.zaid.speedtrail.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zaid.speedtrail.data.model.Trip
import com.zaid.speedtrail.data.repository.TripRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = TripRepository.get(app)

    val trips: StateFlow<List<Trip>> = repo.observeTrips()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(tripId: Long) = viewModelScope.launch { repo.deleteTrip(tripId) }
}
