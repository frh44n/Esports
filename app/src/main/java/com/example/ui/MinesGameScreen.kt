package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinesGameScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val activeGame by viewModel.minesActiveGame.collectAsStateWithLifecycle()
    val loading by viewModel.minesLoading.collectAsStateWithLifecycle()
    val user by viewModel.loggedInUser.collectAsStateWithLifecycle()

    var betAmountStr by remember { mutableStateOf("10") }
    var minesCount by remember { mutableIntStateOf(3) }
    var showHelpDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.checkActiveMinesGame()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("STAKE MINES", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Help", tint = CyanGlow)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        containerColor = DarkBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Balances Display
            user?.let { u ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Deposit Balance", color = Color.Gray, fontSize = 11.sp)
                            Text("₹${String.format("%.2f", u.depositBalance)}", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Withdrawal Balance", color = Color.Gray, fontSize = 11.sp)
                            Text("₹${String.format("%.2f", u.withdrawalBalance)}", color = EmeraldGlow, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Game Board Title/Info
            activeGame?.let { game ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Game Active", color = CyanGlow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        "Multiplier: ${String.format("%.2f", game.multiplier)}x",
                        color = AmberGlow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            } ?: run {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Place Your Bet", color = Color.Gray, fontSize = 13.sp)
                    Text("Multiplier: 1.00x", color = Color.Gray, fontSize = 13.sp)
                }
            }

            // 5x5 Grid Board
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBg)
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    for (row in 0 until 5) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (col in 0 until 5) {
                                val tileIdx = row * 5 + col
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            getTileBackgroundColor(
                                                tileIdx = tileIdx,
                                                activeGame = activeGame
                                            )
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = getTileBorderColor(tileIdx = tileIdx, activeGame = activeGame),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable(
                                            enabled = activeGame != null &&
                                                    activeGame?.status == "ACTIVE" &&
                                                    !activeGame!!.revealed.contains(tileIdx) &&
                                                    !loading
                                        ) {
                                            viewModel.revealMinesTile(
                                                tileIndex = tileIdx,
                                                onGemRevealed = { MinesSoundPlayer.playGemSound() },
                                                onMineHit = { MinesSoundPlayer.playExplodeSound() },
                                                onAutoCashout = { MinesSoundPlayer.playCashoutSound() }
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    RenderTileContent(
                                        tileIdx = tileIdx,
                                        activeGame = activeGame
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Next Tile Potential Info
            activeGame?.let { game ->
                if (game.status == "ACTIVE") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2638)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Next Tile Value:", color = Color.White, fontSize = 12.sp)
                            Text(
                                "Multiplier: ${String.format("%.2f", game.nextMultiplier)}x  (₹${String.format("%.2f", game.betAmount * game.nextMultiplier)})",
                                color = CyanGlow,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Controls Block
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Bet Amount input
                    Column {
                        Text("Bet Amount (₹)", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = betAmountStr,
                                onValueChange = { if (activeGame == null) betAmountStr = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                enabled = activeGame == null,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanGlow,
                                    unfocusedBorderColor = BorderColor,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    disabledBorderColor = BorderColor,
                                    disabledTextColor = Color.Gray
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            // Quick adjustment buttons (disabled during active game)
                            Button(
                                onClick = {
                                    val currentBet = betAmountStr.toDoubleOrNull() ?: 10.0
                                    betAmountStr = String.format("%.2f", (currentBet / 2).coerceAtLeast(1.0))
                                },
                                enabled = activeGame == null,
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BorderColor),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("1/2", color = Color.White, fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    val currentBet = betAmountStr.toDoubleOrNull() ?: 10.0
                                    betAmountStr = String.format("%.2f", currentBet * 2)
                                },
                                enabled = activeGame == null,
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BorderColor),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("2x", color = Color.White, fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    user?.let { u ->
                                        val maxBet = u.depositBalance + u.withdrawalBalance
                                        betAmountStr = String.format("%.2f", maxBet)
                                    }
                                },
                                enabled = activeGame == null,
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BorderColor),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Max", color = CyanGlow, fontSize = 11.sp)
                            }
                        }
                    }

                    // Number of Mines selection
                    if (activeGame == null) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Mines Count: $minesCount", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Gems Count: ${25 - minesCount}", color = Color.Gray, fontSize = 11.sp)
                            }
                            Slider(
                                value = minesCount.toFloat(),
                                onValueChange = { minesCount = it.toInt() },
                                valueRange = 1f..24f,
                                steps = 22,
                                colors = SliderDefaults.colors(
                                    thumbColor = CyanGlow,
                                    activeTrackColor = CyanGlow,
                                    inactiveTrackColor = Color.DarkGray
                                )
                            )
                        }
                    } else {
                        // Display active mines count
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Active Mines: ${activeGame!!.minesCount}", color = Color.Gray, fontSize = 11.sp)
                            Text("Remaining Gems: ${25 - activeGame!!.minesCount - activeGame!!.revealed.size}", color = Color.Gray, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Primary Play/Cashout Button
                    if (activeGame != null && activeGame!!.status == "ACTIVE") {
                        // Cashout Button
                        val winAmount = activeGame!!.betAmount * activeGame!!.multiplier
                        Button(
                            onClick = {
                                viewModel.cashoutMinesGame {
                                    MinesSoundPlayer.playCashoutSound()
                                }
                            },
                            enabled = !loading,
                            colors = ButtonDefaults.buttonColors(containerColor = AmberGlow),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                "Cashout  ₹${String.format("%.2f", winAmount)} (${String.format("%.2f", activeGame!!.multiplier)}x)",
                                color = DarkBg,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    } else if (activeGame != null && (activeGame!!.status == "WON" || activeGame!!.status == "LOST")) {
                        // Reset session to start over
                        Button(
                            onClick = { viewModel.resetMinesSessionState() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Play Again", color = DarkBg, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Bet Button
                        Button(
                            onClick = {
                                val bet = betAmountStr.toDoubleOrNull() ?: 0.0
                                if (bet <= 0) {
                                    viewModel.showToast("Enter a valid bet amount")
                                    return@Button
                                }
                                viewModel.startMinesGame(bet, minesCount)
                            },
                            enabled = !loading,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                if (loading) "Loading..." else "Bet",
                                color = DarkBg,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("How to play Mines?", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Text(
                    "1. Place your desired bet amount.\n" +
                            "2. Choose the number of mines (1-24).\n" +
                            "3. Press Bet to begin.\n" +
                            "4. Click on the gray tiles to reveal what's hidden under them.\n" +
                            "5. If it's a GEM 💎, your multiplier increases! You can Cashout any time.\n" +
                            "6. If you hit a MINE 💥, you lose the bet and the game ends.",
                    color = Color.LightGray,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Got It", color = CyanGlow)
                }
            },
            containerColor = CardBg
        )
    }
}

// -------------------------------------------------------------
// HELPER METHODS FOR VISUAL RENDER
// -------------------------------------------------------------

@Composable
private fun getTileBackgroundColor(
    tileIdx: Int,
    activeGame: com.example.data.MinesGame?
): Color {
    if (activeGame == null) return BorderColor

    val isRevealed = activeGame.revealed.contains(tileIdx)
    val status = activeGame.status

    if (isRevealed) {
        val board = activeGame.board
        return if (board != null) {
            val isMine = board[tileIdx]
            if (isMine) Color(0xFF4C151B) else Color(0xFF154C2B)
        } else {
            // Active game or win prior to board load
            Color(0xFF154C2B)
        }
    }

    // Unrevealed tiles in completed game (reveal at end)
    if (status == "WON" || status == "LOST") {
        val board = activeGame.board
        if (board != null) {
            val isMine = board[tileIdx]
            return if (isMine) Color(0x33FF3D00) else Color(0x3300E676)
        }
    }

    return BorderColor
}

@Composable
private fun getTileBorderColor(
    tileIdx: Int,
    activeGame: com.example.data.MinesGame?
): Color {
    if (activeGame == null) return BorderColor

    val isRevealed = activeGame.revealed.contains(tileIdx)
    val status = activeGame.status

    if (isRevealed) {
        val board = activeGame.board
        return if (board != null) {
            val isMine = board[tileIdx]
            if (isMine) RedGlow else EmeraldGlow
        } else {
            EmeraldGlow
        }
    }

    if (status == "WON" || status == "LOST") {
        val board = activeGame.board
        if (board != null) {
            val isMine = board[tileIdx]
            return if (isMine) Color(0x55FF3D00) else Color(0x5500E676)
        }
    }

    return BorderColor
}

@Composable
private fun RenderTileContent(
    tileIdx: Int,
    activeGame: com.example.data.MinesGame?
) {
    if (activeGame == null) return

    val isRevealed = activeGame.revealed.contains(tileIdx)
    val status = activeGame.status

    if (isRevealed) {
        val board = activeGame.board
        if (board != null) {
            val isMine = board[tileIdx]
            if (isMine) {
                Icon(Icons.Default.Warning, contentDescription = "Mine", tint = RedGlow, modifier = Modifier.size(24.dp))
            } else {
                Icon(Icons.Default.Star, contentDescription = "Gem", tint = AmberGlow, modifier = Modifier.size(24.dp))
            }
        } else {
            // Active or completed game without full board loaded yet
            Icon(Icons.Default.Star, contentDescription = "Gem", tint = AmberGlow, modifier = Modifier.size(24.dp))
        }
        return
    }

    // At the end of the game, show remaining tiles faded out
    if (status == "WON" || status == "LOST") {
        val board = activeGame.board
        if (board != null) {
            val isMine = board[tileIdx]
            if (isMine) {
                Icon(Icons.Default.Warning, contentDescription = "Mine", tint = RedGlow.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
            } else {
                Icon(Icons.Default.Star, contentDescription = "Gem", tint = AmberGlow.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
            }
        }
    }
}
