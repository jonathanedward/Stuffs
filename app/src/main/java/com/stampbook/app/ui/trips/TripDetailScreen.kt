package com.stampbook.app.ui.trips

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stampbook.app.data.model.Stamp
import com.stampbook.app.ui.components.EmptyState
import com.stampbook.app.ui.stamp.StampMark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    viewModel: TripDetailViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onAddStamp: (Long) -> Unit,
    onStampClick: (Stamp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trip by viewModel.trip.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(trip?.trip?.title.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    trip?.let { loaded ->
                        IconButton(onClick = { onEdit(loaded.trip.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit trip")
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete trip")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            trip?.let { loaded ->
                ExtendedFloatingActionButton(
                    onClick = { onAddStamp(loaded.trip.id) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add stamp") },
                )
            }
        },
    ) { padding ->
        val loaded = trip ?: return@Scaffold

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 148.dp),
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text(
                        buildDateLine(loaded.trip.startDate.format(TRIP_DATE), loaded.trip.endDate?.format(TRIP_DATE)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    loaded.trip.notes?.let { notes ->
                        Spacer(Modifier.height(10.dp))
                        Text(notes, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (loaded.stamps.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        title = "No stamps on this trip",
                        body = "Add the places you went and they will show up here and on the map.",
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            items(loaded.stamps, key = { it.id }) { stamp ->
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

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this trip?") },
            text = { Text("The stamps you collected on it are kept in your passport.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        viewModel.deleteTrip(onBack)
                    },
                ) { Text("Delete trip") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}

private fun buildDateLine(start: String, end: String?): String =
    if (end == null) start else "$start – $end"
