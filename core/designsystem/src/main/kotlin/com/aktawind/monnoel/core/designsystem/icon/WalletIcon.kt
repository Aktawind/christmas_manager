package com.aktawind.monnoel.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Porte-monnaie pour l'onglet Budget. */
val MonNoelIcons.Wallet: ImageVector
    get() = wallet

private val wallet: ImageVector by lazy {
    ImageVector.Builder("Wallet", 24.dp, 24.dp, 24f, 24f).apply {
        // Rabat
        path(fill = SolidColor(Color.Black)) {
            moveTo(5f, 4f)
            lineTo(16f, 4f)
            lineTo(17.5f, 7f)
            lineTo(5f, 7f)
            close()
        }
        // Corps, avec le fermoir découpé
        path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
            moveTo(5f, 8f)
            lineTo(19f, 8f)
            curveTo(20.1f, 8f, 21f, 8.9f, 21f, 10f)
            lineTo(21f, 18f)
            curveTo(21f, 19.1f, 20.1f, 20f, 19f, 20f)
            lineTo(5f, 20f)
            curveTo(3.9f, 20f, 3f, 19.1f, 3f, 18f)
            lineTo(3f, 10f)
            curveTo(3f, 8.9f, 3.9f, 8f, 5f, 8f)
            close()
            moveTo(15f, 12f)
            lineTo(21f, 12f)
            lineTo(21f, 16f)
            lineTo(15f, 16f)
            curveTo(13.9f, 16f, 13f, 15.1f, 13f, 14f)
            curveTo(13f, 12.9f, 13.9f, 12f, 15f, 12f)
            close()
        }
    }.build()
}
