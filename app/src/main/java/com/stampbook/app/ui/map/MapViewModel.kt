package com.stampbook.app.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stampbook.app.data.StampbookRepository
import com.stampbook.app.data.model.PassportStats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class MapUiState(
    val visitedCodes: Set<String> = emptySet(),
    val routes: List<MapRoute> = emptyList(),
    val stats: PassportStats = PassportStats.EMPTY,
)

class MapViewModel(repository: StampbookRepository) : ViewModel() {

    val uiState: StateFlow<MapUiState> =
        combine(repository.stamps, repository.stats) { stamps, stats ->
            MapUiState(
                visitedCodes = stamps.map { it.countryCode }.toSet(),
                // One route per trip, stops in the order they were stamped.
                routes = stamps
                    .filter { it.tripId != null }
                    .groupBy { it.tripId }
                    .values
                    .mapNotNull { tripStamps ->
                        val stops = tripStamps
                            .sortedBy { it.date }
                            .mapNotNull { it.country }
                            .distinctUntilChangedByCode()
                        stops.takeIf { it.size > 1 }?.let { MapRoute(it) }
                    },
                stats = stats,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapUiState())
}

/** Collapses consecutive stamps in the same country so a route has no zero-length legs. */
private fun List<com.stampbook.app.data.country.Country>.distinctUntilChangedByCode() =
    filterIndexed { index, country -> index == 0 || this[index - 1].code != country.code }
