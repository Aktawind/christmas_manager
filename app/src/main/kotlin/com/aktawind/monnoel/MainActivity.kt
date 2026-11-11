package com.aktawind.monnoel

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aktawind.monnoel.core.designsystem.theme.MonNoelTheme
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
                val shared by viewModel.sharedIdea.collectAsStateWithLifecycle()
                shared?.let { idea ->
                    QuickIdeaDialog(
                        initialTitle = idea.title,
                        initialUrl = idea.url,
                        onSave = viewModel::saveSharedIdea,
                        onDismiss = viewModel::dismissSharedIdea,
                    )
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
