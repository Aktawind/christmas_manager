package com.aktawind.monnoel.feature.budget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aktawind.monnoel.core.designsystem.theme.LocalMonNoelColors

/** Un segment de barre empilée : sa couleur et sa valeur. */
internal data class BarSegment(val color: Color, val value: Long)

/**
 * Barre horizontale empilée (part de chaque poste). Fine, extrémités arrondies, 2 dp de fond
 * entre les segments. Les valeurs sont dans la légende juste dessous (vue tableau).
 */
@Composable
internal fun StackedBar(segments: List<BarSegment>, description: String, modifier: Modifier = Modifier) {
    val total = segments.sumOf { it.value }.coerceAtLeast(1)
    val track = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier.fillMaxWidth().height(16.dp).semantics { contentDescription = description }) {
        val gap = 2.dp.toPx()
        val radius = CornerRadius(4.dp.toPx())
        val visible = segments.filter { it.value > 0 }
        if (visible.isEmpty()) {
            drawRoundRect(track, cornerRadius = radius)
            return@Canvas
        }
        val usable = size.width - gap * (visible.size - 1)
        var x = 0f
        visible.forEachIndexed { index, segment ->
            val w = usable * segment.value / total
            val first = index == 0
            val last = index == visible.lastIndex
            roundedSegment(segment.color, Offset(x, 0f), Size(w, size.height), radius, first, last)
            x += w + gap
        }
    }
}

/** Rectangle arrondi seulement aux extrémités de la barre complète. */
private fun DrawScope.roundedSegment(color: Color, topLeft: Offset, size: Size, r: CornerRadius, first: Boolean, last: Boolean) {
    val path = Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                left = topLeft.x,
                top = topLeft.y,
                right = topLeft.x + size.width,
                bottom = topLeft.y + size.height,
                topLeftCornerRadius = if (first) r else CornerRadius.Zero,
                bottomLeftCornerRadius = if (first) r else CornerRadius.Zero,
                topRightCornerRadius = if (last) r else CornerRadius.Zero,
                bottomRightCornerRadius = if (last) r else CornerRadius.Zero,
            ),
        )
    }
    drawPath(path, color)
}

/** Une colonne du graphique par année. */
internal data class YearBar(val label: String, val value: Long, val valueLabel: String)

/**
 * Colonnes verticales, une seule série (le coût prévu de chaque Noël) : pas de légende, le
 * titre la nomme ; chaque colonne porte sa valeur au-dessus et son année dessous.
 */
@Composable
internal fun YearBars(bars: List<YearBar>, color: Color, modifier: Modifier = Modifier) {
    val max = bars.maxOfOrNull { it.value }?.coerceAtLeast(1) ?: 1
    val axis = MaterialTheme.colorScheme.outlineVariant
    val secondary = LocalMonNoelColors.current.secondaryText
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(160.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            bars.forEach { bar ->
                // Chaque colonne part de la ligne de base (en bas), sa valeur juste au-dessus.
                Column(
                    Modifier.weight(1f).fillMaxHeight().clearAndSetSemantics {
                        contentDescription = "${bar.label} : ${bar.valueLabel}"
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Text(bar.valueLabel, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                    val fraction = bar.value.toFloat() / max
                    Box(
                        Modifier
                            .padding(top = 4.dp)
                            .widthIn(max = 72.dp)
                            .fillMaxWidth()
                            .height((120 * fraction).coerceAtLeast(2f).dp)
                            .background(color, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)),
                    )
                }
            }
        }
        Canvas(Modifier.fillMaxWidth().height(1.dp)) { drawRect(axis) }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            bars.forEach { bar ->
                Text(
                    bar.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).clearAndSetSemantics { },
                )
            }
        }
    }
}
