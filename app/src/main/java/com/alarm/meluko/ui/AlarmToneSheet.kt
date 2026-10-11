package com.alarm.meluko.ui

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alarm.meluko.alarm.AlarmTonePlayer
import com.alarm.meluko.ui.theme.BodyFontFamily
import com.alarm.meluko.ui.theme.DefaultFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** uri == null means "system default alarm". */
data class AlarmTone(val title: String, val uri: String?)

private fun loadSystemAlarmTones(context: Context): List<AlarmTone> {
    val manager = RingtoneManager(context).apply { setType(RingtoneManager.TYPE_ALARM) }
    val tones = mutableListOf<AlarmTone>()
    manager.cursor.use { cursor ->
        while (cursor.moveToNext()) {
            val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
            val uri = manager.getRingtoneUri(cursor.position)
            if (title != null && uri != null) tones += AlarmTone(title, uri.toString())
        }
    }
    return tones
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmToneSheet(
    currentUri: String?,
    onDismiss: () -> Unit,
    onSave: (AlarmTone) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val player = remember { AlarmTonePlayer(context) }
    DisposableEffect(Unit) { onDispose { player.stop() } }

    val defaultTone = AlarmTone("Default alarm", null)
    var tones by remember { mutableStateOf<List<AlarmTone>?>(null) }
    LaunchedEffect(Unit) {
        tones = withContext(Dispatchers.IO) { listOf(defaultTone) + loadSystemAlarmTones(context) }
    }

    var selected by remember { mutableStateOf(currentUri) }
    var playingUri by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp, bottom = 24.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "ALARM TONE",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                fontFamily = DefaultFontFamily,
                letterSpacing = 3.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Text(
                text = "Tap to preview. It plays at your alarm volume.",
                fontSize = 13.sp,
                fontFamily = BodyFontFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )
            Spacer(Modifier.height(8.dp))

            val list = tones
            if (list == null) {
                Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                    LoadingIndicator(
                            modifier = Modifier.size(36.dp)
                        )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(list, key = { it.uri ?: "default" }) { tone ->
                        val isSelected = tone.uri == selected
                        val rowPlaying = isPlaying && tone.uri == playingUri
                        ToneRow(
                            tone = tone,
                            isSelected = isSelected,
                            isPlaying = rowPlaying,
                            onClick = {
                                selected = tone.uri
                                if (rowPlaying) {
                                    player.stop()
                                    isPlaying = false
                                } else {
                                    player.play(tone.uri?.let(Uri::parse), looping = true)
                                    playingUri = tone.uri
                                    isPlaying = true
                                }
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    player.stop()
                    val tone = list?.firstOrNull { it.uri == selected } ?: defaultTone
                    onSave(tone)
                },
                enabled = list != null,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .height(56.dp)
                    .pressScale(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("SAVE", fontWeight = FontWeight.Black, fontFamily = DefaultFontFamily, letterSpacing = 2.sp)
            }
        }
    }
}

@Composable
private fun ToneRow(
    tone: AlarmTone,
    isSelected: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().pressScale(pressedScale = 0.97f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                if (isPlaying) EqualizerBars(color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = tone.title,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontFamily = DefaultFontFamily,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun EqualizerBars(color: androidx.compose.ui.graphics.Color) {
    val transition = rememberInfiniteTransition(label = "eq")
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(16.dp)
    ) {
        listOf(380, 520, 300, 450).forEachIndexed { i, duration ->
            val h by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(duration, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "bar$i"
            )
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight(h)
                    .background(color, RoundedCornerShape(2.dp))
            )
        }
    }
}
