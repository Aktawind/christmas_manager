package com.aktawind.monnoel.feature.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aktawind.monnoel.core.designsystem.theme.LocalMonNoelColors
import com.aktawind.monnoel.core.domain.budget.BudgetPost
import com.aktawind.monnoel.core.domain.budget.ExpenseDraft
import com.aktawind.monnoel.core.domain.budget.PostBudget
import com.aktawind.monnoel.core.domain.budget.YearBudget
import com.aktawind.monnoel.core.domain.gifts.centsToInput
import com.aktawind.monnoel.core.domain.gifts.formatEuro
import com.aktawind.monnoel.core.domain.gifts.parseEuroToCents
import com.aktawind.monnoel.core.model.Expense
import com.aktawind.monnoel.core.model.ExpenseCategory
import com.aktawind.monnoel.core.ui.DropdownField
import com.aktawind.monnoel.core.ui.EmptyState
import com.aktawind.monnoel.core.ui.MoneyInput
import com.aktawind.monnoel.core.ui.R as UiR
import com.aktawind.monnoel.core.ui.TextInput
import com.aktawind.monnoel.core.ui.resolve

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetRoute(viewModel: BudgetViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var editingBudget by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf(viewModel.initialExpenseId) }
    var creating by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it.resolve(context))
            viewModel.messageShown()
        }
    }
    val overview = state.overview
    val budget = overview?.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(budget?.let { stringResource(R.string.budget_title_year, it.year) } ?: stringResource(R.string.budget_title))
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (budget != null && !state.readOnly) {
                ExtendedFloatingActionButton(
                    onClick = { creating = true },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.fab_expense)) },
                )
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (budget == null) {
                if (!state.loading) {
                    item { EmptyState("💰", stringResource(R.string.budget_title), stringResource(R.string.budget_no_year)) }
                }
                return@LazyColumn
            }
            item { SummaryCard(budget, state.readOnly, onEditBudget = { editingBudget = true }) }
            item { BreakdownCard(budget) }
            item { ExpensesHeader(overview.expenses) }
            if (overview.expenses.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.expenses_empty),
                        color = LocalMonNoelColors.current.secondaryText,
                        modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth(),
                    )
                }
            }
            items(overview.expenses, key = { it.id }) { expense ->
                ExpenseRow(expense, state.readOnly, onClick = { editing = expense.id }, onPaid = { viewModel.setPaid(expense.id, it) })
            }
            if (overview.history.count { it.spentCents > 0 } >= 2) {
                item { YearsCard(overview.history) }
            }
        }
    }

    if (editingBudget && budget != null) {
        GlobalBudgetDialog(
            year = budget.year,
            current = budget.globalBudgetCents,
            onSave = {
                viewModel.setGlobalBudget(it)
                editingBudget = false
            },
            onDismiss = { editingBudget = false },
        )
    }
    val expense = overview?.expenses?.firstOrNull { it.id == editing }
    if (creating || expense != null) {
        ExpenseDialog(
            expense = expense,
            readOnly = state.readOnly,
            onSave = { draft ->
                viewModel.saveExpense(expense?.id, draft)
                creating = false
                editing = null
            },
            onDelete = expense?.let { e ->
                {
                    viewModel.delete(e.id)
                    editing = null
                }
            },
            onDismiss = {
                creating = false
                editing = null
            },
        )
    }
}

@Composable
private fun SummaryCard(budget: YearBudget, readOnly: Boolean, onEditBudget: () -> Unit) {
    val colors = LocalMonNoelColors.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // « Prévu » = ce qu'on a prévu de dépenser (budget global, sinon enveloppes et budgets) ;
            // « Dépensé » = le réel.
            val expected = budget.expectedCents
            val global = budget.globalBudgetCents
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                expected?.let { Figure(stringResource(R.string.budget_planned), formatEuro(it), Modifier.weight(1f)) }
                Figure(stringResource(R.string.budget_spent), formatEuro(budget.spentCents), Modifier.weight(1f))
            }
            if (expected != null && expected > 0) {
                val left = expected - budget.spentCents
                // Dépassement : seule la couleur de la barre change, sans message.
                LinearProgressIndicator(
                    progress = { (budget.spentCents.toFloat() / expected).coerceIn(0f, 1f) },
                    color = if (left < 0) colors.overBudget else colors.gold,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (left >= 0) {
                    Text(
                        stringResource(R.string.budget_margin, formatEuro(left)),
                        color = colors.secondaryText,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Text(
                stringResource(
                    when {
                        global != null -> R.string.budget_from_global
                        expected != null -> R.string.budget_from_posts
                        else -> R.string.budget_none
                    },
                ),
                color = colors.secondaryText,
                style = MaterialTheme.typography.bodySmall,
            )
            if (!readOnly) {
                TextButton(onClick = onEditBudget) {
                    Text(stringResource(if (global == null) R.string.budget_set else R.string.budget_change))
                }
            }
        }
    }
}

@Composable
private fun Figure(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = LocalMonNoelColors.current.secondaryText)
        Text(
            value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BreakdownCard(budget: YearBudget) {
    Column(Modifier.widthIn(max = 840.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            stringResource(R.string.breakdown_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.semantics { heading() },
        )
        StackedBar(
            budget.posts.map { BarSegment(it.post.chartColor(), it.plannedCents) },
            description = stringResource(R.string.breakdown_description),
        )
        // Seuls les postes qui ont des montants apparaissent.
        budget.posts.filter { it.plannedCents > 0 || it.spentCents > 0 || it.budgetCents != null }.forEach { PostRow(it) }
    }
}

/** Ligne de légende : la couleur du poste, son nom, prévu / dépensé, et son budget éventuel. */
@Composable
private fun PostRow(post: PostBudget) {
    val colors = LocalMonNoelColors.current
    Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(12.dp).background(post.post.chartColor(), CircleShape))
            Text(
                "${post.post.emoji}  ${stringResource(post.post.label)}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            // « Prévu » = le budget renseigné (enveloppes, budgets des calendriers) ; sinon le dépensé seul.
            val budgetCents = post.budgetCents
            Text(
                if (budgetCents != null) {
                    stringResource(R.string.post_amounts, formatEuro(budgetCents), formatEuro(post.spentCents))
                } else {
                    stringResource(R.string.post_spent_only, formatEuro(post.spentCents))
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        val budgetCents = post.budgetCents
        if (budgetCents != null) {
            Text(
                stringResource(if (post.post == BudgetPost.GIFTS) R.string.post_envelopes else R.string.post_budgets),
                style = MaterialTheme.typography.bodySmall,
                color = colors.secondaryText,
                modifier = Modifier.padding(start = 22.dp),
            )
            // Une fine barre : dépensé / prévu du poste, plus orangée s'il est dépassé.
            if (budgetCents > 0) {
                LinearProgressIndicator(
                    progress = { (post.spentCents.toFloat() / budgetCents).coerceIn(0f, 1f) },
                    color = if (post.isOverSpent) colors.overBudget else colors.gold,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(start = 22.dp, top = 4.dp),
                )
            }
        }
        if (post.post == BudgetPost.MEALS && post.plannedCents > post.spentCents) {
            Text(
                stringResource(R.string.post_meals_hint),
                style = MaterialTheme.typography.bodySmall,
                color = colors.secondaryText,
                modifier = Modifier.padding(start = 22.dp),
            )
        }
    }
}

@Composable
private fun ExpensesHeader(expenses: List<Expense>) {
    Column(Modifier.widthIn(max = 840.dp).fillMaxWidth().padding(top = 8.dp)) {
        HorizontalDivider(Modifier.padding(bottom = 16.dp), color = LocalMonNoelColors.current.gold.copy(alpha = 0.35f))
        Text(
            stringResource(R.string.expenses_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.semantics { heading() },
        )
        if (expenses.isNotEmpty()) {
            Text(
                stringResource(
                    R.string.expenses_amounts,
                    formatEuro(expenses.sumOf { it.amountCents }),
                    formatEuro(expenses.filter { it.paid }.sumOf { it.amountCents }),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = LocalMonNoelColors.current.secondaryText,
            )
        }
    }
}

@Composable
private fun ExpenseRow(expense: Expense, readOnly: Boolean, onClick: () -> Unit, onPaid: (Boolean) -> Unit) {
    Column(Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(checked = expense.paid, onCheckedChange = if (readOnly) null else onPaid)
            Text(expense.category.emoji, style = MaterialTheme.typography.titleMedium)
            Column(Modifier.weight(1f)) {
                Text(expense.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(if (expense.paid) R.string.expense_paid else R.string.expense_to_pay),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalMonNoelColors.current.secondaryText,
                )
            }
            Text(formatEuro(expense.amountCents), style = MaterialTheme.typography.bodyLarge)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun YearsCard(history: List<YearBudget>) {
    Column(Modifier.widthIn(max = 840.dp).fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider(color = LocalMonNoelColors.current.gold.copy(alpha = 0.35f))
        Text(
            stringResource(R.string.budget_years_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.semantics { heading() },
        )
        YearBars(
            bars = history.takeLast(6).map { YearBar(it.year.toString(), it.spentCents, formatEuro(it.spentCents)) },
            color = BudgetPost.GIFTS.chartColor(),
        )
    }
}

@Composable
private fun GlobalBudgetDialog(year: Int, current: Long?, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var value by rememberSaveable { mutableStateOf(centsToInput(current)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.budget_dialog_title, year)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.budget_dialog_hint), style = MaterialTheme.typography.bodySmall)
                MoneyInput(value, { value = it }, stringResource(R.string.budget_global), Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(value) }, enabled = value.isBlank() || parseEuroToCents(value) != null) {
                Text(stringResource(UiR.string.action_save))
            }
        },
        dismissButton = {
            Row {
                if (current != null) TextButton(onClick = { onSave("") }) { Text(stringResource(R.string.budget_remove)) }
                TextButton(onClick = onDismiss) { Text(stringResource(UiR.string.action_cancel)) }
            }
        },
    )
}

@Composable
private fun ExpenseDialog(
    expense: Expense?,
    readOnly: Boolean,
    onSave: (ExpenseDraft) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(expense?.title.orEmpty()) }
    var amount by rememberSaveable { mutableStateOf(centsToInput(expense?.amountCents)) }
    var category by rememberSaveable { mutableStateOf(expense?.category ?: ExpenseCategory.TREE) }
    var paid by rememberSaveable { mutableStateOf(expense?.paid ?: false) }
    var notes by rememberSaveable { mutableStateOf(expense?.notes.orEmpty()) }
    val cents = parseEuroToCents(amount)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (expense == null) R.string.expense_new else R.string.expense_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownField(
                    label = stringResource(R.string.field_expense_category),
                    selected = category,
                    options = ExpenseCategory.entries,
                    optionLabel = { "${it.emoji}  ${stringResource(it.label)}" },
                    onSelect = { c ->
                        // Le nom suit la catégorie tant qu'il n'a pas été personnalisé.
                        if (title.isBlank() ||
                            ExpenseCategory.entries.any { e -> title == categoryDefaultTitle(e) }
                        ) {
                            title = categoryDefaultTitle(c)
                        }
                        category = c
                    },
                    enabled = !readOnly,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextInput(title, { title = it }, stringResource(R.string.field_expense_title), Modifier.fillMaxWidth(), enabled = !readOnly)
                MoneyInput(amount, {
                    amount = it
                }, stringResource(R.string.field_expense_amount), Modifier.fillMaxWidth(), enabled = !readOnly)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = paid, onCheckedChange = { paid = it }, enabled = !readOnly)
                    Text(stringResource(R.string.field_expense_paid))
                }
                TextInput(notes, { notes = it }, stringResource(R.string.field_expense_notes), Modifier.fillMaxWidth(), enabled = !readOnly)
            }
        },
        confirmButton = {
            if (!readOnly) {
                TextButton(
                    onClick = { onSave(ExpenseDraft(title, cents ?: 0, category, paid, notes)) },
                    enabled =
                    title.isNotBlank() && cents != null,
                ) {
                    Text(stringResource(UiR.string.action_save))
                }
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null && !readOnly) {
                    TextButton(onClick = onDelete) {
                        Text(stringResource(UiR.string.action_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(UiR.string.action_cancel)) }
            }
        },
    )
}

/** Nom proposé pour une nouvelle dépense selon sa catégorie. */
private fun categoryDefaultTitle(category: ExpenseCategory): String = when (category) {
    ExpenseCategory.TREE -> "Sapin"
    ExpenseCategory.DECORATION -> "Décoration"
    ExpenseCategory.WRAPPING -> "Papier cadeau"
    ExpenseCategory.CLOTHES -> "Tenues de fête"
    ExpenseCategory.OUTINGS -> "Sortie"
    ExpenseCategory.OTHER -> ""
}
