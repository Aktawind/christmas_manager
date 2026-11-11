package com.aktawind.monnoel.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aktawind.monnoel.core.designsystem.theme.LocalMonNoelColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Petite étoile dorée à cinq branches. */
@Composable
fun GoldStar(modifier: Modifier = Modifier, size: Dp = 14.dp, color: Color = LocalMonNoelColors.current.gold) {
    Canvas(modifier.size(size)) {
        val center = Offset(this.size.width / 2, this.size.height / 2)
        val outer = this.size.minDimension / 2
        val inner = outer * 0.45f
        val path = Path()
        for (i in 0 until 10) {
            val radius = if (i % 2 == 0) outer else inner
            val angle = -PI / 2 + i * PI / 5
            val point = Offset(center.x + (radius * cos(angle)).toFloat(), center.y + (radius * sin(angle)).toFloat())
            if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        path.close()
        drawPath(path, color)
    }
}

/** Séparateur décoratif : ligne fine, étoile dorée au centre. */
@Composable
fun StarDivider(modifier: Modifier = Modifier) {
    val gold = LocalMonNoelColors.current.gold
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Canvas(Modifier.weight(1f).height(1.dp)) {
            drawLine(gold.copy(alpha = 0.5f), Offset.Zero, Offset(size.width, 0f))
        }
        GoldStar(size = 12.dp)
        Canvas(Modifier.weight(1f).height(1.dp)) {
            drawLine(gold.copy(alpha = 0.5f), Offset.Zero, Offset(size.width, 0f))
        }
    }
}
