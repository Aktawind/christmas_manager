package com.aktawind.monnoel.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Petit gâteau (cupcake) pour l'onglet Cuisine. */
val MonNoelIcons.Cupcake: ImageVector
    get() = cupcake

private val cupcake: ImageVector by lazy {
    ImageVector.Builder("Cupcake", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            // Glaçage
            moveTo(5f, 11f)
            curveTo(4f, 8f, 7f, 5.5f, 9.5f, 6.5f)
            curveTo(10f, 4f, 14f, 4f, 14.5f, 6.5f)
            curveTo(17f, 5.5f, 20f, 8f, 19f, 11f)
            close()
            // Cerise
            moveTo(12f, 2f)
            curveTo(13.1f, 2f, 13.5f, 3f, 13.2f, 3.8f)
            curveTo(12.8f, 4.6f, 11.2f, 4.6f, 10.8f, 3.8f)
            curveTo(10.5f, 3f, 10.9f, 2f, 12f, 2f)
            close()
            // Caissette
            moveTo(5.5f, 12.5f)
            horizontalLineTo(18.5f)
            lineTo(17f, 21f)
            horizontalLineTo(7f)
            close()
        }
    }.build()
}
