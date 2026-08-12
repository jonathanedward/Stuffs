package com.stampbook.app.ui.add

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stampbook.app.data.country.Country
import com.stampbook.app.data.model.Stamp
import com.stampbook.app.data.model.Trip
import com.stampbook.app.ui.components.DatePickerSheet
import com.stampbook.app.ui.stamp.StampMark
import java.time.format.DateTimeFormatter

private val LONG_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStampScreen(
    viewModel: AddStampViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val trips by viewModel.trips.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEditing) "Edit stamp" else "New stamp") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (viewModel.isEditing) {
                        IconButton(onClick = { viewModel.delete(onClose) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete stamp")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (form.selected == null) {
            CountryPicker(
                query = form.query,
                results = form.results,
                onQueryChange = viewModel::onQueryChange,
                onPick = viewModel::onCountrySelected,
                modifier = Modifier.padding(padding).fillMaxSize(),
            )
            return@Scaffold
        }

        val country = form.selected!!
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                StampPreview(
                    stamp = Stamp(
                        countryCode = country.code,
                        city = form.city,
                        date = form.date,
                        seed = viewModel.previewSeed,
                    ),
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    ListItem(
                        headlineContent = { Text(country.name) },
                        supportingContent = { Text(country.continent.displayName) },
                        leadingContent = { Text(country.flag, style = MaterialTheme.typography.headlineSmall) },
                        trailingContent = {
                            TextButton(onClick = viewModel::clearCountry) { Text("Change") }
                        },
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = form.city,
                    onValueChange = viewModel::onCityChange,
                    label = { Text("City (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    modifier = Modifier.clickable { showDatePicker = true },
                ) {
                    ListItem(
                        headlineContent = { Text(form.date.format(LONG_DATE)) },
                        overlineContent = { Text("DATE", style = MaterialTheme.typography.labelSmall) },
                        trailingContent = { Text("Change", color = MaterialTheme.colorScheme.primary) },
                    )
                }
            }

            item {
                TripSelector(
                    trips = trips,
                    selectedTripId = form.tripId,
                    onSelect = viewModel::onTripChange,
                )
            }

            item {
                OutlinedTextField(
                    value = form.note,
                    onValueChange = viewModel::onNoteChange,
                    label = { Text("Note (optional)") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                Button(
                    onClick = { viewModel.save(onClose) },
                    enabled = form.canSave,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text(if (viewModel.isEditing) "Save changes" else "Stamp it")
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerSheet(
            initialDate = form.date,
            onDismiss = { showDatePicker = false },
            onPicked = viewModel::onDateChange,
        )
    }
}

@Composable
private fun StampPreview(stamp: Stamp) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(190.dp),
        contentAlignment = Alignment.Center,
    ) {
        StampMark(stamp = stamp, stampSize = 168.dp)
    }
}

@Composable
private fun CountryPicker(
    query: String,
    results: List<Country>,
    onQueryChange: (String) -> Unit,
    onPick: (Country) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Where were you?") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        if (results.isEmpty()) {
            Text(
                "No match for \"$query\".",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(32.dp),
            )
        }
        LazyColumn {
            items(results, key = { it.code }) { country ->
                ListItem(
                    headlineContent = { Text(country.name) },
                    supportingContent = { Text(country.continent.displayName) },
                    leadingContent = {
                        Text(country.flag, style = MaterialTheme.typography.headlineSmall)
                    },
                    modifier = Modifier.clickable { onPick(country) },
                )
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripSelector(
    trips: List<Trip>,
    selectedTripId: Long?,
    onSelect: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedTitle = trips.firstOrNull { it.id == selectedTripId }?.title ?: "Not part of a trip"

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedTitle,
            onValueChange = {},
            readOnly = true,
            label = { Text("Trip") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Not part of a trip") },
                onClick = {
                    onSelect(null)
                    expanded = false
                },
            )
            trips.forEach { trip ->
                DropdownMenuItem(
                    text = { Text(trip.title) },
                    onClick = {
                        onSelect(trip.id)
                        expanded = false
                    },
                )
            }
        }
    }
}
