package com.stampbook.app.ui.trips

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stampbook.app.data.StampbookRepository
import com.stampbook.app.data.country.Country
import com.stampbook.app.data.model.Stamp
import com.stampbook.app.data.model.Trip
import com.stampbook.app.data.model.TripWithStamps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

const val NO_TRIP_ID = -1L

data class TripSummary(
    val trip: Trip,
    val stampCount: Int,
    val countries: List<Country>,
)

class TripsViewModel(private val repository: StampbookRepository) : ViewModel() {

    val trips: StateFlow<List<TripSummary>> =
        combine(repository.trips, repository.stamps) { trips, stamps ->
            val byTrip = stamps.groupBy { it.tripId }
            trips.map { trip ->
                val tripStamps = byTrip[trip.id].orEmpty()
                TripSummary(
                    trip = trip,
                    stampCount = tripStamps.size,
                    countries = tripStamps.mapNotNull { it.country }.distinctBy { it.code },
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Stamps recorded without a trip; surfaced so nothing gets lost. */
    val looseStamps: StateFlow<List<Stamp>> = repository.stamps
        .map { stamps -> stamps.filter { it.tripId == null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteTrip(id: Long) {
        viewModelScope.launch { repository.deleteTrip(id) }
    }
}

class TripDetailViewModel(
    private val repository: StampbookRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val tripId: Long = savedStateHandle.get<Long>("tripId") ?: NO_TRIP_ID

    val trip: StateFlow<TripWithStamps?> = repository.tripWithStamps(tripId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun deleteStamp(id: Long) {
        viewModelScope.launch { repository.deleteStamp(id) }
    }

    fun removeStampFromTrip(id: Long) {
        viewModelScope.launch { repository.assignStampToTrip(id, null) }
    }

    fun deleteTrip(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteTrip(tripId)
            onDone()
        }
    }
}

data class TripFormState(
    val title: String = "",
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
    val notes: String = "",
    val loaded: Boolean = false,
) {
    val canSave: Boolean get() = title.isNotBlank() && (endDate == null || !endDate.isBefore(startDate))
    val dateRangeInvalid: Boolean get() = endDate != null && endDate.isBefore(startDate)
}

class TripEditViewModel(
    private val repository: StampbookRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val tripId: Long = savedStateHandle.get<Long>("tripId") ?: NO_TRIP_ID
    val isNew: Boolean get() = tripId == NO_TRIP_ID

    private val _form = MutableStateFlow(TripFormState(loaded = isNew))
    val form: StateFlow<TripFormState> = _form.asStateFlow()

    init {
        if (!isNew) {
            viewModelScope.launch {
                repository.tripWithStamps(tripId).first()?.trip?.let { trip ->
                    _form.value = TripFormState(
                        title = trip.title,
                        startDate = trip.startDate,
                        endDate = trip.endDate,
                        notes = trip.notes.orEmpty(),
                        loaded = true,
                    )
                }
            }
        }
    }

    fun onTitleChange(value: String) = _form.update { it.copy(title = value) }
    fun onNotesChange(value: String) = _form.update { it.copy(notes = value) }
    fun onStartDateChange(value: LocalDate) = _form.update {
        // Keep the range coherent when the start is dragged past the end.
        val end = it.endDate?.takeIf { end -> !end.isBefore(value) }
        it.copy(startDate = value, endDate = end)
    }

    fun onEndDateChange(value: LocalDate?) = _form.update { it.copy(endDate = value) }

    fun save(onSaved: (Long) -> Unit) {
        val state = _form.value
        if (!state.canSave) return
        viewModelScope.launch {
            val saved = repository.saveTrip(
                Trip(
                    id = if (isNew) 0L else tripId,
                    title = state.title.trim(),
                    startDate = state.startDate,
                    endDate = state.endDate,
                    notes = state.notes.takeIf { it.isNotBlank() },
                ),
            )
            // Room returns the new row id on insert and the existing id on update.
            onSaved(if (saved > 0) saved else tripId)
        }
    }
}

/** Shared helper for showing a handful of flags without spilling off the card. */
fun List<Country>.flagStrip(max: Int = 6): String {
    val shown = take(max).joinToString(" ") { it.flag }
    return if (size > max) "$shown  +${size - max}" else shown
}
