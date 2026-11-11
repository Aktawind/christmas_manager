package com.aktawind.monnoel.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object MonNoelIcons {
    /** Paquet cadeau avec nœud. */
    val Gift: ImageVector by lazy {
        ImageVector.Builder("Gift", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                // Couvercle
                moveTo(3f, 8f)
                horizontalLineTo(21f)
                verticalLineTo(12f)
                horizontalLineTo(3f)
                close()
                // Boîte
                moveTo(4.5f, 13f)
                horizontalLineTo(19.5f)
                verticalLineTo(21f)
                horizontalLineTo(4.5f)
                close()
                // Nœud
                moveTo(12f, 7.5f)
                curveTo(10.5f, 4f, 6.5f, 3.5f, 7f, 6f)
                curveTo(7.3f, 7.3f, 9.5f, 7.5f, 12f, 7.5f)
                close()
                moveTo(12f, 7.5f)
                curveTo(13.5f, 4f, 17.5f, 3.5f, 17f, 6f)
                curveTo(16.7f, 7.3f, 14.5f, 7.5f, 12f, 7.5f)
                close()
            }
            // Ruban (évidé)
            path(fill = SolidColor(Color.White)) {
                moveTo(11.2f, 8f)
                horizontalLineTo(12.8f)
                verticalLineTo(21f)
                horizontalLineTo(11.2f)
                close()
            }
        }.build()
    }
}
