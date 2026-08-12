package com.stampbook.app.nav

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stampbook.app.ui.StampbookViewModelFactory
import com.stampbook.app.ui.add.AddStampScreen
import com.stampbook.app.ui.add.AddStampViewModel
import com.stampbook.app.ui.add.NO_STAMP_ID
import com.stampbook.app.ui.map.MapScreen
import com.stampbook.app.ui.map.MapViewModel
import com.stampbook.app.ui.passport.PassportScreen
import com.stampbook.app.ui.passport.PassportViewModel
import com.stampbook.app.ui.trips.NO_TRIP_ID
import com.stampbook.app.ui.trips.TripDetailScreen
import com.stampbook.app.ui.trips.TripDetailViewModel
import com.stampbook.app.ui.trips.TripEditScreen
import com.stampbook.app.ui.trips.TripEditViewModel
import com.stampbook.app.ui.trips.TripsScreen
import com.stampbook.app.ui.trips.TripsViewModel

private object Routes {
    const val PASSPORT = "passport"
    const val TRIPS = "trips"
    const val MAP = "map"
    const val TRIP_DETAIL = "trip/{tripId}"
    const val TRIP_EDIT = "tripEdit?tripId={tripId}"
    const val STAMP = "stamp?stampId={stampId}&tripId={tripId}"

    fun tripDetail(tripId: Long) = "trip/$tripId"
    fun tripEdit(tripId: Long = NO_TRIP_ID) = "tripEdit?tripId=$tripId"
    fun stamp(stampId: Long = NO_STAMP_ID, tripId: Long = NO_TRIP_ID) =
        "stamp?stampId=$stampId&tripId=$tripId"
}

private data class TopLevel(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val TOP_LEVEL = listOf(
    TopLevel(Routes.PASSPORT, "Passport", Icons.Default.Book),
    TopLevel(Routes.TRIPS, "Trips", Icons.Default.Luggage),
    TopLevel(Routes.MAP, "Map", Icons.Default.Public),
)

@Composable
fun StampbookApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.PASSPORT) {
        topLevel(navController)
        details(navController)
    }
}

private fun NavGraphBuilder.topLevel(navController: NavHostController) {
    composable(Routes.PASSPORT) {
        MainScaffold(
            navController = navController,
            title = "Passport",
            onFabClick = { navController.navigate(Routes.stamp()) },
            fabLabel = "New stamp",
        ) { padding ->
            val viewModel: PassportViewModel = viewModel(factory = StampbookViewModelFactory)
            PassportScreen(
                viewModel = viewModel,
                onStampClick = { navController.navigate(Routes.stamp(stampId = it.id)) },
                contentPadding = padding,
            )
        }
    }

    composable(Routes.TRIPS) {
        MainScaffold(
            navController = navController,
            title = "Trips",
            onFabClick = { navController.navigate(Routes.tripEdit()) },
            fabLabel = "New trip",
        ) { padding ->
            val viewModel: TripsViewModel = viewModel(factory = StampbookViewModelFactory)
            TripsScreen(
                viewModel = viewModel,
                onTripClick = { navController.navigate(Routes.tripDetail(it)) },
                onStampClick = { navController.navigate(Routes.stamp(stampId = it.id)) },
                contentPadding = padding,
            )
        }
    }

    composable(Routes.MAP) {
        MainScaffold(
            navController = navController,
            title = "Map",
            onFabClick = { navController.navigate(Routes.stamp()) },
            fabLabel = "New stamp",
        ) { padding ->
            val viewModel: MapViewModel = viewModel(factory = StampbookViewModelFactory)
            MapScreen(viewModel = viewModel, contentPadding = padding)
        }
    }
}

private fun NavGraphBuilder.details(navController: NavHostController) {
    composable(
        route = Routes.TRIP_DETAIL,
        arguments = listOf(navArgument("tripId") { type = NavType.LongType }),
    ) {
        val viewModel: TripDetailViewModel = viewModel(factory = StampbookViewModelFactory)
        TripDetailScreen(
            viewModel = viewModel,
            onBack = { navController.popBackStack() },
            onEdit = { navController.navigate(Routes.tripEdit(it)) },
            onAddStamp = { navController.navigate(Routes.stamp(tripId = it)) },
            onStampClick = { navController.navigate(Routes.stamp(stampId = it.id)) },
        )
    }

    composable(
        route = Routes.TRIP_EDIT,
        arguments = listOf(
            navArgument("tripId") {
                type = NavType.LongType
                defaultValue = NO_TRIP_ID
            },
        ),
    ) {
        val viewModel: TripEditViewModel = viewModel(factory = StampbookViewModelFactory)
        TripEditScreen(
            viewModel = viewModel,
            onBack = { navController.popBackStack() },
            onSaved = { tripId ->
                // Land on the trip that was just saved rather than back on the list.
                navController.popBackStack()
                if (viewModel.isNew) navController.navigate(Routes.tripDetail(tripId))
            },
        )
    }

    composable(
        route = Routes.STAMP,
        arguments = listOf(
            navArgument("stampId") {
                type = NavType.LongType
                defaultValue = NO_STAMP_ID
            },
            navArgument("tripId") {
                type = NavType.LongType
                defaultValue = NO_TRIP_ID
            },
        ),
    ) {
        val viewModel: AddStampViewModel = viewModel(factory = StampbookViewModelFactory)
        AddStampScreen(viewModel = viewModel, onClose = { navController.popBackStack() })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(
    navController: NavHostController,
    title: String,
    onFabClick: () -> Unit,
    fabLabel: String,
    content: @Composable (PaddingValues) -> Unit,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        topBar = { TopAppBar(title = { Text(title) }) },
        bottomBar = {
            NavigationBar {
                TOP_LEVEL.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            if (currentRoute != destination.route) {
                                navController.navigate(destination.route) {
                                    // Single top-level entry on the back stack, tabs keep their state.
                                    popUpTo(Routes.PASSPORT) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onFabClick) {
                Icon(Icons.Default.Add, contentDescription = fabLabel)
            }
        },
        content = content,
    )
}
