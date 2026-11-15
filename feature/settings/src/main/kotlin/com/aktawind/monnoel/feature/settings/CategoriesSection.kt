package com.aktawind.monnoel.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aktawind.monnoel.core.common.IdGenerator
import com.aktawind.monnoel.core.designsystem.theme.LocalMonNoelColors
import com.aktawind.monnoel.core.domain.cooking.CookingRepository
import com.aktawind.monnoel.core.domain.repository.GiftCategoryRepository
import com.aktawind.monnoel.core.model.GiftCategory
import com.aktawind.monnoel.core.model.RecipeCategory
import com.aktawind.monnoel.core.ui.TextInput
import com.aktawind.monnoel.feature.settings.navigation.CategoriesDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Une catégorie, de cadeaux ou de recettes : l'écran de réglage est le même. */
data class EditableCategory(val id: String, val name: String, val sortOrder: Int)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val giftCategories: GiftCategoryRepository,
    private val cooking: CookingRepository,
    private val ids: IdGenerator,
) : ViewModel() {
    private val recipes = savedStateHandle.toRoute<CategoriesDestination>().recipes

    val uiState: StateFlow<List<EditableCategory>> =
        (
            if (recipes) {
                cooking.observeCategories().map { list -> list.map { EditableCategory(it.id, it.name, it.sortOrder) } }
            } else {
                giftCategories.observeCategories().map { list -> list.map { EditableCategory(it.id, it.name, it.sortOrder) } }
            }
            ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private suspend fun save(category: EditableCategory) {
        if (recipes) {
            cooking.saveCategory(RecipeCategory(category.id, category.name, category.sortOrder))
        } else {
            giftCategories.upsert(GiftCategory(category.id, category.name, category.sortOrder))
        }
    }

    fun add(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val order = (uiState.value.maxOfOrNull { it.sortOrder } ?: -1) + 1
            save(EditableCategory(ids.newId(), name.trim(), order))
        }
    }

    fun rename(category: EditableCategory, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { save(category.copy(name = name.trim())) }
    }

    fun delete(category: EditableCategory) {
        viewModelScope.launch { if (recipes) cooking.deleteCategory(category.id) else giftCategories.delete(category.id) }
    }
}

/** Sous-écran des réglages : catégories de cadeaux ou de recettes (ajouter, renommer, supprimer). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesRoute(recipes: Boolean, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (recipes) R.string.settings_recipe_categories else R.string.settings_categories)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxWidth()) { CategoriesSection(recipes) }
        }
    }
}

@Composable
internal fun CategoriesSection(recipes: Boolean, viewModel: CategoriesViewModel = hiltViewModel()) {
    val categories by viewModel.uiState.collectAsStateWithLifecycle()
    var newName by rememberSaveable { mutableStateOf("") }
    var renaming by remember { mutableStateOf<EditableCategory?>(null) }
    var deleting by remember { mutableStateOf<EditableCategory?>(null) }

    SettingsCard(stringResource(R.string.settings_categories_mine)) {
        Text(
            stringResource(if (recipes) R.string.settings_recipe_categories_explain else R.string.settings_categories_explain),
            style = MaterialTheme.typography.bodyMedium,
            color = LocalMonNoelColors.current.secondaryText,
        )
        Column {
            categories.forEach { category ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(category.name, modifier = Modifier.weight(1f))
                    IconButton(onClick = { renaming = category }) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.settings_category_rename, category.name),
                        )
                    }
                    IconButton(onClick = { deleting = category }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.settings_category_delete, category.name),
                        )
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            TextInput(newName, { newName = it }, stringResource(R.string.settings_category_new), Modifier.weight(1f))
            Button(onClick = {
                viewModel.add(newName)
                newName = ""
            }, enabled = newName.isNotBlank()) { Text(stringResource(R.string.settings_category_add)) }
        }
    }

    renaming?.let { category ->
        var name by rememberSaveable(category.id) { mutableStateOf(category.name) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text(stringResource(R.string.settings_category_rename_title)) },
            text = {
                TextInput(name, { name = it }, stringResource(R.string.settings_category_name), Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.rename(category, name)
                    renaming = null
                }, enabled = name.isNotBlank()) {
                    Text(stringResource(com.aktawind.monnoel.core.ui.R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    renaming = null
                }) { Text(stringResource(com.aktawind.monnoel.core.ui.R.string.action_cancel)) }
            },
        )
    }
    deleting?.let { category ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.settings_category_delete_title, category.name)) },
            text = {
                Text(
                    stringResource(
                        if (recipes) R.string.settings_recipe_category_delete_message else R.string.settings_category_delete_message,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(category)
                    deleting = null
                }) {
                    Text(
                        stringResource(com.aktawind.monnoel.core.ui.R.string.action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    deleting = null
                }) { Text(stringResource(com.aktawind.monnoel.core.ui.R.string.action_cancel)) }
            },
        )
    }
}
