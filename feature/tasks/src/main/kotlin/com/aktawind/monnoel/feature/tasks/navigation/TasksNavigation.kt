package com.aktawind.monnoel.feature.tasks.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.aktawind.monnoel.core.model.LinkType
import com.aktawind.monnoel.feature.tasks.TasksRoute
import com.aktawind.monnoel.feature.tasks.edit.TaskEditRoute
import kotlinx.serialization.Serializable

@Serializable
data object TasksDestination

@Serializable
data class TaskEditDestination(val taskId: String? = null, val module: String? = null)

fun NavGraphBuilder.tasksGraph(navController: NavController, onOpenLink: (LinkType, String) -> Unit) {
    composable<TasksDestination> {
        TasksRoute(
            onOpenTask = { navController.navigate(TaskEditDestination(it)) },
            onNewTask = { module -> navController.navigate(TaskEditDestination(module = module.name)) },
            onOpenLink = onOpenLink,
        )
    }
    composable<TaskEditDestination> { TaskEditRoute(onDone = { navController.popBackStack() }, onOpenLink = onOpenLink) }
}
