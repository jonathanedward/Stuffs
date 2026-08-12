package com.stampbook.app.ui.add

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stampbook.app.core.StampStyles
import com.stampbook.app.data.StampbookRepository
import com.stampbook.app.data.country.Countries
import com.stampbook.app.data.country.Country
import com.stampbook.app.data.model.Stamp
import com.stampbook.app.data.model.Trip
import com.stampbook.app.ui.trips.NO_TRIP_ID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

const val NO_STAMP_ID = -1L

data class StampFormState(
    val query: String = "",
    val results: List<Country> = Countries.all,
    val selected: Country? = null,
    val city: String = "",
    val date: LocalDate = LocalDate.now(),
    val note: String = "",
    val tripId: Long? = null,
) {
    val canSave: Boolean get() = selected != null && !date.isAfter(LocalDate.now().plusYears(50))
}

class AddStampViewModel(
    private val repository: StampbookRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val stampId: Long = savedStateHandle.get<Long>("stampId") ?: NO_STAMP_ID
    private val presetTripId: Long = savedStateHandle.get<Long>("tripId") ?: NO_TRIP_ID
    val isEditing: Boolean get() = stampId != NO_STAMP_ID

    /** Held so an edit never regenerates the stamp's look. */
    private var editingSeed: Int? = null

    /**
     * The seed the preview draws with, and the one the new stamp is saved with, so
     * what you see while filling the form is exactly what lands in the passport.
     */
    var previewSeed: Int = StampStyles.newSeed()
        private set

    private val _form = MutableStateFlow(
        StampFormState(tripId = presetTripId.takeIf { it != NO_TRIP_ID }),
    )
    val form: StateFlow<StampFormState> = _form.asStateFlow()

    val trips: StateFlow<List<Trip>> = repository.trips
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (isEditing) {
            viewModelScope.launch {
                repository.stamp(stampId).first()?.let { stamp ->
                    editingSeed = stamp.seed
                    previewSeed = stamp.seed
                    _form.value = StampFormState(
                        selected = stamp.country,
                        city = stamp.city.orEmpty(),
                        date = stamp.date,
                        note = stamp.note.orEmpty(),
                        tripId = stamp.tripId,
                    )
                }
            }
        }
    }

    fun onQueryChange(value: String) = _form.update {
        it.copy(query = value, results = Countries.search(value))
    }

    fun onCountrySelected(country: Country) = _form.update { it.copy(selected = country) }

    /** Drops back to the search list without losing anything else already typed. */
    fun clearCountry() = _form.update { it.copy(selected = null, query = "", results = Countries.all) }
    fun onCityChange(value: String) = _form.update { it.copy(city = value) }
    fun onDateChange(value: LocalDate) = _form.update { it.copy(date = value) }
    fun onNoteChange(value: String) = _form.update { it.copy(note = value) }
    fun onTripChange(value: Long?) = _form.update { it.copy(tripId = value) }

    fun save(onSaved: () -> Unit) {
        val state = _form.value
        val country = state.selected ?: return
        viewModelScope.launch {
            val seed = editingSeed
            if (isEditing && seed != null) {
                repository.updateStamp(
                    Stamp(
                        id = stampId,
                        tripId = state.tripId,
                        countryCode = country.code,
                        city = state.city,
                        date = state.date,
                        note = state.note,
                        seed = seed,
                    ),
                )
            } else {
                repository.addStamp(
                    countryCode = country.code,
                    city = state.city,
                    date = state.date,
                    note = state.note,
                    tripId = state.tripId,
                    seed = previewSeed,
                )
            }
            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        if (!isEditing) return
        viewModelScope.launch {
            repository.deleteStamp(stampId)
            onDeleted()
        }
    }
}
