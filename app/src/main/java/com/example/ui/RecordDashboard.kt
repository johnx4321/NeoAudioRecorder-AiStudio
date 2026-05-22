package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.RecordingEntity
import java.io.File
import kotlin.math.absoluteValue

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordDashboard(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDarkTheme by viewModel.isDarkTheme

    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val amplitude by viewModel.amplitude.collectAsStateWithLifecycle()
    val recordedDurationMs by viewModel.recordedDurationMs.collectAsStateWithLifecycle()

    val recordings by viewModel.recordings.collectAsStateWithLifecycle()
    val favoriteRecordings by viewModel.favoriteRecordings.collectAsStateWithLifecycle()

    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val playPositionMs by viewModel.playPositionMs.collectAsStateWithLifecycle()
    val playDurationMs by viewModel.playDurationMs.collectAsStateWithLifecycle()
    val activeEntity by viewModel.activePlaybackEntity.collectAsStateWithLifecycle()

    val systemMessage by viewModel.messageText.collectAsStateWithLifecycle()

    // Active tab: index 0 (Record), 1 (All List), 2 (Favorites List)
    var activeTabIdx by viewModel.activeTab

    // Safe permission check
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            viewModel.startRecording()
        }
    }

    // Colors according to Selected Mode
    val primaryBg = if (isDarkTheme) NeoColors.DarkBg else NeoColors.LightBg
    val textBaseColor = if (isDarkTheme) Color.White else Color(0xFF2C3A4E)
    val textMutedColor = if (isDarkTheme) Color(0xFFBBC4D4) else Color(0xFF6F7E97)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(primaryBg),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- TOP HEADER LOGO & THEME SWITCHER ---
            Spacer(modifier = Modifier.height(16.dp))
            
            HeaderSection(
                isDarkTheme = isDarkTheme,
                onThemeToggle = { viewModel.isDarkTheme.value = !viewModel.isDarkTheme.value }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // --- TAB SWITCHER (Record vs History list vs Favorites) ---
            TabSection(
                activeTabIdx = activeTabIdx,
                onTabSelected = { activeTabIdx = it },
                isDarkTheme = isDarkTheme
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- MAIN PANES ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                this@Column.AnimatedVisibility(
                    visible = activeTabIdx == 0,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    RecordPane(
                        isRecording = isRecording,
                        recordedDurationMs = recordedDurationMs,
                        amplitude = amplitude,
                        selectedFormat = viewModel.selectedFormat.value,
                        onFormatChange = { viewModel.selectedFormat.value = it },
                        onRecordToggle = {
                            if (isRecording) {
                                viewModel.stopRecording()
                            } else {
                                if (hasMicPermission) {
                                    viewModel.startRecording()
                                } else {
                                    launcher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        isDarkTheme = isDarkTheme
                    )
                }

                this@Column.AnimatedVisibility(
                    visible = activeTabIdx == 1,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    HistoryPane(
                        recordings = recordings,
                        activeEntity = activeEntity,
                        isPlaying = isPlaying,
                        isDarkTheme = isDarkTheme,
                        onItemPlay = { viewModel.togglePlayback(it) },
                        onItemFavorite = { viewModel.toggleFavorite(it) },
                        onItemDelete = { viewModel.deleteRecording(it) }
                    )
                }

                this@Column.AnimatedVisibility(
                    visible = activeTabIdx == 2,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    HistoryPane(
                        recordings = favoriteRecordings,
                        activeEntity = activeEntity,
                        isPlaying = isPlaying,
                        isDarkTheme = isDarkTheme,
                        onItemPlay = { viewModel.togglePlayback(it) },
                        onItemFavorite = { viewModel.toggleFavorite(it) },
                        onItemDelete = { viewModel.deleteRecording(it) }
                    )
                }
            }

            // --- FLOATING FEEDBACK / SNACKBAR LEVEL MESSAGES ---
            systemMessage?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkTheme) Color(0xFF2E3342) else Color(0xFF333333)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .testTag("system_flash_card")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = msg,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss Notification",
                            tint = Color.LightGray,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { viewModel.dismissMessage() }
                        )
                    }
                }
            }

            // --- REAL-TIME PLAYBACK STICKY BAR IN BOTTOM ---
            activeEntity?.let { entity ->
                Spacer(modifier = Modifier.height(12.dp))
                StickyMediaPlayer(
                    entity = entity,
                    isPlaying = isPlaying,
                    currentPositionMs = playPositionMs,
                    durationMs = playDurationMs,
                    isDarkTheme = isDarkTheme,
                    onPlayToggle = { viewModel.togglePlayback(entity) },
                    onStop = { viewModel.stopPlayback() },
                    onSeek = { viewModel.seekPlayback(it) }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun HeaderSection(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Outset square frame for the studio icon representation
            NeoButton(
                onClick = {},
                isDarkTheme = isDarkTheme,
                isCircle = false,
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings, // Neomorphic studio micro-controller
                    contentDescription = "Studio Mic",
                    tint = NeoColors.Primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Column {
                Text(
                    text = "Audio Capture",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp,
                    color = if (isDarkTheme) Color(0xFFE0E0E0) else Color(0xFF2C3A4E)
                )
                Text(
                    text = "PROFESSIONAL STUDIO",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isDarkTheme) Color(0xFF71717A) else Color(0xFF6F7E97),
                    letterSpacing = 1.3.sp
                )
            }
        }

        // Settings config Toggle Button
        NeoButton(
            onClick = onThemeToggle,
            isDarkTheme = isDarkTheme,
            isCircle = true,
            modifier = Modifier
                .size(42.dp)
                .testTag("theme_toggle_button")
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Theme Toggle & Settings",
                tint = if (isDarkTheme) Color(0xFFE0E0E0) else Color(0xFF5A6273),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun TabSection(
    activeTabIdx: Int,
    onTabSelected: (Int) -> Unit,
    isDarkTheme: Boolean
) {
    NeoCard(
        modifier = Modifier.fillMaxWidth(),
        isDarkTheme = isDarkTheme,
        cornerRadius = 16.dp,
        elevation = 6f
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                TabItem("Record", Icons.Default.Add, 0),
                TabItem("History", Icons.Default.Menu, 1),
                TabItem("Favorited", Icons.Default.Favorite, 2)
            )

            tabs.forEach { tab ->
                val isSelected = activeTabIdx == tab.index
                val activeTintColor = when(tab.index) {
                    0 -> NeoColors.RecordRed
                    1 -> NeoColors.Primary
                    else -> Color(0xFFFF4D6D)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                ) {
                    NeoButton(
                        onClick = { onTabSelected(tab.index) },
                        modifier = Modifier.fillMaxWidth(),
                        isDarkTheme = isDarkTheme,
                        isActivated = isSelected,
                        activeColor = activeTintColor
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = if (isSelected) Color.White else {
                                    if (isDarkTheme) Color.LightGray else Color.DarkGray
                                },
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tab.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else {
                                    if (isDarkTheme) Color.LightGray else Color.DarkGray
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

data class TabItem(val label: String, val icon: ImageVector, val index: Int)

@Composable
fun RecordPane(
    isRecording: Boolean,
    recordedDurationMs: Long,
    amplitude: Float,
    selectedFormat: String,
    onFormatChange: (String) -> Unit,
    onRecordToggle: () -> Unit,
    isDarkTheme: Boolean
) {
    val textMutedColor = if (isDarkTheme) Color(0xFFBBC4D4) else Color(0xFF6F7E97)
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // --- 1. Audio Recording Format Selectors ---
        NeoFormatSelector(
            selectedFormat = selectedFormat,
            onFormatSelected = onFormatChange,
            isDarkTheme = isDarkTheme,
            modifier = Modifier.testTag("format_tabs")
        )

        Spacer(modifier = Modifier.height(28.dp))

        // --- 2. Live Adaptive Waveform Canvas Component ---
        NeoWaveVisualizer(
            amplitude = amplitude,
            isRecording = isRecording,
            isDarkTheme = isDarkTheme,
            modifier = Modifier.testTag("audio_wave_visualizer")
        )

        Spacer(modifier = Modifier.height(28.dp))

        // --- 3. Dynamic Stop watch and Activity Indicator ---
        NeoCard(
            modifier = Modifier.wrapContentWidth(),
            isDarkTheme = isDarkTheme,
            cornerRadius = 16.dp,
            elevation = 6f
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isRecording) "RECORDING IN PROGRESS..." else "RECORDER IDLE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isRecording) NeoColors.RecordRed else {
                        if (isDarkTheme) Color.LightGray else Color.DarkGray
                    },
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDuration(recordedDurationMs),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isRecording) {
                        if (isDarkTheme) Color.White else Color.Black
                    } else {
                        if (isDarkTheme) Color.Gray else Color.LightGray
                    },
                    modifier = Modifier.testTag("recording_chronometer")
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- 4. Central Massive Neomorphic Record Power Button ---
        Box(
            modifier = Modifier.size(130.dp),
            contentAlignment = Alignment.Center
        ) {
            NeoButton(
                onClick = onRecordToggle,
                isDarkTheme = isDarkTheme,
                isCircle = true,
                isActivated = isRecording,
                activeColor = NeoColors.RecordRed,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("action_record_button")
            ) {
                if (isRecording) {
                    // Standard solid neomorphic Stop icon
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                    )
                } else {
                    // Custom micro-phone icon in vector paint
                    Icon(
                        imageVector = Icons.Default.PlayArrow, // Fallback play-icon as core mic doesn't exist
                        contentDescription = "Start Recording",
                        tint = if (isDarkTheme) Color.White else Color(0xFF2C3A4E),
                        modifier = Modifier.size(46.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = if (isRecording) "TAP TO SAVE AUDIO" else "TAP BUTTON TO RECORD",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (isRecording) NeoColors.RecordRed else textMutedColor,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun HistoryPane(
    recordings: List<RecordingEntity>,
    activeEntity: RecordingEntity?,
    isPlaying: Boolean,
    isDarkTheme: Boolean,
    onItemPlay: (RecordingEntity) -> Unit,
    onItemFavorite: (RecordingEntity) -> Unit,
    onItemDelete: (RecordingEntity) -> Unit
) {
    if (recordings.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            NeoCard(
                isDarkTheme = isDarkTheme,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(30.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Empty History",
                        tint = if (isDarkTheme) Color(0xFF5D6B82) else Color(0xFFA3B1C6),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "History Chamber Empty",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) Color.White else Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Every high-fidelity recording node you encode will display cleanly right here.",
                        fontSize = 12.sp,
                        color = if (isDarkTheme) Color.LightGray else Color.Gray,
                        textAlign = Alignment.Center.toString() as? TextAlign ?: TextAlign.Center
                    )
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("recordings_lazy_list"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 120.dp, top = 4.dp)
        ) {
            items(
                items = recordings,
                key = { it.id }
            ) { recording ->
                val isCurrentPlaying = activeEntity?.id == recording.id
                
                RecordingItemCard(
                    recording = recording,
                    isCurrentPlaying = isCurrentPlaying,
                    isPlaying = isPlaying && isCurrentPlaying,
                    isDarkTheme = isDarkTheme,
                    onPlayClick = { onItemPlay(recording) },
                    onFavoriteClick = { onItemFavorite(recording) },
                    onDeleteClick = { onItemDelete(recording) }
                )
            }
        }
    }
}

@Composable
fun RecordingItemCard(
    recording: RecordingEntity,
    isCurrentPlaying: Boolean,
    isPlaying: Boolean,
    isDarkTheme: Boolean,
    onPlayClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val durationText = formatDuration(recording.durationMs)
    val sizeText = formatFileSize(recording.fileSize)
    val dateText = formatDateTime(recording.timestamp)

    NeoCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recording_item_${recording.id}"),
        isDarkTheme = isDarkTheme,
        cornerRadius = 20.dp,
        elevation = if (isCurrentPlaying) 4f else 12f
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Action Play trigger
            NeoButton(
                onClick = onPlayClick,
                isDarkTheme = isDarkTheme,
                isCircle = true,
                isActivated = isCurrentPlaying,
                activeColor = if (recording.format.uppercase() == "WAV") NeoColors.Primary else NeoColors.PrimaryGlow,
                modifier = Modifier
                    .size(54.dp)
                    .testTag("play_recording_${recording.id}")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow, // Use PlayArrow/Close for indicators
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Central info panel
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recording.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isDarkTheme) Color.White else Color(0xFF202A3C)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateText,
                    fontSize = 11.sp,
                    color = if (isDarkTheme) Color(0xFF8F9BB3) else Color(0xFF6F7E97)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Accent Format Tag Pill
                    Box(
                        modifier = Modifier
                            .background(
                                color = when (recording.format.uppercase()) {
                                    "WAV" -> NeoColors.Primary.copy(alpha = 0.15f)
                                    "AAC" -> NeoColors.PrimaryGlow.copy(alpha = 0.15f)
                                    else -> NeoColors.RecordRed.copy(alpha = 0.15f)
                                },
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = recording.format,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = when (recording.format.uppercase()) {
                                "WAV" -> NeoColors.Primary
                                "AAC" -> NeoColors.PrimaryGlow
                                else -> NeoColors.RecordRed
                            }
                        )
                    }

                    Text(
                        text = durationText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDarkTheme) Color.LightGray else Color.DarkGray
                    )

                    Text(
                        text = "•",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )

                    Text(
                        text = sizeText,
                        fontSize = 11.sp,
                        color = if (isDarkTheme) Color.LightGray else Color.DarkGray
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right Actions Side panel (Favorite/Delete)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Favorite Toggle
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("favorite_recording_${recording.id}")
                ) {
                    Icon(
                        imageVector = if (recording.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (recording.isFavorite) Color(0xFFFF5D8F) else {
                            if (isDarkTheme) Color(0xFF5A6273) else Color(0xFFA3B1C6)
                        }
                    )
                }

                // Trash Action
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("delete_recording_${recording.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Recording",
                        tint = if (isDarkTheme) Color(0xFF6F7E97) else Color(0xFFA3B1C6)
                    )
                }
            }
        }
    }
}

// --- Dynamic seeking and playback sheet console in base ---
@Composable
fun StickyMediaPlayer(
    entity: RecordingEntity,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    isDarkTheme: Boolean,
    onPlayToggle: () -> Unit,
    onStop: () -> Unit,
    onSeek: (Long) -> Unit
) {
    val displayPosition = formatDuration(currentPositionMs)
    val displayTotal = formatDuration(durationMs.coerceAtLeast(entity.durationMs))
    
    val sliderValue = if (durationMs > 0) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    NeoCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_sticky_player"),
        isDarkTheme = isDarkTheme,
        cornerRadius = 24.dp,
        elevation = 16f
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Playing icon indicator
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(NeoColors.Primary, NeoColors.PrimaryGlow)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Now Playing",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entity.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isDarkTheme) Color.White else Color(0xFF202A3C)
                    )
                    Text(
                        text = "Streaming playback...",
                        fontSize = 11.sp,
                        color = if (isDarkTheme) Color(0xFF8F9BB3) else Color(0xFF6F7E97)
                    )
                }

                // Micro neomorphic play control
                NeoButton(
                    onClick = onPlayToggle,
                    isDarkTheme = isDarkTheme,
                    isCircle = true,
                    isActivated = isPlaying,
                    activeColor = NeoColors.Primary,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow, // toggle shape
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Micro stops controls
                NeoButton(
                    onClick = onStop,
                    isDarkTheme = isDarkTheme,
                    isCircle = true,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isDarkTheme) Color.White else Color.Black)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dynamic progress slider scrubbers
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayPosition,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) Color.LightGray else Color.DarkGray,
                    modifier = Modifier.width(40.dp)
                )

                // Slider tracking
                Slider(
                    value = sliderValue,
                    onValueChange = { percent ->
                        val target = (percent * durationMs).toLong()
                        onSeek(target)
                    },
                    colors = SliderDefaults.colors(
                        activeTrackColor = NeoColors.Primary,
                        inactiveTrackColor = if (isDarkTheme) Color(0xFF333A4A) else Color(0xFFCBD2DC),
                        thumbColor = NeoColors.PrimaryGlow
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                        .testTag("playback_progress_slider")
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = displayTotal,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) Color.LightGray else Color.DarkGray,
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

// Format logic helpers
fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%02d:%02d", min, sec)
}

fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024f
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024f
    return String.format("%.1f MB", mb)
}

fun formatDateTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM dd, yyyy • hh:mm a", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
