package com.stampbook.app.ui.trips

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stampbook.app.data.model.Stamp
import com.stampbook.app.ui.components.EmptyState
import java.time.format.DateTimeFormatter

internal val TRIP_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

@Composable
fun TripsScreen(
    viewModel: TripsViewModel,
    onTripClick: (Long) -> Unit,
    onStampClick: (Stamp) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val trips by viewModel.trips.collectAsStateWithLifecycle()
    val loose by viewModel.looseStamps.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (trips.isEmpty()) {
            item {
                EmptyState(
                    title = "No trips yet",
                    body = "A trip groups the stamps you collected on one journey, " +
                        "and draws its route on the map.",
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        items(trips, key = { it.trip.id }) { summary ->
            TripCard(summary = summary, onClick = { onTripClick(summary.trip.id) })
        }

        if (loose.isNotEmpty()) {
            item {
                Text(
                    "Not in a trip",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 20.dp, top = 16.dp),
                )
            }
            items(loose, key = { "loose-${it.id}" }) { stamp ->
                LooseStampRow(stamp = stamp, onClick = { onStampClick(stamp) })
            }
        }
    }
}

@Composable
private fun TripCard(summary: TripSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(summary.trip.title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(2.dp))
            Text(
                dateRangeLabel(summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (summary.countries.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(summary.countries.flagStrip(), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${summary.stampCount} ${if (summary.stampCount == 1) "stamp" else "stamps"}" +
                    " · ${summary.countries.size} ${if (summary.countries.size == 1) "country" else "countries"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun dateRangeLabel(summary: TripSummary): String {
    val start = summary.trip.startDate.format(TRIP_DATE)
    val end = summary.trip.endDate?.format(TRIP_DATE)
    val nights = summary.trip.nights
    return when {
        end == null -> start
        nights != null && nights > 0 -> "$start – $end · $nights ${if (nights == 1L) "night" else "nights"}"
        else -> "$start – $end"
    }
}

@Composable
private fun LooseStampRow(stamp: Stamp, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stamp.country?.flag.orEmpty(), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.padding(horizontal = 6.dp))
        Column(Modifier.weight(1f)) {
            Text(stamp.headline, style = MaterialTheme.typography.bodyLarge)
            stamp.subhead?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            stamp.date.format(TRIP_DATE),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
