package com.satguess.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.satguess.game.Mode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class Units { KM, MI }

data class Settings(
    /** Durée d'une manche en secondes ; 0 = pas de chrono. */
    val timerSeconds: Int = 0,
    val units: Units = Units.KM,
    val lastMode: Mode = Mode.MOVE,
)

data class Stats(val games: Int = 0, val best: Int = 0, val total: Long = 0) {
    val average: Int get() = if (games == 0) 0 else (total / games).toInt()
}

private val Context.dataStore by preferencesDataStore(name = "satguess")

class Prefs(private val context: Context) {
    private object Keys {
        val timer = intPreferencesKey("timer_seconds")
        val units = stringPreferencesKey("units")
        val lastMode = stringPreferencesKey("last_mode")
        val games = intPreferencesKey("games")
        val best = intPreferencesKey("best")
        val total = longPreferencesKey("total")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            timerSeconds = p[Keys.timer] ?: 0,
            units = p[Keys.units]?.let { runCatching { Units.valueOf(it) }.getOrNull() } ?: Units.KM,
            lastMode = p[Keys.lastMode]?.let { runCatching { Mode.valueOf(it) }.getOrNull() } ?: Mode.MOVE,
        )
    }

    val stats: Flow<Stats> = context.dataStore.data.map { p ->
        Stats(games = p[Keys.games] ?: 0, best = p[Keys.best] ?: 0, total = p[Keys.total] ?: 0)
    }

    suspend fun setTimer(seconds: Int) = context.dataStore.edit { it[Keys.timer] = seconds }
    suspend fun setUnits(units: Units) = context.dataStore.edit { it[Keys.units] = units.name }
    suspend fun setLastMode(mode: Mode) = context.dataStore.edit { it[Keys.lastMode] = mode.name }

    suspend fun recordGame(score: Int) = context.dataStore.edit {
        it[Keys.games] = (it[Keys.games] ?: 0) + 1
        it[Keys.best] = maxOf(it[Keys.best] ?: 0, score)
        it[Keys.total] = (it[Keys.total] ?: 0) + score
    }
}
