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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.satguess.R
import com.satguess.data.Settings
import com.satguess.data.Stats
import com.satguess.data.Units
import com.satguess.game.MAX_ROUND_SCORE
import com.satguess.game.ROUNDS

@Composable
private fun SimpleScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        content()
    }
}

@Composable
fun StatsScreen(stats: Stats, onBack: () -> Unit) {
    SimpleScaffold(stringResource(R.string.stats), onBack) {
        val max = formatScore(MAX_ROUND_SCORE * ROUNDS)
        StatRow(stringResource(R.string.games_played), formatScore(stats.games))
        StatRow(stringResource(R.string.best_score), "${formatScore(stats.best)} / $max")
        StatRow(stringResource(R.string.average_score), "${formatScore(stats.average)} / $max")
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
    HorizontalDivider()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settings: Settings, onTimer: (Int) -> Unit, onUnits: (Units) -> Unit, onBack: () -> Unit) {
    SimpleScaffold(stringResource(R.string.settings), onBack) {
        Text(stringResource(R.string.timer), style = MaterialTheme.typography.titleMedium)
        val timers = listOf(0, 60, 120)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            timers.forEachIndexed { i, s ->
                SegmentedButton(
                    selected = settings.timerSeconds == s,
                    onClick = { onTimer(s) },
                    shape = SegmentedButtonDefaults.itemShape(i, timers.size),
                ) { Text(if (s == 0) stringResource(R.string.timer_off) else stringResource(R.string.timer_seconds, s)) }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.units), style = MaterialTheme.typography.titleMedium)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Units.entries.forEachIndexed { i, u ->
                SegmentedButton(
                    selected = settings.units == u,
                    onClick = { onUnits(u) },
                    shape = SegmentedButtonDefaults.itemShape(i, Units.entries.size),
                ) { Text(stringResource(if (u == Units.KM) R.string.units_km else R.string.units_mi)) }
            }
        }
    }
}
