package com.stampbook.app.ui.passport

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stampbook.app.data.model.PassportStats
import com.stampbook.app.data.model.Stamp
import com.stampbook.app.ui.components.EmptyState
import com.stampbook.app.ui.components.StatTile
import com.stampbook.app.ui.stamp.StampMark

/**
 * The passport itself: a cover page of totals, then every stamp collected,
 * newest year first.
 */
@Composable
fun PassportScreen(
    viewModel: PassportViewModel,
    onStampClick: (Stamp) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 148.dp),
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            CoverPage(state.stats)
        }

        if (!state.loading && state.stampsByYear.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(
                    title = "No stamps yet",
                    body = "Tap Stamp to record the first place you have been. " +
                        "Everything stays on this device.",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                )
            }
        }

        state.stampsByYear.forEach { (year, stamps) ->
            item(span = { GridItemSpan(maxLineSpan) }, key = "year-$year") {
                YearDivider(year = year, count = stamps.size)
            }
            items(stamps, key = { it.id }) { stamp ->
                Box(
                    Modifier
                        .padding(4.dp)
                        .clickable { onStampClick(stamp) },
                    contentAlignment = Alignment.Center,
                ) {
                    StampMark(stamp = stamp, stampSize = 140.dp)
                }
            }
        }
    }
}

@Composable
private fun CoverPage(stats: PassportStats) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "PASSPORT",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "${stats.countriesVisited} countries",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${stats.worldPercentLabel} of the world",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { stats.worldFraction },
                modifier = Modifier.fillMaxWidth().height(6.dp),
            )
            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatTile(stats.stampCount.toString(), "stamps")
                StatTile(stats.citiesVisited.toString(), "cities")
                StatTile(stats.tripCount.toString(), "trips")
                StatTile("${stats.continentsVisited}/7", "continents")
            }
        }
    }
}

@Composable
private fun YearDivider(year: Int, count: Int) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(year.toString(), style = MaterialTheme.typography.titleLarge)
        HorizontalDivider(Modifier.weight(1f).padding(horizontal = 12.dp))
        Text(
            "$count ${if (count == 1) "stamp" else "stamps"}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
