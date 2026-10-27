package com.satguess.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.satguess.R
import com.satguess.data.Units
import com.satguess.game.Game
import com.satguess.game.GeoPoint
import com.satguess.game.MAX_ROUND_SCORE
import com.satguess.game.Mode
import com.satguess.game.RoundResult
import com.satguess.map.GUESS_STYLE_URL
import com.satguess.map.MapLibreView
import com.satguess.map.fitTo
import com.satguess.map.showResults
import org.maplibre.android.maps.Style

@Composable
private fun ResultsMap(results: List<RoundResult>, modifier: Modifier) {
    MapLibreView(Style.Builder().fromUri(GUESS_STYLE_URL), modifier) { h ->
        h.map.uiSettings.apply {
            isRotateGesturesEnabled = false
            isTiltGesturesEnabled = false
            isCompassEnabled = false
        }
        h.style.showResults(results)
        val points = results.flatMap { r -> listOfNotNull(r.guess, GeoPoint(r.location.lat, r.location.lon)) }
        val padding = (48 * h.view.resources.displayMetrics.density).toInt()
        h.view.post { h.map.fitTo(points, padding) }
    }
}

/** Nom du lieu, courte description et pays : ce que le joueur découvre après sa réponse. */
@Composable
private fun PlaceInfo(result: RoundResult) {
    val loc = result.location
    Text(loc.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    loc.description?.let {
        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    loc.countryName?.let {
        Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun RoundResultScreen(game: Game, units: Units, onNext: () -> Unit) {
    val result = game.lastResult ?: return
    val last = game.results.size >= game.locations.size
    Column(Modifier.fillMaxSize().background(Night)) {
        ResultsMap(listOf(result), Modifier.fillMaxWidth().weight(1f))
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp)) {
                Text(
                    stringResource(R.string.round_x, game.results.size, game.locations.size) + " · " +
                        stringResource(R.string.it_was),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PlaceInfo(result)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        result.distanceKm?.let { stringResource(R.string.distance_away, formatDistance(it, units)) }
                            ?: stringResource(R.string.no_guess),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(R.string.points_x, formatScore(result.points)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                LinearProgressIndicator(
                    progress = { result.points / MAX_ROUND_SCORE.toFloat() },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                )
                Text(
                    stringResource(R.string.score_x, formatScore(game.totalScore)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text(stringResource(if (last) R.string.see_results else R.string.next_round))
                }
            }
        }
    }
}

@Composable
fun FinalScreen(
    game: Game,
    units: Units,
    onNewGame: () -> Unit,
    onReplaySeed: () -> Unit,
    onHome: () -> Unit,
) {
    val context = LocalContext.current
    val maxTotal = MAX_ROUND_SCORE * game.locations.size
    val seed = game.seed.encode()
    val modeName = stringResource(if (game.seed.mode == Mode.MOVE) R.string.mode_move else R.string.mode_no_move)
    val shareText = stringResource(R.string.share_text, formatScore(game.totalScore), formatScore(maxTotal), modeName, seed)
    val copiedText = stringResource(R.string.seed_copied)

    Column(Modifier.fillMaxSize().background(Night)) {
        ResultsMap(game.results, Modifier.fillMaxWidth().weight(0.4f))
        LazyColumn(
            Modifier.fillMaxWidth().weight(0.6f).background(MaterialTheme.colorScheme.surface),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        ) {
            item {
                Text(stringResource(R.string.game_over), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    stringResource(R.string.total_of, formatScore(game.totalScore), formatScore(maxTotal)),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${stringResource(R.string.seed_x, seed)} · $modeName", style = MaterialTheme.typography.bodyMedium)
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("SatGuess seed", seed))
                        Toast.makeText(context, copiedText, Toast.LENGTH_SHORT).show()
                    }) { Icon(Icons.Default.ContentCopy, stringResource(R.string.copy_seed), Modifier.size(20.dp)) }
                }
                Spacer(Modifier.height(8.dp))
            }
            itemsIndexed(game.results) { i, r ->
                HorizontalDivider()
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) { Text("${i + 1}", fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.location.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        r.location.description?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            r.distanceKm?.let { formatDistance(it, units) } ?: stringResource(R.string.no_guess),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(formatScore(r.points), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            item {
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))
                Button(onClick = onNewGame, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text(stringResource(R.string.new_game))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onReplaySeed, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.replay_seed), maxLines = 1)
                    }
                    OutlinedButton(onClick = {
                        val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText)
                        context.startActivity(Intent.createChooser(intent, null))
                    }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                        Text(stringResource(R.string.share), Modifier.padding(start = 6.dp))
                    }
                }
                TextButton(onClick = onHome, modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
                    Text(stringResource(R.string.home))
                }
            }
        }
    }
}
