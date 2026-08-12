package com.stampbook.app.ui.passport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stampbook.app.data.StampbookRepository
import com.stampbook.app.data.model.PassportStats
import com.stampbook.app.data.model.Stamp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PassportUiState(
    val stampsByYear: List<Pair<Int, List<Stamp>>> = emptyList(),
    val stats: PassportStats = PassportStats.EMPTY,
    val loading: Boolean = true,
)

class PassportViewModel(private val repository: StampbookRepository) : ViewModel() {

    val uiState: StateFlow<PassportUiState> =
        combine(repository.stamps, repository.stats) { stamps, stats ->
            PassportUiState(
                // Newest year first, and newest stamp first inside each year.
                stampsByYear = stamps.groupBy { it.date.year }
                    .toList()
                    .sortedByDescending { it.first },
                stats = stats,
                loading = false,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PassportUiState())

    fun deleteStamp(id: Long) {
        viewModelScope.launch { repository.deleteStamp(id) }
    }
}
