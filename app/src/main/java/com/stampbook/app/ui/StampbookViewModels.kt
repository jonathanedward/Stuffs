package com.stampbook.app.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stampbook.app.StampbookApplication
import com.stampbook.app.data.StampbookRepository
import com.stampbook.app.ui.add.AddStampViewModel
import com.stampbook.app.ui.map.MapViewModel
import com.stampbook.app.ui.passport.PassportViewModel
import com.stampbook.app.ui.trips.TripDetailViewModel
import com.stampbook.app.ui.trips.TripEditViewModel
import com.stampbook.app.ui.trips.TripsViewModel

private fun CreationExtras.repository(): StampbookRepository =
    (this[APPLICATION_KEY] as StampbookApplication).container.repository

/** One factory for every screen; the graph is small enough not to need more. */
val StampbookViewModelFactory = viewModelFactory {
    initializer { PassportViewModel(repository()) }
    initializer { TripsViewModel(repository()) }
    initializer { MapViewModel(repository()) }
    initializer { TripDetailViewModel(repository(), createSavedStateHandle()) }
    initializer { TripEditViewModel(repository(), createSavedStateHandle()) }
    initializer { AddStampViewModel(repository(), createSavedStateHandle()) }
}
