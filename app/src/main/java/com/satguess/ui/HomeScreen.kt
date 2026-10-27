package com.satguess.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.satguess.R
import com.satguess.game.Mode
import com.satguess.game.SeedError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    locationCount: Int?,
    initialMode: Mode,
    onPlay: (Mode) -> Unit,
    onPlaySeed: (String) -> SeedError?,
    onStats: () -> Unit,
    onSettings: () -> Unit,
) {
    var mode by rememberSaveable(initialMode) { mutableStateOf(initialMode) }
    var showSeedDialog by remember { mutableStateOf(false) }
    val ready = locationCount != null

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.Public, null, Modifier.height(72.dp).fillMaxWidth(), tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.app_name), fontSize = 44.sp, fontWeight = FontWeight.Bold)
        Text(
            stringResource(R.string.tagline),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(40.dp))

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            Mode.entries.forEachIndexed { i, m ->
                SegmentedButton(
                    selected = mode == m,
                    onClick = { mode = m },
                    shape = SegmentedButtonDefaults.itemShape(i, Mode.entries.size),
                ) { Text(stringResource(if (m == Mode.MOVE) R.string.mode_move else R.string.mode_no_move)) }
            }
        }
        Text(
            stringResource(if (mode == Mode.MOVE) R.string.mode_move_desc else R.string.mode_no_move_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        Spacer(Modifier.height(24.dp))

        Button(onClick = { onPlay(mode) }, enabled = ready, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(stringResource(R.string.play), fontSize = 18.sp)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { showSeedDialog = true }, enabled = ready, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.play_with_seed))
        }
        Spacer(Modifier.height(24.dp))
        SecondaryActions(onStats, onSettings)
        Spacer(Modifier.height(24.dp))
        Text(
            if (locationCount == null) stringResource(R.string.loading)
            else stringResource(R.string.locations_count, formatScore(locationCount)),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (showSeedDialog) SeedDialog(onDismiss = { showSeedDialog = false }, onSubmit = onPlaySeed)
}

@Composable
private fun SecondaryActions(onStats: () -> Unit, onSettings: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onStats) {
            Icon(Icons.Default.BarChart, null)
            Text(stringResource(R.string.stats), Modifier.padding(start = 6.dp))
        }
        TextButton(onClick = onSettings) {
            Icon(Icons.Default.Settings, null)
            Text(stringResource(R.string.settings), Modifier.padding(start = 6.dp))
        }
    }
}

@Composable
private fun SeedDialog(onDismiss: () -> Unit, onSubmit: (String) -> SeedError?) {
    var text by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<SeedError?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.enter_seed)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(12); error = null },
                placeholder = { Text(stringResource(R.string.seed_hint)) },
                singleLine = true,
                isError = error != null,
                supportingText = error?.let {
                    { Text(stringResource(if (it == SeedError.INVALID) R.string.seed_invalid else R.string.seed_wrong_version)) }
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false),
            )
        },
        confirmButton = {
            TextButton(onClick = { error = onSubmit(text) }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
