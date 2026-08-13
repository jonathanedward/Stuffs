package com.stampbook.app.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stampbook.app.ui.components.ProgressRow
import com.stampbook.app.ui.components.StatTile

@Composable
fun MapScreen(
    viewModel: MapViewModel,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                WorldChoroplethMap(
                    visitedCodes = state.visitedCodes,
                    routes = state.routes,
                    ocean = MaterialTheme.colorScheme.surface,
                    land = MaterialTheme.colorScheme.surfaceVariant,
                    visited = MaterialTheme.colorScheme.secondary,
                    border = MaterialTheme.colorScheme.outline,
                    route = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.9f)
                        .clip(RoundedCornerShape(18.dp)),
                )
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LegendKey(MaterialTheme.colorScheme.secondary, "Stamped")
                    Spacer(Modifier.width(14.dp))
                    LegendKey(MaterialTheme.colorScheme.surfaceVariant, "Not yet")
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Pinch to zoom",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatTile(state.stats.worldPercentLabel, "of the world")
                StatTile(state.stats.countriesVisited.toString(), "countries")
                StatTile(state.stats.territoriesVisited.toString(), "territories")
            }
        }

        item {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("By continent", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                state.stats.perContinent.forEach { progress ->
                    ProgressRow(
                        label = progress.continentName,
                        visited = progress.visited,
                        total = progress.total,
                        fraction = progress.fraction,
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendKey(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(9.dp)
                .background(color, CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
