package com.aktawind.monnoel.feature.cooking.meal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aktawind.monnoel.core.designsystem.theme.LocalMonNoelColors
import com.aktawind.monnoel.core.domain.gifts.formatEuro
import com.aktawind.monnoel.core.model.CourseType
import com.aktawind.monnoel.core.model.Meal
import com.aktawind.monnoel.core.model.MealDish
import com.aktawind.monnoel.core.ui.PhotoImage
import com.aktawind.monnoel.core.ui.emoji
import com.aktawind.monnoel.core.ui.label
import com.aktawind.monnoel.core.ui.resolve
import com.aktawind.monnoel.feature.cooking.R

/** Fiche d'un repas : qui, où, quand, et le menu rangé par moments. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealRoute(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onAddDish: (mealId: String, course: CourseType) -> Unit,
    onOpenDish: (mealId: String, dishId: String) -> Unit,
    viewModel: MealViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it.resolve(context))
            viewModel.messageShown()
        }
    }
    // Repas supprimé ailleurs : on revient.
    LaunchedEffect(state.loading, state.meal) { if (!state.loading && state.meal == null) onBack() }
    val meal = state.meal ?: return
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${meal.type.emoji}  ${meal.name}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(com.aktawind.monnoel.core.ui.R.string.action_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onEdit(meal.id) }) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.meal_edit_action))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item { MealHeader(meal, state.personNames) }
            // Les moments retenus pour ce repas, plus ceux qui contiennent déjà des plats.
            val shown = CourseType.entries.filter { c -> c in meal.courses || state.dishes.any { it.course == c } }
            shown.forEach { course ->
                val dishes = state.dishes.filter { it.course == course }
                if (state.readOnly && dishes.isEmpty()) return@forEach
                item(key = "h-$course") {
                    Column(Modifier.widthIn(max = 840.dp).fillMaxWidth().padding(top = 20.dp)) {
                        HorizontalDivider(Modifier.padding(bottom = 12.dp), color = LocalMonNoelColors.current.gold.copy(alpha = 0.35f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${course.emoji}  ${stringResource(course.label)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.weight(1f).semantics { heading() },
                            )
                            if (!state.readOnly) {
                                TextButton(onClick = { onAddDish(meal.id, course) }) {
                                    Icon(Icons.Filled.Add, contentDescription = null)
                                    Text(stringResource(R.string.dish_add))
                                }
                                if (dishes.isEmpty()) {
                                    val courseName = stringResource(course.label)
                                    IconButton(onClick = { viewModel.setCourse(course, false) }) {
                                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.course_remove, courseName))
                                    }
                                }
                            }
                        }
                    }
                }
                items(dishes, key = { it.id }) { dish ->
                    DishRow(dish, state.readOnly, onClick = {
                        onOpenDish(meal.id, dish.id)
                    }, onOrdered = { viewModel.setOrdered(dish.id, it) })
                }
            }
            val missing = CourseType.entries - shown.toSet()
            if (!state.readOnly && missing.isNotEmpty()) {
                item(key = "add-course") { AddCourseButton(missing, onAdd = { viewModel.setCourse(it, true) }) }
            }
        }
    }
}

@Composable
private fun AddCourseButton(missing: List<CourseType>, onAdd: (CourseType) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.widthIn(max = 840.dp).fillMaxWidth().padding(top = 20.dp)) {
        OutlinedButton(onClick = { open = true }) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(stringResource(R.string.course_add))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            missing.forEach { course ->
                DropdownMenuItem(
                    text = { Text("${course.emoji}  ${stringResource(course.label)}") },
                    onClick = {
                        open = false
                        onAdd(course)
                    },
                )
            }
        }
    }
}

@Composable
private fun MealHeader(meal: Meal, personNames: List<String>) {
    val secondary = LocalMonNoelColors.current.secondaryText
    Column(Modifier.widthIn(max = 840.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val whenWhere = listOfNotNull(meal.date?.format(mealDateFormat), meal.place.ifBlank { null }).joinToString(" · ")
        if (whenWhere.isNotBlank()) Text(whenWhere, style = MaterialTheme.typography.titleMedium)
        Text(
            pluralStringResource(R.plurals.meal_guests, meal.guestCount, meal.guestCount) +
                (personNames + meal.guests).takeIf { it.isNotEmpty() }?.joinToString(", ", prefix = " : ").orEmpty(),
            style = MaterialTheme.typography.bodyLarge,
        )
        meal.estimatedCostCents?.let {
            Text(stringResource(R.string.meal_cost_value, formatEuro(it)), style = MaterialTheme.typography.bodyMedium, color = secondary)
        }
        if (meal.notes.isNotBlank()) Text(meal.notes, style = MaterialTheme.typography.bodyMedium, color = secondary)
    }
}

@Composable
private fun DishRow(dish: MealDish, readOnly: Boolean, onClick: () -> Unit, onOrdered: (Boolean) -> Unit) {
    Column(Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PhotoImage(dish.photoPath, null, size = 44.dp, shape = MaterialTheme.shapes.small, placeholder = dish.course.emoji)
            Column(Modifier.weight(1f)) {
                Text(dish.name, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (dish.broughtBy.isNotBlank()) {
                    Text(
                        stringResource(R.string.dish_brought_by_value, dish.broughtBy),
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalMonNoelColors.current.secondaryText,
                    )
                }
            }
            if (dish.toOrder) {
                FilterChip(
                    selected = dish.ordered,
                    onClick = { onOrdered(!dish.ordered) },
                    enabled = !readOnly,
                    label = { Text(stringResource(if (dish.ordered) R.string.dish_ordered else R.string.dish_to_order)) },
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}
