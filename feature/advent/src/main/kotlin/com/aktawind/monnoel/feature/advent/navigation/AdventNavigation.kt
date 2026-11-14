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
data class CalendarEditDestination(val calendarId: String? = null)

@Serializable
data class BoxEditDestination(val boxId: String)

fun NavGraphBuilder.adventGraph(navController: NavController) {
    composable<AdventDestination> {
        AdventListRoute(
            onOpenCalendar = { navController.navigate(CalendarDestination(it)) },
            onNewCalendar = { navController.navigate(CalendarEditDestination()) },
        )
    }
    composable<CalendarDestination> {
        CalendarRoute(
            onBack = { navController.popBackStack() },
            onEdit = { navController.navigate(CalendarEditDestination(it)) },
            onOpenBox = { navController.navigate(BoxEditDestination(it)) },
        )
    }
    composable<CalendarEditDestination> {
        CalendarEditRoute(
            onSaved = { id, isNew ->
                navController.popBackStack()
                if (isNew) navController.navigate(CalendarDestination(id))
            },
            onCancel = { navController.popBackStack() },
        )
    }
    composable<BoxEditDestination> { BoxEditRoute(onDone = { navController.popBackStack() }) }
}
