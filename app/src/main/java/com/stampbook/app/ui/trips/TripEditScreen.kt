package com.stampbook.app.ui.trips

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stampbook.app.ui.components.DatePickerSheet

private enum class EditingField { NONE, START, END }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripEditScreen(
    viewModel: TripEditViewModel,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf(EditingField.NONE) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isNew) "New trip" else "Edit trip") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = form.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Trip name") },
                placeholder = { Text("Summer in Portugal") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            DateRow(
                label = "STARTS",
                value = form.startDate.format(TRIP_DATE),
                onClick = { editing = EditingField.START },
            )

            DateRow(
                label = "ENDS",
                value = form.endDate?.format(TRIP_DATE) ?: "Not set",
                onClick = { editing = EditingField.END },
                trailing = if (form.endDate != null) {
                    { TextButton(onClick = { viewModel.onEndDateChange(null) }) { Text("Clear") } }
                } else {
                    null
                },
            )

            if (form.dateRangeInvalid) {
                Text(
                    "The end date is before the start date.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            OutlinedTextField(
                value = form.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("Notes (optional)") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = { viewModel.save(onSaved) },
                enabled = form.canSave,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(if (viewModel.isNew) "Create trip" else "Save changes")
            }
        }
    }

    when (editing) {
        EditingField.START -> DatePickerSheet(
            initialDate = form.startDate,
            onDismiss = { editing = EditingField.NONE },
            onPicked = viewModel::onStartDateChange,
        )

        EditingField.END -> DatePickerSheet(
            initialDate = form.endDate ?: form.startDate,
            onDismiss = { editing = EditingField.NONE },
            onPicked = { viewModel.onEndDateChange(it) },
        )

        EditingField.NONE -> Unit
    }
}

@Composable
private fun DateRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        ListItem(
            headlineContent = { Text(value) },
            overlineContent = { Text(label, style = MaterialTheme.typography.labelSmall) },
            trailingContent = trailing,
        )
    }
}
