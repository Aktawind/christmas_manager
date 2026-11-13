package com.aktawind.monnoel.core.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.aktawind.monnoel.core.common.MediaPaths
import java.io.File

/** Affiche une photo de l'application, ou un emoji de remplacement. */
@Composable
fun PhotoImage(
    path: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    shape: Shape = MaterialTheme.shapes.medium,
    placeholder: String = "🎁",
) {
    val context = LocalContext.current
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (path != null) {
            AsyncImage(
                model = MediaPaths.resolve(context.filesDir, path),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                placeholder,
                style = if (size >=
                    96.dp
                ) {
                    MaterialTheme.typography.displaySmall
                } else {
                    MaterialTheme.typography.titleLarge
                },
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
    }
}

/** Avatar rond : photo, ou initiales. */
@Composable
fun PersonAvatar(path: String?, initials: String, modifier: Modifier = Modifier, size: Dp = 48.dp) {
    val context = LocalContext.current
    Box(
        modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (path != null) {
            AsyncImage(
                model = MediaPaths.resolve(context.filesDir, path),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                initials.ifBlank { "🎅" },
                style = initialsStyle(size),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

/** Initiales en police système, plus grandes sur les grands avatars. */
@Composable
private fun initialsStyle(size: Dp): TextStyle {
    val base = if (size >= 72.dp) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium
    return base.copy(fontFamily = FontFamily.Default)
}

/**
 * Choix d'une photo : galerie ou appareil photo. [onPicked] reçoit l'URI à importer.
 */
@Composable
fun PhotoField(
    path: String?,
    onPicked: (String) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "🎁",
) {
    val context = LocalContext.current
    var cameraUri by rememberSaveable { mutableStateOf<String?>(null) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { onPicked(it.toString()) }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) cameraUri?.let(onPicked)
    }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        PhotoImage(path, stringResource(R.string.photo_current), size = 112.dp, placeholder = placeholder)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(
                enabled = enabled,
                onClick = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            ) { Text(stringResource(R.string.photo_gallery)) }
            OutlinedButton(
                enabled = enabled,
                onClick = {
                    val uri = newCameraUri(context)
                    cameraUri = uri.toString()
                    camera.launch(uri)
                },
            ) { Text(stringResource(R.string.photo_camera)) }
            if (path != null) {
                TextButton(enabled = enabled, onClick = onRemove) { Text(stringResource(R.string.photo_remove)) }
            }
        }
    }
}

private fun newCameraUri(context: android.content.Context): Uri {
    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File(dir, "photo-${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
}
