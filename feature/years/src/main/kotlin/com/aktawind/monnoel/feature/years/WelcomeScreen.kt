package com.aktawind.monnoel.feature.years

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aktawind.monnoel.core.designsystem.component.GoldStar
import com.aktawind.monnoel.core.designsystem.theme.Champagne
import com.aktawind.monnoel.core.designsystem.theme.FirGreen
import com.aktawind.monnoel.core.designsystem.theme.FirGreenDark
import com.aktawind.monnoel.core.designsystem.theme.Ink
import com.aktawind.monnoel.core.designsystem.theme.Ivory
import com.aktawind.monnoel.core.ui.ImportBackupDialog
import com.aktawind.monnoel.core.ui.resolve

/** Premier lancement : créer son premier Noël ou restaurer une sauvegarde. */
@Composable
fun WelcomeRoute(viewModel: WelcomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.inspect(it.toString()) }
    }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it.resolve(context))
            viewModel.messageShown()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(FirGreen, FirGreenDark))),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(32.dp)
                .widthIn(max = 560.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                GoldStar(size = 18.dp, color = Champagne)
                GoldStar(size = 28.dp, color = Champagne)
                GoldStar(size = 18.dp, color = Champagne)
            }
            Text("🎄", style = MaterialTheme.typography.displayLarge, modifier = Modifier.clearAndSetSemantics { })
            Text(
                stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.displaySmall,
                color = Ivory,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.welcome_message),
                style = MaterialTheme.typography.bodyLarge,
                color = Ivory.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
            )
            if (state.busy) {
                CircularProgressIndicator(color = Champagne, modifier = Modifier.size(48.dp))
            } else {
                Button(
                    onClick = viewModel::start,
                    colors = ButtonDefaults.buttonColors(containerColor = Champagne, contentColor = Ink),
                ) {
                    Text(
                        stringResource(R.string.welcome_start, state.year),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                OutlinedButton(
                    onClick = { picker.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Ivory),
                ) {
                    Text(stringResource(R.string.welcome_restore))
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).safeDrawingPadding())
    }

    state.pendingImport?.let { (_, summary) ->
        ImportBackupDialog(
            summary = summary,
            replacesExistingData = false,
            onConfirm = viewModel::confirmImport,
            onDismiss = viewModel::cancelImport,
        )
    }
}
