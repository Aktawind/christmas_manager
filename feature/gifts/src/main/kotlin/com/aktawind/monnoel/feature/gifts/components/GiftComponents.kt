package com.aktawind.monnoel.feature.gifts.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aktawind.monnoel.core.designsystem.theme.LocalMonNoelColors
import com.aktawind.monnoel.core.domain.gifts.PersonGiftBudget
import com.aktawind.monnoel.core.domain.gifts.formatEuro
import com.aktawind.monnoel.core.model.GiftStatus
import com.aktawind.monnoel.core.model.PlannedGift
import com.aktawind.monnoel.core.ui.PhotoImage
import com.aktawind.monnoel.feature.gifts.R

@get:StringRes
val GiftStatus.label: Int
    get() = when (this) {
        GiftStatus.TO_BUY -> R.string.status_to_buy
        GiftStatus.ORDERED -> R.string.status_ordered
        GiftStatus.RECEIVED -> R.string.status_received
        GiftStatus.TO_WRAP -> R.string.status_to_wrap
        GiftStatus.WRAPPED -> R.string.status_wrapped
        GiftStatus.GIVEN -> R.string.status_given
    }

val GiftStatus.emoji: String
    get() = when (this) {
        GiftStatus.TO_BUY -> "🛒"
        GiftStatus.ORDERED -> "📦"
        GiftStatus.RECEIVED -> "📬"
        GiftStatus.TO_WRAP -> "✂️"
        GiftStatus.WRAPPED -> "🎀"
        GiftStatus.GIVEN -> "🎁"
    }

/** Badge de statut : un appui ouvre le menu déroulant des statuts. */
@Composable
fun StatusMenuChip(
    status: GiftStatus,
    enabled: Boolean,
    onChange: (GiftStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        AssistChip(
            onClick = { open = true },
            enabled = enabled,
            label = { Text("${status.emoji} ${stringResource(status.label)}") },
            trailingIcon = if (enabled) {
                { Icon(Icons.Filled.ArrowDropDown, contentDescription = stringResource(R.string.status_change)) }
            } else {
                null
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = if (status == GiftStatus.GIVEN) {
                    MaterialTheme.colorScheme.tertiaryContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
            ),
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            GiftStatus.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            "${option.emoji}  ${stringResource(option.label)}",
                            fontWeight = if (option == status) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        open = false
                        onChange(option)
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("🏬  ${stringResource(R.string.status_bought_in_store)}") },
                onClick = {
                    open = false
                    onChange(GiftStatus.TO_WRAP)
                },
            )
        }
    }
}

@Composable
fun GiftRow(
    gift: PlannedGift,
    showRecipient: Boolean,
    readOnly: Boolean,
    onClick: () -> Unit,
    onStatus: (GiftStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PhotoImage(gift.photoPath, null, size = 56.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    gift.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val recipient = gift.recipientName.takeIf { showRecipient && it.isNotBlank() }
                    ?.let { stringResource(R.string.gift_for, it) }
                val details = listOfNotNull(
                    recipient,
                    gift.priceCents?.let { formatEuro(it) },
                    gift.shop.takeIf { it.isNotBlank() },
                ).joinToString(" · ")
                if (details.isNotBlank()) {
                    Text(
                        details,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalMonNoelColors.current.secondaryText,
                    )
                }
            }
            StatusMenuChip(gift.status, enabled = !readOnly, onChange = onStatus)
        }
    }
}

/** Avancement doux d'une enveloppe : jamais de rouge, juste une indication. */
@Composable
fun EnvelopeProgress(budget: PersonGiftBudget?, modifier: Modifier = Modifier) {
    val secondary = LocalMonNoelColors.current.secondaryText
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val planned = budget?.plannedCents ?: 0
        val envelope = budget?.envelopeCents
        val text = if (envelope != null) {
            stringResource(R.string.envelope_progress, formatEuro(planned), formatEuro(envelope))
        } else {
            stringResource(R.string.envelope_none, formatEuro(planned))
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, color = secondary)
        budget?.envelopeRatio?.let { ratio ->
            LinearProgressIndicator(
                progress = { ratio.coerceIn(0f, 1f) },
                color = LocalMonNoelColors.current.gold,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
            if (budget.isOverEnvelope) {
                Text(
                    stringResource(R.string.envelope_over),
                    style = MaterialTheme.typography.bodySmall,
                    color = secondary,
                )
            }
        }
    }
}
