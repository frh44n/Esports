package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Tournament
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack

// --- Synthetic Sound Generator for Ludo Classic Game ---
object LudoSoundPlayer {
    fun playTone(frequency: Double, durationMs: Int, volume: Float = 0.5f) {
        Thread {
            try {
                val sampleRate = 8000
                val numSamples = (durationMs * sampleRate / 1000)
                val sample = DoubleArray(numSamples)
                val generatedSnd = ByteArray(2 * numSamples)

                for (i in 0 until numSamples) {
                    sample[i] = Math.sin(2 * Math.PI * i / (sampleRate / frequency))
                }

                var idx = 0
                for (dVal in sample) {
                    val valShort = (dVal * 32767).toInt().toShort()
                    generatedSnd[idx++] = (valShort.toInt() and 0x00ff).toByte()
                    generatedSnd[idx++] = ((valShort.toInt() and 0xff00) ushr 8).toByte()
                }

                val audioTrack = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(sampleRate)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(generatedSnd.size)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        generatedSnd.size,
                        AudioTrack.MODE_STATIC
                    )
                }

                audioTrack.write(generatedSnd, 0, generatedSnd.size)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    audioTrack.setVolume(volume)
                } else {
                    @Suppress("DEPRECATION")
                    audioTrack.setStereoVolume(volume, volume)
                }
                audioTrack.play()
                Thread.sleep(durationMs.toLong() + 30)
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}

object LudoSoundEffects {
    // 1. Pawn Move Sound
    fun playMove() {
        LudoSoundPlayer.playTone(587.33, 80, 0.4f) // D5 note, short & punchy
    }

    // 2. Capture Sound
    fun playCapture() {
        Thread {
            LudoSoundPlayer.playTone(440.0, 100, 0.5f) // A4
            Thread.sleep(110)
            LudoSoundPlayer.playTone(220.0, 220, 0.6f) // A3 (deep downward explosion)
        }.start()
    }

    // 3. Entering the Center / Finished
    fun playFinish() {
        Thread {
            val notes = listOf(523.25, 659.25, 784.0, 1046.50) // C5, E5, G5, C6 arpeggio
            for (note in notes) {
                LudoSoundPlayer.playTone(note, 120, 0.5f)
                Thread.sleep(130)
            }
        }.start()
    }

    // 4. Dice Rolling Click
    fun playRollClick() {
        LudoSoundPlayer.playTone(987.77, 25, 0.15f) // B5 note, very short and soft clicks
    }
}

// Visual Ludo Colors matching the screenshot
val LudoRed = Color(0xFFE53935)
val LudoGreen = Color(0xFF2E7D32)
val LudoBlue = Color(0xFF1E88E5)
val LudoYellow = Color(0xFFFBC02D)
val LudoTrackGray = Color(0xFFECEFF1)
val LudoBorder = Color(0xFF37474F)

// Coordinate mapping for the 52 clockwise active cells
val TrackCoordinates = listOf(
    // Red quadrant track (Left wing, upper row, left to right)
    Pair(1, 6), Pair(2, 6), Pair(3, 6), Pair(4, 6), Pair(5, 6),
    // Green quadrant track (Top wing, left column, bottom to top)
    Pair(6, 5), Pair(6, 4), Pair(6, 3), Pair(6, 2), Pair(6, 1), Pair(6, 0),
    // Top apex (Green entry corner)
    Pair(7, 0),
    // Green quadrant track (Top wing, right column, top to bottom)
    Pair(8, 0), Pair(8, 1), Pair(8, 2), Pair(8, 3), Pair(8, 4), Pair(8, 5),
    // Yellow quadrant track (Right wing, upper row, left to right)
    Pair(9, 6), Pair(10, 6), Pair(11, 6), Pair(12, 6), Pair(13, 6), Pair(14, 6),
    // Right apex (Yellow entry corner)
    Pair(14, 7),
    // Yellow quadrant track (Right wing, lower row, right to left)
    Pair(14, 8), Pair(13, 8), Pair(12, 8), Pair(11, 8), Pair(10, 8), Pair(9, 8),
    // Blue quadrant track (Bottom wing, right column, top to bottom)
    Pair(8, 9), Pair(8, 10), Pair(8, 11), Pair(8, 12), Pair(8, 13), Pair(8, 14),
    // Bottom apex (Blue entry corner)
    Pair(7, 14),
    // Blue quadrant track (Bottom wing, left column, bottom to top)
    Pair(6, 14), Pair(6, 13), Pair(6, 12), Pair(6, 11), Pair(6, 10), Pair(6, 9),
    // Red quadrant track (Left wing, lower row, right to left)
    Pair(5, 8), Pair(4, 8), Pair(3, 8), Pair(2, 8), Pair(1, 8), Pair(0, 8),
    // Left apex (Red entry corner)
    Pair(0, 7),
    // Left wing, upper row corner
    Pair(0, 6)
)

data class LudoPawn(
    val id: Int,
    val player: Int, // 1: Player 1 (Blue/User), 2: Player 2 (Green/Bot)
    var position: Int = -1, // -1: In Yard, 0..50: Active track step index, 51..55: Home path index, 56: Finished
    val yardXOffset: Float, // fractional Offset inside base yard
    val yardYOffset: Float
)

@Composable
fun LudoTournamentsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val tournaments by viewModel.allTournaments.collectAsStateWithLifecycle()
    val user by viewModel.loggedInUser.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    var selectedTournamentForPlay by remember { mutableStateOf<Tournament?>(null) }
    var selectedTournamentForReg by remember { mutableStateOf<Tournament?>(null) }
    var selectedTournamentForInfo by remember { mutableStateOf<Tournament?>(null) }

    var matchingTournament by remember { mutableStateOf<Tournament?>(null) }
    var matchingTimer by remember { mutableStateOf(10) }
    var activeOpponentName by remember { mutableStateOf("Opponent") }
    

    // Filter only Ludo tournaments
    val ludoTours = remember(tournaments) {
        tournaments.filter {
            it.game.equals("Ludo", ignoreCase = true) ||
                    it.game.lowercase().contains("ludo")
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshOnlineData()
    }

    if (matchingTournament != null) {
        LudoMatchmakingScreen(
            viewModel = viewModel,
            tournament = matchingTournament!!,
            onMatchFound = { opponentName ->
                activeOpponentName = opponentName
                selectedTournamentForPlay = matchingTournament
                matchingTournament = null
            },
            onCancel = {
                matchingTournament = null
            }
        )
        return
    }

    if (selectedTournamentForPlay != null) {
        LudoGameManager(
            viewModel = viewModel,
            tournament = selectedTournamentForPlay!!,
            opponentName = activeOpponentName,
            onBack = { selectedTournamentForPlay = null }
        )
        return
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBg)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LUDO CLASSIC TOURNAMENTS",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBg)
        ) {
            if (isRefreshing && ludoTours.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = PurpleGlow
                )
            } else if (ludoTours.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = "No Games",
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Ludo Tournaments Active",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Admin will list new 1v1 and 2v2 Ludo classic tournaments shortly! Tap refresh below to check.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.refreshOnlineData() },
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleGlow)
                    ) {
                        Text("Refresh Feed")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardBg),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, PurpleGlow.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(PurpleGlow.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Casino, contentDescription = "Ludo Rules", tint = PurpleGlow, modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Point Battle League", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                                    Text("15 Minute Timed match. 1 point/cell moved. Capture: +50. Win: +100.", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    items(ludoTours) { tour ->
                        LudoTournamentCard(
                            tournament = tour,
                            userPhone = user?.whatsappNumber ?: "",
                            isAdmin = user?.isAdmin == true,
                            onRegister = { selectedTournamentForReg = tour },
                            onPlay = { 
                                if (tour.isJoined) {
                                    matchingTournament = tour
                                } else {
                                    viewModel.registerTournamentWithTeam(tour.id, "", "") { success ->
                                        if (success) {
                                            matchingTournament = tour
                                        }
                                    }
                                }
                            },
                            onInfo = { selectedTournamentForInfo = tour },
                            onStartTour = { viewModel.adminStartTournament(tour.id) },
                            onFinishTour = { viewModel.adminFinishTournament(tour.id) }
                        )
                    }
                }
            }
        }
    }

    // --- Matching Dialog ---

    // --- Dynamic Registration Dialog ---
    if (selectedTournamentForReg != null) {
        val tour = selectedTournamentForReg!!
        val isDuo = tour.rules.lowercase().contains("2v2") || tour.title.lowercase().contains("2v2")
        val modeText = if (isDuo) "2v2 Team Match" else "1v1 Solo Match"

        Dialog(onDismissRequest = { selectedTournamentForReg = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, PurpleGlow, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Ludo Tournament Entry",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = modeText,
                        fontSize = 12.sp,
                        color = PurpleGlow,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tour.title,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "Entry Fee: ₹${tour.entryFee} • Prize Pool: ₹${tour.prizePool}",
                        fontSize = 12.sp,
                        color = EmeraldGlow,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    var teamName by remember { mutableStateOf("") }
                    var player1 by remember { mutableStateOf("") }
                    var player2 by remember { mutableStateOf("") }

                    if (isDuo) {
                        OutlinedTextField(
                            value = teamName,
                            onValueChange = { teamName = it },
                            label = { Text("Team Name", color = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = PurpleGlow, unfocusedBorderColor = BorderColor),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                    }

                    OutlinedTextField(
                        value = player1,
                        onValueChange = { player1 = it },
                        label = { Text("Your Player Name", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = PurpleGlow, unfocusedBorderColor = BorderColor),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    if (isDuo) {
                        OutlinedTextField(
                            value = player2,
                            onValueChange = { player2 = it },
                            label = { Text("Partner Player Name", color = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = PurpleGlow, unfocusedBorderColor = BorderColor),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { selectedTournamentForReg = null },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, BorderColor),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                val isInvalid = if (isDuo) {
                                    teamName.isBlank() || player1.isBlank() || player2.isBlank()
                                } else {
                                    player1.isBlank()
                                }

                                if (isInvalid) {
                                    viewModel.showToast("Please fill in all player details!")
                                } else {
                                    val finalTeam = if (isDuo) teamName.trim() else "Ludo Solo"
                                    val finalMembers = if (isDuo) "${player1.trim()}, ${player2.trim()}" else player1.trim()
                                    viewModel.registerTournamentWithTeam(tour.id, finalTeam, finalMembers) { success ->
                                        if (success) {
                                            selectedTournamentForReg = null
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleGlow),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Pay & Join")
                        }
                    }
                }
            }
        }
    }

    // --- Details Dialog ---
    if (selectedTournamentForInfo != null) {
        val tour = selectedTournamentForInfo!!
        Dialog(onDismissRequest = { selectedTournamentForInfo = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = tour.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = BorderColor)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Prize Pool Distribution", fontWeight = FontWeight.Bold, color = PurpleGlow, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("1st Place (Winner)", color = Color.Gray, fontSize = 12.sp)
                        Text("₹${tour.prize1st}", color = EmeraldGlow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    if (tour.prize2nd > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("2nd Place", color = Color.Gray, fontSize = 12.sp)
                            Text("₹${tour.prize2nd}", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Tournament Rules", fontWeight = FontWeight.Bold, color = PurpleGlow, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tour.rules.ifBlank { "1. 1v1 Classic Points System.\n2. Timed 15-minute game.\n3. Turn shifts dynamically.\n4. Decisions of automated referees are final." },
                        fontSize = 12.sp,
                        color = Color.Gray,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { selectedTournamentForInfo = null },
                        colors = ButtonDefaults.buttonColors(containerColor = CardBg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                    ) {
                        Text("Close", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun LudoTournamentCard(
    tournament: Tournament,
    userPhone: String,
    isAdmin: Boolean,
    onRegister: () -> Unit,
    onPlay: () -> Unit,
    onInfo: () -> Unit,
    onStartTour: () -> Unit,
    onFinishTour: () -> Unit
) {
    // Determine slots
    val isDuo = tournament.rules.lowercase().contains("2v2") || tournament.title.lowercase().contains("2v2")
    val maxSlots = if (isDuo) 8 else 16 // 8 teams or 16 players
    // Fetch simulated/real registrations
    val minJoined = if (isDuo) 1 else 1 // Always at least 1 player joined
    val regCount = if (tournament.isJoined) minJoined + 1 else minJoined
    val spotsLeft = maxSlots - regCount
    val progress = regCount.toFloat() / maxSlots.toFloat()

    val isStarted = tournament.startTime.contains("[STARTED]")
    val isFinished = tournament.startTime.contains("[FINISHED]")

    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (tournament.isJoined) PurpleGlow else BorderColor, RoundedCornerShape(14.dp))
    ) {
        Column {
            if (tournament.posterRes.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                ) {
                    coil.compose.AsyncImage(
                        model = tournament.posterRes,
                        contentDescription = "Tournament Poster",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                // Header Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(if (isDuo) PurpleGlow.copy(alpha = 0.15f) else CyanGlow.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isDuo) "2v2 DUO" else "1v1 SOLO",
                        color = if (isDuo) PurpleGlow else CyanGlow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                // Live status
                Text(
                    text = when {
                        isFinished -> "FINISHED"
                        isStarted -> "LIVE NOW"
                        else -> "UPCOMING"
                    },
                    color = when {
                        isFinished -> Color.Gray
                        isStarted -> EmeraldGlow
                        else -> AmberGlow
                    },
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = tournament.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Prize Pool: ₹${tournament.prizePool}", color = EmeraldGlow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Entry Fee: ₹${tournament.entryFee}", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Slots Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Slots Filled", color = Color.Gray, fontSize = 11.sp)
                Text("$regCount / $maxSlots ($spotsLeft left)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (tournament.isJoined) PurpleGlow else CyanGlow,
                trackColor = BorderColor
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Details button
                OutlinedButton(
                    onClick = onInfo,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Details", fontSize = 12.sp)
                }

                // Action Button (Play)
                if (isFinished) {
                    Button(
                        onClick = {},
                        enabled = false,
                        colors = ButtonDefaults.buttonColors(containerColor = BorderColor),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("Ended", fontSize = 12.sp, color = Color.Gray)
                    }
                } else {
                    Button(
                        onClick = onPlay,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text(if (tournament.isJoined) "PLAY NOW" else "PLAY (₹${tournament.entryFee})", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Admin Actions
            if (isAdmin) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = BorderColor)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onStartTour,
                        enabled = !isStarted && !isFinished,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Start Tour", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onFinishTour,
                        enabled = isStarted,
                        colors = ButtonDefaults.buttonColors(containerColor = RedGlow),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Finish Tour", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
}

@Composable
fun LudoGameManager(
    viewModel: MainViewModel,
    tournament: Tournament,
    opponentName: String = "Opponent",
    onBack: () -> Unit
) {
    var gameState by remember { mutableStateOf("playing") } // "playing", "finished"
    var finalScore by remember { mutableIntStateOf(0) }
    var userWonMatch by remember { mutableStateOf(false) }

    when (gameState) {
        "playing" -> {
            LudoGamePlayScreen(
                viewModel = viewModel,
                tournament = tournament,
                opponentName = opponentName,
                onGameFinished = { score, isWinner ->
                    finalScore = score
                    userWonMatch = isWinner
                    gameState = "finished"
                }
            )
        }
        "finished" -> {
            LudoResultScreen(
                viewModel = viewModel,
                tournament = tournament,
                score = finalScore,
                isWinner = userWonMatch,
                onClose = onBack
            )
        }
    }
}

@Composable
fun LudoGamePlayScreen(
    viewModel: MainViewModel,
    tournament: Tournament,
    opponentName: String = "Opponent",
    onGameFinished: (Int, Boolean) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var matchSecondsLeft by remember { mutableIntStateOf(900) } // 15 minutes limit


    // Real-time points state
    var p1Points by remember { mutableIntStateOf(0) }
    var p2Points by remember { mutableIntStateOf(0) }

    // Pawns state (4 for User Blue, 4 for Bot Green)
    // Yard Offset fractions coordinates inside the bases
    val pawns = remember {
        mutableStateListOf(
            // Blue Pawns (Player 1) - start at Bottom Left base
            LudoPawn(id = 0, player = 1, position = -1, yardXOffset = 0.25f, yardYOffset = 0.25f),
            LudoPawn(id = 1, player = 1, position = -1, yardXOffset = 0.25f, yardYOffset = 0.55f),
            LudoPawn(id = 2, player = 1, position = -1, yardXOffset = 0.55f, yardYOffset = 0.25f),
            LudoPawn(id = 3, player = 1, position = -1, yardXOffset = 0.55f, yardYOffset = 0.55f),

            // Green Pawns (Player 2) - start at Top Right base
            LudoPawn(id = 4, player = 2, position = -1, yardXOffset = 0.25f, yardYOffset = 0.25f),
            LudoPawn(id = 5, player = 2, position = -1, yardXOffset = 0.25f, yardYOffset = 0.55f),
            LudoPawn(id = 6, player = 2, position = -1, yardXOffset = 0.55f, yardYOffset = 0.25f),
            LudoPawn(id = 7, player = 2, position = -1, yardXOffset = 0.55f, yardYOffset = 0.55f)
        )
    }

    var currentTurn by remember { mutableIntStateOf(1) } // 1: Player 1, 2: Player 2
    var diceValue by remember { mutableIntStateOf(1) }
    var isRolling by remember { mutableStateOf(false) }
    var hasRolledThisTurn by remember { mutableStateOf(false) }

    // Highlight playable pawns
    val playablePawns = remember { mutableStateListOf<Int>() }

    // Action banner
    var p1Lives by remember { mutableIntStateOf(3) }
    var p2Lives by remember { mutableIntStateOf(3) }
    var turnSecondsLeft by remember { mutableIntStateOf(8) }

    var actionBannerText by remember { mutableStateOf("ROLL DICE TO START!") }

    // Timer countdown
    LaunchedEffect(Unit) {
        while (matchSecondsLeft > 0) {
            delay(1000)
            matchSecondsLeft--
        }
        // Match finished on timeout!
        val userWon = p1Points >= p2Points
        onGameFinished(p1Points, userWon)
    }

    // Standard Ludo Rule Logic
    fun getPlayablePawns(player: Int, roll: Int): List<Int> {
        val list = mutableListOf<Int>()
        val playerPawns = pawns.filter { it.player == player }
        playerPawns.forEach { pawn ->
            if (pawn.position == -1) {
                // To get out of yard, must roll a 6
                if (roll == 6) list.add(pawn.id)
            } else if (pawn.position in 0..55) {
                // Check if can move without exceeding home base
                if (pawn.position + roll <= 56) {
                    list.add(pawn.id)
                }
            }
        }
        return list
    }

    // Capturing pawn check
    fun checkCaptures(movedPawn: LudoPawn) {
        val targetCoord = if (movedPawn.position in 0..50) {
            val idx = if (movedPawn.player == 1) {
                (39 + movedPawn.position) % 52
            } else {
                (13 + movedPawn.position) % 52
            }
            TrackCoordinates[idx]
        } else null

        if (targetCoord != null) {
            // Safe zone check (stars)
            val isSafeStar = listOf(
                Pair(1, 6), Pair(8, 1), Pair(13, 8), Pair(6, 13), // Starts
                Pair(2, 8), Pair(6, 2), Pair(12, 6), Pair(8, 12)  // Additional safe zones
            ).contains(targetCoord)

            if (!isSafeStar) {
                var capturedAny = false
                // Search for opponent pawns on the same track coordinate
                pawns.forEach { other ->
                    if (other.player != movedPawn.player && other.position in 0..50) {
                        val otherIdx = if (other.player == 1) {
                            (39 + other.position) % 52
                        } else {
                            (13 + other.position) % 52
                        }
                        val otherCoord = TrackCoordinates[otherIdx]
                        if (otherCoord == targetCoord) {
                            // CAPTURE! Send opponent pawn back to yard
                            other.position = -1
                            capturedAny = true
                            if (movedPawn.player == 1) {
                                p1Points += 50
                                actionBannerText = "💥 YOU CAPTURED GREEN PAWN! +50 PTS"
                            } else {
                                p2Points += 50
                                actionBannerText = "💥 ${opponentName.uppercase()} CAPTURED YOUR BLUE PAWN!"
                            }
                        }
                    }
                }
                if (capturedAny) {
                    LudoSoundEffects.playCapture()
                }
            }
        }
    }

    // Bot AI Turn execution
    fun executeOpponentTurn() {
        coroutineScope.launch {
            actionBannerText = "${opponentName.uppercase()} IS THINKING..."
            delay(1200)

            // Roll dice animation
            isRolling = true
            repeat(6) {
                diceValue = Random.nextInt(1, 7)
                LudoSoundEffects.playRollClick()
                delay(100)
            }
            isRolling = false

            val roll = diceValue
            actionBannerText = "${opponentName.uppercase()} ROLLED A $roll!"
            delay(800)

            val moves = getPlayablePawns(2, roll)
            if (moves.isEmpty()) {
                actionBannerText = "NO MOVES FOR ${opponentName.uppercase()}!"
                delay(1200)
                // Shift turn
                currentTurn = 1
                hasRolledThisTurn = false
                actionBannerText = "YOUR TURN! ROLL THE DICE."
            } else {
                // High IQ Bot Logic: prioritizes capturing, escaping danger, landing on stars, and advancing
                val safeStars = listOf(
                    Pair(1, 6), Pair(8, 1), Pair(13, 8), Pair(6, 13), 
                    Pair(2, 8), Pair(6, 2), Pair(12, 6), Pair(8, 12)
                )
                
                val chosenId = moves.maxByOrNull { id ->
                    val pawn = pawns.first { it.id == id }
                    var priority = 0
                    
                    if (pawn.position == -1 && roll == 6) {
                        priority = 60 // Good to get pawns out
                    } else {
                        val newPos = pawn.position + roll
                        if (newPos <= 50) {
                            val newIdx = (13 + newPos) % 52
                            val targetCoord = TrackCoordinates[newIdx]
                            val isSafeStar = safeStars.contains(targetCoord)
                            
                            // 1. Can we capture an opponent?
                            var canCapture = false
                            pawns.filter { it.player == 1 && it.position in 0..50 }.forEach { opp ->
                                val oppIdx = (39 + opp.position) % 52
                                if (TrackCoordinates[oppIdx] == targetCoord && !isSafeStar) {
                                    canCapture = true
                                }
                            }
                            if (canCapture) priority += 100 // Top priority!
                            
                            // 2. Are we landing on a safe star?
                            if (isSafeStar) priority += 40
                            
                            // 3. Are we escaping from a threatened position?
                            val currentIdx = (13 + pawn.position) % 52
                            val currentCoord = TrackCoordinates[currentIdx]
                            val currentlySafe = safeStars.contains(currentCoord)
                            
                            if (!currentlySafe && pawn.position >= 0) {
                                var isThreatened = false
                                pawns.filter { it.player == 1 && it.position in 0..50 }.forEach { opp ->
                                    val oppIdx = (39 + opp.position) % 52
                                    val dist = (currentIdx - oppIdx + 52) % 52
                                    if (dist in 1..5) isThreatened = true
                                }
                                if (isThreatened) priority += 50
                            }
                            
                            // 4. Progress towards home
                            priority += newPos / 2
                            if (newPos > 44) priority += 20 // Close to home column
                        } else if (newPos == 56) {
                            priority += 200 // Winning move!
                        } else {
                            priority += 15 // Advancing inside home column
                        }
                    }
                    priority
                } ?: moves.random()

                val pawn = pawns.first { it.id == chosenId }
                if (pawn.position == -1) {
                    pawn.position = 0
                    p2Points += 1
                    actionBannerText = "${opponentName.uppercase()} MOVED GREEN PAWN OUT!"
                    LudoSoundEffects.playMove()
                    delay(250)
                } else {
                    actionBannerText = "${opponentName.uppercase()} IS MOVING GREEN PAWN..."
                    for (step in 1..roll) {
                        if (pawn.position + 1 <= 56) {
                            pawn.position += 1
                            p2Points += 1
                            if (pawn.position == 56) {
                                p2Points += 100 // Finished bonus
                                actionBannerText = "🏆 BOT GOT A PAWN HOME!"
                                LudoSoundEffects.playFinish()
                            } else {
                                LudoSoundEffects.playMove()
                            }
                            delay(220)
                        }
                    }
                }

                checkCaptures(pawn)
                delay(800)

                // Check win condition
                if (pawns.filter { it.player == 2 }.all { it.position == 56 }) {
                    delay(800)
                    onGameFinished(p1Points, false)
                    return@launch
                }

                // If rolled a 6, gets another turn, else switch
                if (roll == 6) {
                    actionBannerText = "BOT ROLLED A 6! ROLLING AGAIN..."
                    executeOpponentTurn()
                } else {
                    currentTurn = 1
                    hasRolledThisTurn = false
                    actionBannerText = "YOUR TURN! ROLL THE DICE."
                }
            }
        }
    }

    // User pawn move click
    fun onUserPawnClicked(pawnId: Int) {
        if (currentTurn != 1 || !hasRolledThisTurn || !playablePawns.contains(pawnId)) return

        val pawn = pawns.first { it.id == pawnId }
        val roll = diceValue

        playablePawns.clear()
        hasRolledThisTurn = false // Prevent multi-tapping during animation

        coroutineScope.launch {
            if (pawn.position == -1) {
                pawn.position = 0
                p1Points += 1
                actionBannerText = "MOVED BLUE PAWN OUT OF YARD!"
                LudoSoundEffects.playMove()
                delay(250)
            } else {
                actionBannerText = "MOVING BLUE PAWN..."
                for (step in 1..roll) {
                    if (pawn.position + 1 <= 56) {
                        pawn.position += 1
                        p1Points += 1
                        if (pawn.position == 56) {
                            p1Points += 100 // Finished bonus
                            actionBannerText = "🎉 YOU GOT A BLUE PAWN HOME! +100 PTS"
                            LudoSoundEffects.playFinish()
                        } else {
                            LudoSoundEffects.playMove()
                        }
                        delay(220)
                    }
                }
            }

            checkCaptures(pawn)
            delay(600)

            // Check user win
            if (pawns.filter { it.player == 1 }.all { it.position == 56 }) {
                delay(800)
                onGameFinished(p1Points, true)
                return@launch
            }

            // Standard Ludo: 6 gives extra turn
            if (roll == 6) {
                hasRolledThisTurn = false
                actionBannerText = "ROLLED A 6! ROLL AGAIN."
            } else {
                currentTurn = 2
                hasRolledThisTurn = false
                executeOpponentTurn()
            }
        }
    }

    LaunchedEffect(currentTurn, hasRolledThisTurn) {
        turnSecondsLeft = 8
        while (turnSecondsLeft > 0) {
            delay(1000)
            turnSecondsLeft--
        }
        if (currentTurn == 1) {
            p1Lives--
            if (p1Lives <= 0) {
                onGameFinished(p1Points, false)
            } else {
                currentTurn = 2
                hasRolledThisTurn = false
                playablePawns.clear()
                executeOpponentTurn()
            }
        }
    }


    // Roll dice click
    fun onUserRollClicked() {
        if (currentTurn != 1 || isRolling || hasRolledThisTurn) return

        coroutineScope.launch {
            isRolling = true
            // Spin numbers
            repeat(8) {
                diceValue = Random.nextInt(1, 7)
                LudoSoundEffects.playRollClick()
                delay(80)
            }
            isRolling = false
            hasRolledThisTurn = true

            val roll = diceValue
            val moves = getPlayablePawns(1, roll)
            if (moves.isEmpty()) {
                actionBannerText = "ROLLED A $roll! NO MOVES."
                delay(1200)
                currentTurn = 2
                hasRolledThisTurn = false
                executeOpponentTurn()
            } else {
                actionBannerText = "ROLLED A $roll! CHOOSE BLUE PAWN TO MOVE."
                playablePawns.addAll(moves)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // --- Game Header ---
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "LUDO PRO MATCH", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PurpleGlow)
                    Text(
                        text = "Time Left: ${"%02d:%02d".format(matchSecondsLeft / 60, matchSecondsLeft % 60)}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val transition = rememberInfiniteTransition(label = "")
                    val alphaGlow by transition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ), label = ""
                    )

                    val p1Glow = if (currentTurn == 1) Modifier.border(2.dp, LudoBlue.copy(alpha = alphaGlow), RoundedCornerShape(4.dp)).padding(4.dp) else Modifier.padding(4.dp)
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = p1Glow) {
                        Text("YOU (BLUE)", color = LudoBlue, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text("$p1Points pts", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        Row {
                            repeat(3) { i ->
                                Icon(
                                    imageVector = if (i < p1Lives) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Life",
                                    tint = RedGlow,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        if (currentTurn == 1) {
                            Text("00:0${turnSecondsLeft}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    val p2Glow = if (currentTurn == 2) Modifier.border(2.dp, LudoGreen.copy(alpha = alphaGlow), RoundedCornerShape(4.dp)).padding(4.dp) else Modifier.padding(4.dp)
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = p2Glow) {
                        Text("OPPONENT (GREEN)", color = LudoGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text("$p2Points pts", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        Row {
                            repeat(3) { i ->
                                Icon(
                                    imageVector = if (i < p2Lives) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Life",
                                    tint = RedGlow,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        if (currentTurn == 2) {
                            Text("00:0${turnSecondsLeft}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Action info banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PurpleGlow.copy(alpha = 0.15f))
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = actionBannerText,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.weight(0.1f))

        // --- THE LUDO BOARD CANVAS ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(8.dp)
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(2.dp, LudoBorder, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Render the classic Ludo Board drawing Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val boardSize = size.minDimension
                val cellSize = boardSize / 15f

                // 1. Draw Home Bases Yards Backgrounds
                // Red base (top-left)
                drawRect(color = LudoRed, size = Size(cellSize * 6, cellSize * 6))
                // Green base (top-right)
                drawRect(color = LudoGreen, topLeft = Offset(cellSize * 9, 0f), size = Size(cellSize * 6, cellSize * 6))
                // Blue base (bottom-left)
                drawRect(color = LudoBlue, topLeft = Offset(0f, cellSize * 9), size = Size(cellSize * 6, cellSize * 6))
                // Yellow base (bottom-right)
                drawRect(color = LudoYellow, topLeft = Offset(cellSize * 9, cellSize * 9), size = Size(cellSize * 6, cellSize * 6))

                // 2. Draw nested white base inner yards
                drawRect(color = Color.White, topLeft = Offset(cellSize, cellSize), size = Size(cellSize * 4, cellSize * 4))
                drawRect(color = Color.White, topLeft = Offset(cellSize * 10, cellSize), size = Size(cellSize * 4, cellSize * 4))
                drawRect(color = Color.White, topLeft = Offset(cellSize, cellSize * 10), size = Size(cellSize * 4, cellSize * 4))
                drawRect(color = Color.White, topLeft = Offset(cellSize * 10, cellSize * 10), size = Size(cellSize * 4, cellSize * 4))

                // Draw standard base circle dots (pawn bases)
                val baseDotOffsets = listOf(
                    Offset(0.25f, 0.25f), Offset(0.25f, 0.55f),
                    Offset(0.55f, 0.25f), Offset(0.55f, 0.55f)
                )

                // Red dots
                baseDotOffsets.forEach { off ->
                    drawCircle(color = LudoRed, radius = cellSize * 0.4f, center = Offset(cellSize * (1 + off.x * 4), cellSize * (1 + off.y * 4)))
                }
                // Green dots
                baseDotOffsets.forEach { off ->
                    drawCircle(color = LudoGreen, radius = cellSize * 0.4f, center = Offset(cellSize * (10 + off.x * 4), cellSize * (1 + off.y * 4)))
                }
                // Blue dots
                baseDotOffsets.forEach { off ->
                    drawCircle(color = LudoBlue, radius = cellSize * 0.4f, center = Offset(cellSize * (1 + off.x * 4), cellSize * (10 + off.y * 4)))
                }
                // Yellow dots
                baseDotOffsets.forEach { off ->
                    drawCircle(color = LudoYellow, radius = cellSize * 0.4f, center = Offset(cellSize * (10 + off.x * 4), cellSize * (10 + off.y * 4)))
                }

                // 3. Draw standard track borders and cells (6 cells columns wings)
                // We'll draw 15x15 cell dividers lines
                for (i in 0..15) {
                    val pos = i * cellSize
                    // Vertical borders
                    drawLine(color = LudoBorder, start = Offset(pos, 0f), end = Offset(pos, boardSize), strokeWidth = 1.dp.toPx())
                    // Horizontal borders
                    drawLine(color = LudoBorder, start = Offset(0f, pos), end = Offset(boardSize, pos), strokeWidth = 1.dp.toPx())
                }

                // 4. Color the safe zones and home column cells
                // Red Home path (row 7, columns 1 to 5)
                for (col in 1..5) {
                    drawRect(color = LudoRed, topLeft = Offset(col * cellSize, 7 * cellSize), size = Size(cellSize, cellSize))
                }
                // Green Home path (column 7, rows 1 to 5)
                for (row in 1..5) {
                    drawRect(color = LudoGreen, topLeft = Offset(7 * cellSize, row * cellSize), size = Size(cellSize, cellSize))
                }
                // Yellow Home path (row 7, columns 9 to 13)
                for (col in 9..13) {
                    drawRect(color = LudoYellow, topLeft = Offset(col * cellSize, 7 * cellSize), size = Size(cellSize, cellSize))
                }
                // Blue Home path (column 7, rows 9 to 13)
                for (row in 9..13) {
                    drawRect(color = LudoBlue, topLeft = Offset(7 * cellSize, row * cellSize), size = Size(cellSize, cellSize))
                }

                // Color the starting cells
                drawRect(color = LudoRed, topLeft = Offset(cellSize, 6 * cellSize), size = Size(cellSize, cellSize))
                drawRect(color = LudoGreen, topLeft = Offset(8 * cellSize, cellSize), size = Size(cellSize, cellSize))
                drawRect(color = LudoYellow, topLeft = Offset(13 * cellSize, 8 * cellSize), size = Size(cellSize, cellSize))
                drawRect(color = LudoBlue, topLeft = Offset(6 * cellSize, 13 * cellSize), size = Size(cellSize, cellSize))

                // Draw safe zones stars
                val starPositions = listOf(
                    Pair(1, 6), Pair(8, 1), Pair(13, 8), Pair(6, 13), // Starts
                    Pair(2, 8), Pair(6, 2), Pair(12, 6), Pair(8, 12)  // Additional safe zones
                )
                starPositions.forEach { star ->
                    // Draw star shape instead of simple circle
                    val cx = (star.first + 0.5f) * cellSize
                    val cy = (star.second + 0.5f) * cellSize
                    val r = cellSize * 0.3f
                    val starPath = Path().apply {
                        val points = 5
                        val angle = Math.PI / points
                        for (i in 0 until points * 2) {
                            val radius = if (i % 2 == 0) r else r / 2
                            val x = cx + (radius * kotlin.math.cos(i * angle - Math.PI / 2)).toFloat()
                            val y = cy + (radius * kotlin.math.sin(i * angle - Math.PI / 2)).toFloat()
                            if (i == 0) moveTo(x, y) else lineTo(x, y)
                        }
                        close()
                    }
                    drawPath(path = starPath, color = Color.Yellow)
                }

                // 5. Draw center home triangle quadrants meeting at the center (row 6..8, col 6..8)
                val cLeft = 6 * cellSize
                val cRight = 9 * cellSize
                val cCenter = 7.5f * cellSize

                // Red (left) triangle
                val redTriPath = Path().apply {
                    moveTo(cLeft, cLeft)
                    lineTo(cCenter, cCenter)
                    lineTo(cLeft, cRight)
                    close()
                }
                drawPath(path = redTriPath, color = LudoRed)

                // Green (top) triangle
                val greenTriPath = Path().apply {
                    moveTo(cLeft, cLeft)
                    lineTo(cCenter, cCenter)
                    lineTo(cRight, cLeft)
                    close()
                }
                drawPath(path = greenTriPath, color = LudoGreen)

                // Yellow (right) triangle
                val yellowTriPath = Path().apply {
                    moveTo(cRight, cLeft)
                    lineTo(cCenter, cCenter)
                    lineTo(cRight, cRight)
                    close()
                }
                drawPath(path = yellowTriPath, color = LudoYellow)

                // Blue (bottom) triangle
                val blueTriPath = Path().apply {
                    moveTo(cLeft, cRight)
                    lineTo(cCenter, cCenter)
                    lineTo(cRight, cRight)
                    close()
                }
                drawPath(path = blueTriPath, color = LudoBlue)

                // Draw thick borders on central triangles meeting lines
                drawLine(color = LudoBorder, start = Offset(cLeft, cLeft), end = Offset(cRight, cRight), strokeWidth = 2.dp.toPx())
                drawLine(color = LudoBorder, start = Offset(cLeft, cRight), end = Offset(cRight, cLeft), strokeWidth = 2.dp.toPx())
            }

            // Draw Pawns / Pins dynamically on top of the Canvas grid
            pawns.forEach { pawn ->
                val boardSize = 15f
                val (col, row) = when {
                    pawn.position == -1 -> {
                        // Base yard coordinates
                        if (pawn.player == 1) {
                            Pair(1 + pawn.yardXOffset * 4, 10 + pawn.yardYOffset * 4)
                        } else {
                            Pair(10 + pawn.yardXOffset * 4, 1 + pawn.yardYOffset * 4)
                        }
                    }
                    pawn.position in 0..50 -> {
                        // Track coordinates
                        val trackIndex = if (pawn.player == 1) {
                            (39 + pawn.position) % 52
                        } else {
                            (13 + pawn.position) % 52
                        }
                        val coord = TrackCoordinates[trackIndex]
                        Pair(coord.first + 0.5f, coord.second + 0.5f)
                    }
                    pawn.position in 51..55 -> {
                        // Home columns coordinates
                        val step = pawn.position - 51
                        if (pawn.player == 1) {
                            Pair(7.5f, 13.5f - step)
                        } else {
                            Pair(7.5f, 1.5f + step)
                        }
                    }
                    else -> {
                        // Finished
                        if (pawn.player == 1) {
                            Pair(7.5f, 8.2f)
                        } else {
                            Pair(7.5f, 6.8f)
                        }
                    }
                }

                // Draw interactive Pin marker Composable
                val color = if (pawn.player == 1) LudoBlue else LudoGreen
                val isPawnPlayable = playablePawns.contains(pawn.id) && currentTurn == 1

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center)
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val boardPx = constraints.maxWidth
                        val cellPx = boardPx / 15f
                        val xOffset = col * cellPx - cellPx / 2
                        val yOffset = row * cellPx - cellPx / 2

                        // Breathe scale animation for playable pawns
                        val infiniteTransition = rememberInfiniteTransition(label = "")
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = if (isPawnPlayable) 1.25f else 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ), label = ""
                        )

                        Box(
                            modifier = Modifier
                                .absoluteOffset(
                                    x = (xOffset / LocalContext.current.resources.displayMetrics.density).dp,
                                    y = (yOffset / LocalContext.current.resources.displayMetrics.density).dp
                                )
                                .size((cellPx / LocalContext.current.resources.displayMetrics.density).dp)
                                .clip(CircleShape)
                                .clickable(enabled = isPawnPlayable) {
                                    onUserPawnClicked(pawn.id)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // 3D Pin Style Shape
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .border(
                                        width = if (isPawnPlayable) 2.dp else 1.dp,
                                        color = if (isPawnPlayable) CyanGlow else Color.White,
                                        shape = CircleShape
                                    )
                                    .background(color, CircleShape)
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color.White, CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.1f))

        // --- Dice rolling control block at bottom ---
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(16.dp, 16.dp, 0.dp, 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp, 16.dp, 0.dp, 0.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // User info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(LudoBlue.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "User", tint = LudoBlue)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("PLAYER 1 (YOU)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        Text(
                            text = if (currentTurn == 1) "YOUR TURN" else "BOT'S TURN",
                            color = if (currentTurn == 1) CyanGlow else Color.Gray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                // Interactive 3D Dice block roller
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.White, Color(0xFFE0E0E0))
                            )
                        )
                        .border(2.dp, if (currentTurn == 1 && !hasRolledThisTurn) CyanGlow else BorderColor, RoundedCornerShape(12.dp))
                        .clickable(enabled = currentTurn == 1 && !hasRolledThisTurn && !isRolling) {
                            onUserRollClicked()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isRolling) {
                        CircularProgressIndicator(color = PurpleGlow, modifier = Modifier.size(24.dp))
                    } else {
                        // Render standard Dice dot faces based on value
                        DiceFaceDrawing(value = diceValue)
                    }
                }
            }
        }
    }
}

@Composable
fun DiceFaceDrawing(value: Int) {
    Box(
        modifier = Modifier
            .size(36.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension / 8f
            val w = size.width
            val h = size.height

            val dots = when (value) {
                1 -> listOf(Offset(w / 2, h / 2))
                2 -> listOf(Offset(w / 4, h / 4), Offset(3 * w / 4, 3 * h / 4))
                3 -> listOf(Offset(w / 4, h / 4), Offset(w / 2, h / 2), Offset(3 * w / 4, 3 * h / 4))
                4 -> listOf(Offset(w / 4, h / 4), Offset(3 * w / 4, h / 4), Offset(w / 4, 3 * h / 4), Offset(3 * w / 4, 3 * h / 4))
                5 -> listOf(Offset(w / 4, h / 4), Offset(3 * w / 4, h / 4), Offset(w / 2, h / 2), Offset(w / 4, 3 * h / 4), Offset(3 * w / 4, 3 * h / 4))
                else -> listOf(
                    Offset(w / 4, h / 4), Offset(w / 4, h / 2), Offset(w / 4, 3 * h / 4),
                    Offset(3 * w / 4, h / 4), Offset(3 * w / 4, h / 2), Offset(3 * w / 4, 3 * h / 4)
                )
            }

            dots.forEach { center ->
                drawCircle(color = Color.Black, radius = r, center = center)
            }
        }
    }
}

@Composable
fun LudoResultScreen(
    viewModel: MainViewModel,
    tournament: Tournament,
    score: Int,
    isWinner: Boolean,
    onClose: () -> Unit
) {
    var isSavingResult by remember { mutableStateOf(true) }
    var prizeWonByPlayer by remember { mutableDoubleStateOf(0.0) }

    LaunchedEffect(Unit) {
        viewModel.completeLudoGame(tournament.id, score, isWinner) { prize ->
            prizeWonByPlayer = prize
            isSavingResult = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isSavingResult) {
            CircularProgressIndicator(color = PurpleGlow)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Saving your results on Server...", color = Color.White, fontSize = 14.sp)
        } else {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(if (isWinner) EmeraldGlow.copy(alpha = 0.15f) else RedGlow.copy(alpha = 0.15f), CircleShape)
                    .border(2.dp, if (isWinner) EmeraldGlow else RedGlow, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isWinner) Icons.Default.EmojiEvents else Icons.Default.SentimentVeryDissatisfied,
                    contentDescription = "Result",
                    tint = if (isWinner) EmeraldGlow else RedGlow,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = if (isWinner) "🏆 CONGRATULATIONS! YOU WON!" else "DEFEAT! BETTER LUCK NEXT TIME!",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isWinner) EmeraldGlow else Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Your Total Score: $score Points",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (isWinner && prizeWonByPlayer > 0.0) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = EmeraldGlow.copy(alpha = 0.15f)),
                    modifier = Modifier.border(1.dp, EmeraldGlow, RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = "Prize Awarded: ₹${"%.2f".format(prizeWonByPlayer)} added to Withdrawal Wallet!",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = PurpleGlow),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Return to Tournament Page", fontWeight = FontWeight.Bold)
            }
        }
    }
}
