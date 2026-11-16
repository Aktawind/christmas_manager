package com.aktawind.monnoel.feature.search

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aktawind.monnoel.core.designsystem.theme.LocalMonNoelColors
import com.aktawind.monnoel.core.domain.search.SearchResult
import com.aktawind.monnoel.core.model.LinkType
import com.aktawind.monnoel.core.ui.EmptyState
import com.aktawind.monnoel.core.ui.PersonAvatar
import com.aktawind.monnoel.core.ui.PhotoImage

private val LinkType.emoji: String
    get() = when (this) {
        LinkType.PERSON -> "🧑‍🎄"
        LinkType.IDEA -> "💡"
        LinkType.GIFT -> "🎁"
        LinkType.CALENDAR -> "🗓️"
        LinkType.BOX -> "📦"
        LinkType.TASK -> "📝"
        LinkType.RECIPE -> "📖"
        LinkType.PRODUCTION -> "🧁"
        LinkType.MEAL -> "🍽️"
        LinkType.EXPENSE -> "💰"
    }

@get:StringRes
private val LinkType.groupLabel: Int
    get() = when (this) {
        LinkType.PERSON -> R.string.search_persons
        LinkType.IDEA -> R.string.search_ideas
        LinkType.GIFT -> R.string.search_gifts
        LinkType.CALENDAR -> R.string.search_calendars
        LinkType.BOX -> R.string.search_boxes
        LinkType.TASK -> R.string.search_tasks
        LinkType.RECIPE -> R.string.search_recipes
        LinkType.PRODUCTION -> R.string.search_productions
        LinkType.MEAL -> R.string.search_meals
        LinkType.EXPENSE -> R.string.search_expenses
    }

@Composable
fun SearchRoute(onOpen: (LinkType, String) -> Unit, viewModel: SearchViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Le texte saisi vit dans l'écran (réaction immédiate) ; la recherche suit avec un léger délai.
    var text by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    viewModel.setQuery(it)
                },
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (text.isNotEmpty()) {
                        IconButton(onClick = {
                            text = ""
                            viewModel.setQuery("")
                        }) {
                            Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.search_clear))
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth().focusRequester(focus),
            )
        }
        if (text.isBlank()) {
            item {
                EmptyState(
                    emoji = "🔍",
                    title = stringResource(R.string.search_empty_title),
                    message = stringResource(R.string.search_empty_message),
                )
            }
        } else if (state.results.isEmpty() && state.query == text) {
            item {
                Text(
                    stringResource(R.string.search_no_result, text),
                    color = LocalMonNoelColors.current.secondaryText,
                    modifier = Modifier.padding(vertical = 32.dp),
                )
            }
        }
        state.results.entries.forEachIndexed { index, (type, list) ->
            item(key = "h-$type") {
                Column(Modifier.widthIn(max = 840.dp).fillMaxWidth().padding(top = if (index == 0) 20.dp else 24.dp, bottom = 4.dp)) {
                    if (index >
                        0
                    ) {
                        HorizontalDivider(Modifier.padding(bottom = 16.dp), color = LocalMonNoelColors.current.gold.copy(alpha = 0.35f))
                    }
                    Text(
                        "${type.emoji}  ${stringResource(type.groupLabel)} · ${list.size}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.semantics { heading() },
                    )
                }
            }
            items(list, key = { "${it.type}-${it.id}" }) { result -> ResultRow(result, onClick = { onOpen(result.type, result.id) }) }
        }
    }
}

@Composable
private fun ResultRow(result: SearchResult, onClick: () -> Unit) {
    Row(
        Modifier.widthIn(max = 840.dp).fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (result.type == LinkType.PERSON) {
            PersonAvatar(
                result.photoPath,
                result.title.split(" ").mapNotNull {
                    it.firstOrNull()?.uppercaseChar()
                }.take(2).joinToString(""),
                size = 44.dp,
            )
        } else {
            PhotoImage(result.photoPath, null, size = 44.dp, shape = MaterialTheme.shapes.small, placeholder = result.type.emoji)
        }
        Column(Modifier.weight(1f)) {
            Text(result.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (result.subtitle.isNotBlank()) {
                Text(
                    result.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalMonNoelColors.current.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
