package com.satguess

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.satguess.game.GameViewModel
import com.satguess.game.Phase
import com.satguess.game.Screen
import com.satguess.ui.FinalScreen
import com.satguess.ui.GameScreen
import com.satguess.ui.HomeScreen
import com.satguess.ui.Night
import com.satguess.ui.RoundResultScreen
import com.satguess.ui.SatGuessTheme
import com.satguess.ui.SettingsScreen
import com.satguess.ui.StatsScreen
import org.maplibre.android.MapLibre

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        enableEdgeToEdge()
        setContent {
            SatGuessTheme {
                Surface(Modifier.fillMaxSize(), color = Night) { App(viewModel) }
            }
        }
    }
}

@Composable
private fun App(vm: GameViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    var confirmQuit by rememberSaveable { mutableStateOf(false) }

    val game = state.game
    when {
        state.screen == Screen.GAME && game != null -> {
            BackHandler {
                if (game.phase == Phase.FINISHED) vm.navigate(Screen.HOME) else confirmQuit = true
            }
            when (game.phase) {
                Phase.GUESSING -> GameScreen(game, vm::setGuess, vm::submit)
                Phase.ROUND_RESULT -> RoundResultScreen(game, settings.units, vm::next)
                Phase.FINISHED -> FinalScreen(
                    game, settings.units,
                    onNewGame = vm::newGameSameMode,
                    onReplaySeed = vm::replaySameSeed,
                    onHome = { vm.navigate(Screen.HOME) },
                )
            }
        }
        state.screen == Screen.STATS -> {
            BackHandler { vm.navigate(Screen.HOME) }
            StatsScreen(stats, onBack = { vm.navigate(Screen.HOME) })
        }
        state.screen == Screen.SETTINGS -> {
            BackHandler { vm.navigate(Screen.HOME) }
            SettingsScreen(settings, onTimer = { vm.setTimer(it) }, onUnits = { vm.setUnits(it) },
                onBack = { vm.navigate(Screen.HOME) })
        }
        else -> HomeScreen(
            locationCount = state.list?.locations?.size,
            initialMode = settings.lastMode,
            onPlay = vm::startRandom,
            onPlaySeed = vm::startWithSeed,
            onStats = { vm.navigate(Screen.STATS) },
            onSettings = { vm.navigate(Screen.SETTINGS) },
        )
    }

    if (confirmQuit) {
        AlertDialog(
            onDismissRequest = { confirmQuit = false },
            title = { Text(stringResource(R.string.quit_title)) },
            text = { Text(stringResource(R.string.quit_text)) },
            confirmButton = {
                TextButton(onClick = { confirmQuit = false; vm.navigate(Screen.HOME) }) { Text(stringResource(R.string.quit)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmQuit = false }) { Text(stringResource(R.string.keep_playing)) }
            },
        )
    }
}
