package com.satguess.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.satguess.data.Location
import com.satguess.data.LocationList
import com.satguess.data.LocationRepository
import com.satguess.data.Prefs
import com.satguess.data.Settings
import com.satguess.data.Stats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen { HOME, GAME, STATS, SETTINGS }

enum class Phase { GUESSING, ROUND_RESULT, FINISHED }

data class GeoPoint(val lat: Double, val lon: Double)

data class RoundResult(val location: Location, val guess: GeoPoint?, val distanceKm: Double?, val points: Int)

data class Game(
    val seed: Seed,
    val locations: List<Location>,
    val results: List<RoundResult> = emptyList(),
    val phase: Phase = Phase.GUESSING,
    val guess: GeoPoint? = null,
    /** Secondes restantes, ou null sans chrono. */
    val timeLeft: Int? = null,
) {
    val roundIndex: Int get() = if (phase == Phase.GUESSING) results.size else results.size - 1
    val location: Location get() = locations[roundIndex]
    val totalScore: Int get() = results.sumOf { it.points }
    val lastResult: RoundResult? get() = results.lastOrNull()
}

enum class SeedError { INVALID, WRONG_VERSION }

data class UiState(
    val screen: Screen = Screen.HOME,
    val list: LocationList? = null,
    val game: Game? = null,
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = Prefs(application)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    val settings: StateFlow<Settings> = prefs.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())
    val stats: StateFlow<Stats> = prefs.stats.stateIn(viewModelScope, SharingStarted.Eagerly, Stats())

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { LocationRepository.load(getApplication()) }
            _state.update { it.copy(list = list) }
        }
    }

    fun navigate(screen: Screen) {
        if (screen != Screen.GAME) stopTimer()
        _state.update { it.copy(screen = screen, game = if (screen == Screen.GAME) it.game else null) }
    }

    fun startRandom(mode: Mode) {
        val list = _state.value.list ?: return
        viewModelScope.launch { prefs.setLastMode(mode) }
        start(Seed.random(mode, list.version))
    }

    /** Lance une partie à partir d'une seed saisie ; renvoie l'erreur éventuelle. */
    fun startWithSeed(code: String): SeedError? {
        val list = _state.value.list ?: return SeedError.INVALID
        val seed = Seed.parse(code) ?: return SeedError.INVALID
        if (seed.listVersion != list.version) return SeedError.WRONG_VERSION
        start(seed)
        return null
    }

    fun replaySameSeed() = _state.value.game?.let { start(it.seed) }

    fun newGameSameMode() = _state.value.game?.let { startRandom(it.seed.mode) }

    private fun start(seed: Seed) {
        val list = _state.value.list ?: return
        val picked = pickForSeed(list.locations, seed, ROUNDS) { it.id }
        _state.update { it.copy(screen = Screen.GAME, game = Game(seed, picked)) }
        startRoundTimer()
    }

    fun setGuess(lat: Double, lon: Double) = updateGame { g ->
        if (g.phase == Phase.GUESSING) g.copy(guess = GeoPoint(lat, lon)) else g
    }

    fun submit() {
        stopTimer()
        updateGame { g ->
            if (g.phase != Phase.GUESSING) return@updateGame g
            val loc = g.location
            val distance = g.guess?.let { haversineKm(it.lat, it.lon, loc.lat, loc.lon) }
            val result = RoundResult(loc, g.guess, distance, distance?.let(::roundScore) ?: 0)
            g.copy(results = g.results + result, phase = Phase.ROUND_RESULT, guess = null, timeLeft = null)
        }
    }

    fun next() {
        val g = _state.value.game ?: return
        if (g.phase != Phase.ROUND_RESULT) return
        if (g.results.size >= g.locations.size) {
            updateGame { it.copy(phase = Phase.FINISHED) }
            viewModelScope.launch { prefs.recordGame(g.totalScore) }
        } else {
            updateGame { it.copy(phase = Phase.GUESSING) }
            startRoundTimer()
        }
    }

    fun setTimer(seconds: Int) = viewModelScope.launch { prefs.setTimer(seconds) }
    fun setUnits(units: com.satguess.data.Units) = viewModelScope.launch { prefs.setUnits(units) }

    private fun startRoundTimer() {
        stopTimer()
        timerJob = viewModelScope.launch {
            // Lu directement depuis le stockage : `settings` peut ne pas avoir encore émis au premier lancement.
            val total = prefs.settings.first().timerSeconds.takeIf { it > 0 } ?: return@launch
            for (left in total downTo 1) {
                updateGame { it.copy(timeLeft = left) }
                delay(1000)
            }
            updateGame { it.copy(timeLeft = 0) }
            submit()
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private inline fun updateGame(crossinline transform: (Game) -> Game) =
        _state.update { s -> s.game?.let { s.copy(game = transform(it)) } ?: s }
}
