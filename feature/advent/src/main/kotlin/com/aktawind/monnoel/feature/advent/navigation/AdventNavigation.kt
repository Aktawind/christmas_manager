package com.aktawind.monnoel.feature.advent.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.aktawind.monnoel.feature.advent.box.BoxEditRoute
import com.aktawind.monnoel.feature.advent.calendar.CalendarRoute
import com.aktawind.monnoel.feature.advent.edit.CalendarEditRoute
import com.aktawind.monnoel.feature.advent.list.AdventListRoute
import kotlinx.serialization.Serializable

@Serializable
data object AdventDestination

@Serializable
data class CalendarDestination(val calendarId: String)

@Serializable
data class CalendarEditDestination(val calendarId: String? = null, val readyMade: Boolean = false)

@Serializable
data class BoxEditDestination(val boxId: String)

fun NavGraphBuilder.adventGraph(navController: NavController) {
    composable<AdventDestination> {
        AdventListRoute(
            onOpenCalendar = { navController.navigate(CalendarDestination(it)) },
            onNewCalendar = { navController.navigate(CalendarEditDestination()) },
            onNewReadyMade = { navController.navigate(CalendarEditDestination(readyMade = true)) },
            onOpenReadyMade = { navController.navigate(CalendarEditDestination(it)) },
        )
    }
    composable<CalendarDestination> {
        CalendarRoute(
            onBack = { navController.popBackStack() },
            onEdit = { navController.navigate(CalendarEditDestination(it)) },
            onOpenBox = { navController.navigate(BoxEditDestination(it)) },
            // Un calendrier tout prêt n'a pas de cases : on ouvre directement sa fiche.
            onReadyMade = { id ->
                navController.popBackStack()
                navController.navigate(CalendarEditDestination(id))
            },
        )
    }
    composable<CalendarEditDestination> {
        CalendarEditRoute(
            onSaved = { id, openGrid ->
                navController.popBackStack()
                if (openGrid) navController.navigate(CalendarDestination(id))
            },
            onCancel = { navController.popBackStack() },
        )
    }
    composable<BoxEditDestination> { BoxEditRoute(onDone = { navController.popBackStack() }) }
}
