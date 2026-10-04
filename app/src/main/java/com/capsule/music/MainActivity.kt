package com.capsule.music

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.capsule.music.data.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Запрос разрешения на уведомления для Android 13+
            val context = LocalContext.current
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = {}
            )

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            var showCapsuleStories by remember { mutableStateOf(false) }

            if (showCapsuleStories) {
                CapsuleStoriesScreen(onClose = { showCapsuleStories = false })
            } else {
                MainDashboardScreen(onOpenCapsule = { showCapsuleStories = true })
            }
        }
    }
}

// -------------------------------------------------------------
// ГЛАВНЫЙ ЭКРАН (DASHBOARD)
// -------------------------------------------------------------
@Composable
fun MainDashboardScreen(onOpenCapsule: () -> Unit) {
    val context = LocalContext.current
    val dao = CapsuleApp.database.playbackDao()
    val scope = rememberCoroutineScope()
    val history by dao.getAllEvents().collectAsState(initial = emptyList())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F081D), Color(0xFF040407))
                )
            )
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(44.dp))

            // Шапка со стриком
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Music Capsule", fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text("Локальный трекер FLAC", fontSize = 12.sp, color = Color.Gray)
                }

                // Плашка стрика дней
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF2A153A),
                    modifier = Modifier.padding(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔥", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (history.isNotEmpty()) "1 день" else "0 дней",
                            color = Color(0xFFFF9800),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // БОЛЬШАЯ КАРТОЧКА SPOTIFY WRAPPED
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121))
                        )
                    )
                    .clickable { onOpenCapsule() }
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("ИТОГИ МЕСЯЦА", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(0.8f))
                        Text("СМОТРЕТЬ СТОРИС ➔", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Твоя Капсула готова ✨",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "Топ артисты со стриками, треки и график прослушиваний",
                        fontSize = 13.sp,
                        color = Color.White.copy(0.9f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Кнопки управления
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).apply {
                            putExtra(
                                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                                "${context.packageName}/${com.capsule.music.tracker.MusicNotificationListener::class.java.name}"
                            )
                        })
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Доступ к плееру", fontSize = 12.sp, color = Color.White)
                }

                Button(
                    onClick = {
                        scope.launch {
                            insertDemoData(dao)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF281C40)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Залить демо-топ 🧪", fontSize = 12.sp, color = Color(0xFF1DB954))
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text("Недавние треки", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))

            // Живая лента
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(history) { event ->
                    TrackRow(event)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// ЭКРАН ИТОГОВ: SPOTIFY WRAPPED STORIES
// -------------------------------------------------------------
@Composable
fun CapsuleStoriesScreen(onClose: () -> Unit) {
    BackHandler { onClose() }

    val dao = CapsuleApp.database.playbackDao()
    var currentSlide by remember { mutableIntStateOf(0) }
    val totalSlides = 4

    var topTracks by remember { mutableStateOf<List<TrackStat>>(emptyList()) }
    var topArtists by remember { mutableStateOf<List<ArtistStat>>(emptyList()) }
    var dailyStats by remember { mutableStateOf<List<DayStat>>(emptyList()) }
    var totalMonthMinutes by remember { mutableLongStateOf(0L) }
    var artistStreakMap by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    LaunchedEffect(Unit) {
        val monthAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val twoMonthsAgo = System.currentTimeMillis() - (60L * 24 * 60 * 60 * 1000)

        val tracks = dao.getTopTracks(monthAgo)
        val artists = dao.getTopArtists(monthAgo)
        val days = dao.getDailyStats(monthAgo)
        val totalMs = dao.getMinutesSince(monthAgo)

        topTracks = tracks
        topArtists = artists
        dailyStats = days
        totalMonthMinutes = totalMs / 60000

        val streaks = mutableMapOf<String, Int>()
        for (a in artists) {
            val wasInPrev = dao.isArtistInPrevMonthTop(a.artistName, twoMonthsAgo, monthAgo)
            streaks[a.artistName] = if (wasInPrev > 0) 2 else 1
        }
        artistStreakMap = streaks
    }

    val progress = remember { Animatable(0f) }

    LaunchedEffect(currentSlide) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 6500, easing = LinearEasing)
        )
        if (currentSlide < totalSlides - 1) {
            currentSlide++
        } else {
            onClose()
        }
    }

    val bgGradients = listOf(
        listOf(Color(0xFF4A0E4E), Color(0xFF0F0014)),
        listOf(Color(0xFF0B3C5D), Color(0xFF021019)),
        listOf(Color(0xFFB82601), Color(0xFF1E0000)),
        listOf(Color(0xFF0D5C3A), Color(0xFF021B10))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(bgGradients[currentSlide]))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (currentSlide < totalSlides - 1) currentSlide++ else onClose()
            }
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(28.dp))

            // Полоски сторис
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (i in 0 until totalSlides) {
                    val barFill = when {
                        i < currentSlide -> 1f
                        i == currentSlide -> progress.value
                        else -> 0f
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(0.25f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(barFill)
                                .background(Color.White)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "КАПСУЛА МЕСЯЦА",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(0.6f)
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            when (currentSlide) {
                0 -> SlideTotalTime(totalMonthMinutes, dailyStats)
                1 -> SlideTopTracks(topTracks)
                2 -> SlideTopArtists(topArtists, artistStreakMap)
                3 -> SlideSummaryCard(totalMonthMinutes, topArtists.firstOrNull()?.artistName ?: "—", topTracks.firstOrNull()?.trackTitle ?: "—")
            }
        }
    }
}

// -------------------------------------------------------------
// СЛАЙДЫ
// -------------------------------------------------------------
@Composable
fun SlideTotalTime(totalMinutes: Long, dailyStats: List<DayStat>) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("В этом месяце ты провел в музыке:", fontSize = 20.sp, color = Color.White.copy(0.8f))
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "$totalMinutes МИНУТ",
            fontSize = 46.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFFFD700)
        )
        Text("Это примерно ${(totalMinutes / 60)} ч. чистого звука", fontSize = 15.sp, color = Color.LightGray)

        Spacer(modifier = Modifier.height(40.dp))
        Text("Активность по дням:", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))

        if (dailyStats.isNotEmpty()) {
            val maxMs = (dailyStats.maxOfOrNull { it.totalMs } ?: 1L).coerceAtLeast(1L)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                dailyStats.takeLast(7).forEach { stat ->
                    val fraction = (stat.totalMs.toFloat() / maxMs).coerceIn(0.08f, 1f)
                    val dayMinutes = stat.totalMs / 60000
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stat.day.takeLast(5), fontSize = 12.sp, color = Color.Gray, modifier = Modifier.width(50.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(18.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(0.1f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction)
                                    .background(Color(0xFFE94057))
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("$dayMinutes м", fontSize = 12.sp, color = Color.White, modifier = Modifier.width(42.dp))
                    }
                }
            }
        } else {
            Text("Слушай больше треков, чтобы увидеть график!", color = Color.Gray, fontSize = 14.sp)
        }
    }
}

@Composable
fun SlideTopTracks(tracks: List<TrackStat>) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Треки на повторе", fontSize = 16.sp, color = Color.White.copy(0.7f))
        Text("ТВОЙ ТОП-5", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color.White)
        Spacer(modifier = Modifier.height(24.dp))

        if (tracks.isEmpty()) {
            Text("Пока нет данных для топа", color = Color.Gray)
        } else {
            tracks.forEachIndexed { index, track ->
                val minutes = track.totalMs / 60000
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                ) {
                    Text(
                        text = "${index + 1}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = if (index == 0) Color(0xFFFFD700) else Color.White.copy(0.5f),
                        modifier = Modifier.width(36.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(track.trackTitle, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                        Text(track.artistName, fontSize = 13.sp, color = Color.LightGray, maxLines = 1)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${track.playCount} раз", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1DB954))
                        Text("$minutes мин", fontSize = 11.sp, color = Color.Gray)
                    }
                }
                HorizontalDivider(color = Color.White.copy(0.08f))
            }
        }
    }
}

@Composable
fun SlideTopArtists(artists: List<ArtistStat>, streaks: Map<String, Int>) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Главные герои месяца", fontSize = 16.sp, color = Color.White.copy(0.7f))
        Text("ТОП АРТИСТОВ", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color.White)
        Spacer(modifier = Modifier.height(24.dp))

        if (artists.isEmpty()) {
            Text("Пока нет данных для топа", color = Color.Gray)
        } else {
            artists.forEachIndexed { index, artist ->
                val streak = streaks[artist.artistName] ?: 1
                val minutes = artist.totalMs / 60000
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (index == 0) Color(0xFFFF9800) else Color.White.copy(0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(artist.artistName, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (streak > 1) "🔥 $streak мес. в топе подряд!" else "✨ Новый в топе",
                                fontSize = 11.sp,
                                color = if (streak > 1) Color(0xFFFF9800) else Color(0xFF00E5FF),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Text("$minutes мин", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                }
                HorizontalDivider(color = Color.White.copy(0.08f))
            }
        }
    }
}

@Composable
fun SlideSummaryCard(minutes: Long, topArtist: String, topTrack: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF142419)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎵 МОЯ КАПСУЛА 2026", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF1DB954))
                Spacer(modifier = Modifier.height(18.dp))

                Text("Всего времени:", fontSize = 13.sp, color = Color.Gray)
                Text("$minutes МИНУТ", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.White)

                Spacer(modifier = Modifier.height(16.dp))

                Text("Главный артист месяца:", fontSize = 13.sp, color = Color.Gray)
                Text(topArtist, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))

                Spacer(modifier = Modifier.height(16.dp))

                Text("Главный трек:", fontSize = 13.sp, color = Color.Gray)
                Text(topTrack, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)

                Spacer(modifier = Modifier.height(24.dp))
                Text("Слушаю в Lossless FLAC 🎧", fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun TrackRow(event: PlaybackEvent) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171322)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color(0xFF1DB954))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(event.trackTitle, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
                Text(event.artistName, color = Color.Gray, fontSize = 12.sp, maxLines = 1)
            }
            Text(timeFormat.format(Date(event.timestamp)), color = Color.DarkGray, fontSize = 11.sp)
        }
    }
}

suspend fun insertDemoData(dao: PlaybackDao) {
    val now = System.currentTimeMillis()
    val dayMs = 24L * 60 * 60 * 1000
    val demoList = listOf(
        PlaybackEvent(trackTitle = "イドラのサーカス", artistName = "Neru", durationMs = 210000, timestamp = now),
        PlaybackEvent(trackTitle = "ロストワンの号哭", artistName = "Neru", durationMs = 215000, timestamp = now - 100000),
        PlaybackEvent(trackTitle = "Bug", artistName = "Kairiki Bear", durationMs = 185000, timestamp = now - dayMs),
        PlaybackEvent(trackTitle = "Darling Dance", artistName = "Kairiki Bear", durationMs = 205000, timestamp = now - dayMs),
        PlaybackEvent(trackTitle = "Vampire", artistName = "DECO*27", durationMs = 180000, timestamp = now - (dayMs * 2)),
        PlaybackEvent(trackTitle = "Rabbit Hole", artistName = "DECO*27", durationMs = 190000, timestamp = now - (dayMs * 3)),
        PlaybackEvent(trackTitle = "Magical Doctor", artistName = "MARETU", durationMs = 240000, timestamp = now - (dayMs * 4)),
        PlaybackEvent(trackTitle = "Mind Brand", artistName = "MARETU", durationMs = 250000, timestamp = now - (dayMs * 5)),
        PlaybackEvent(trackTitle = "Lagtrain", artistName = "inabakumori", durationMs = 260000, timestamp = now - (dayMs * 6))
    )
    dao.insertAll(demoList)
}