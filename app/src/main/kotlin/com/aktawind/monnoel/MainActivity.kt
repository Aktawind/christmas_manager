package com.aktawind.monnoel

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aktawind.monnoel.core.designsystem.theme.MonNoelTheme
import com.aktawind.monnoel.core.domain.gifts.PreviewIssue
import com.aktawind.monnoel.core.model.ThemePreference
import com.aktawind.monnoel.core.ui.QuickIdeaDialog
import com.aktawind.monnoel.ui.MonNoelApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { viewModel.uiState.value is MainUiState.Loading }
        enableEdgeToEdge()
        if (savedInstanceState == null) handleShare(intent)
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val ready = state as? MainUiState.Ready ?: return@setContent
            val dark = when (ready.theme) {
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
            }
            MonNoelTheme(darkTheme = dark) {
                MonNoelApp(hasEditions = ready.hasEditions)
                val sharedLoading by viewModel.sharedLoading.collectAsStateWithLifecycle()
                if (sharedLoading) SharedLoadingDialog(onSkip = viewModel::skipSharedLoading)
                val shared by viewModel.sharedIdea.collectAsStateWithLifecycle()
                shared?.let { idea ->
                    val persons by viewModel.persons.collectAsStateWithLifecycle()
                    key(idea) {
                        QuickIdeaDialog(
                            persons = persons,
                            initialTitle = idea.title,
                            initialUrl = idea.url,
                            initialPriceCents = idea.priceCents,
                            photoPath = idea.photoPath,
                            note = previewNote(idea.previewIssue, idea.previewCode),
                            onSave = viewModel::saveSharedIdea,
                            onDismiss = viewModel::dismissSharedIdea,
                        )
                    }
                }
                val saved by viewModel.ideaSaved.collectAsStateWithLifecycle()
                LaunchedEffect(saved) {
                    if (saved) {
                        Toast.makeText(this@MainActivity, R.string.share_saved, Toast.LENGTH_SHORT).show()
                        viewModel.ideaSavedShown()
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShare(intent)
    }

    private fun handleShare(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            viewModel.onShared(intent.getStringExtra(Intent.EXTRA_TEXT), intent.getStringExtra(Intent.EXTRA_SUBJECT))
        }
    }
}

@Composable
private fun SharedLoadingDialog(onSkip: () -> Unit) {
    AlertDialog(
        onDismissRequest = onSkip,
        title = { Text(stringResource(com.aktawind.monnoel.core.ui.R.string.quick_idea_title)) },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(32.dp))
                Text(stringResource(com.aktawind.monnoel.core.ui.R.string.quick_idea_loading))
            }
        },
        confirmButton = {
            TextButton(onClick = onSkip) { Text(stringResource(com.aktawind.monnoel.core.ui.R.string.quick_idea_skip)) }
        },
    )
}

@Composable
private fun previewNote(issue: PreviewIssue?, code: Int?): String? = when (issue) {
    null -> null
    PreviewIssue.PAGE_REFUSED -> stringResource(com.aktawind.monnoel.core.ui.R.string.preview_refused, code ?: 0)
    PreviewIssue.PAGE_UNREACHABLE -> stringResource(com.aktawind.monnoel.core.ui.R.string.preview_unreachable)
    PreviewIssue.IMAGE_NOT_FOUND -> stringResource(com.aktawind.monnoel.core.ui.R.string.preview_no_image)
    PreviewIssue.IMAGE_NOT_LOADED -> stringResource(com.aktawind.monnoel.core.ui.R.string.preview_image_failed)
}
