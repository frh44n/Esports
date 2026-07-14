package com.example.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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

            // 5x5 Grid Board (Matching the Stake visual theme)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F212E)) // dark board container background
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                    .padding(8.dp)
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
                                        .clickable(
                                            enabled = activeGame != null &&
                                                    activeGame?.status == "ACTIVE" &&
                                                    !activeGame!!.revealed.contains(tileIdx) &&
                                                    !loading
                                        ) {
                                            // Play tactile click sound instantly when pressed
                                            MinesSoundPlayer.playClickSound()
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

                    Spacer(modifier = Modifier.height(4.dp))

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
                            Spacer(modifier = Modifier.height(4.dp))
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
// HIGH-FIDELITY VECTOR GRAPHICS DRAWING COMPOSABLES
// -------------------------------------------------------------

@Composable
fun GemstoneIcon(
    modifier: Modifier = Modifier,
    isFaded: Boolean = false
) {
    Canvas(modifier = modifier.fillMaxSize(0.72f)) {
        val w = size.width
        val h = size.height

        // Define the 8 key points of the diamond geometry (normalized to fit beautifully)
        val p1 = Offset(w * 0.33f, h * 0.20f) // Top-left table corner
        val p2 = Offset(w * 0.67f, h * 0.20f) // Top-right table corner
        val p3 = Offset(w * 0.12f, h * 0.45f) // Mid-left crown corner
        val p4 = Offset(w * 0.88f, h * 0.45f) // Mid-right crown corner
        val p5 = Offset(w * 0.50f, h * 0.85f) // Bottom tip point
        val p6 = Offset(w * 0.50f, h * 0.45f) // Center horizontal mid-point
        val p7 = Offset(w * 0.33f, h * 0.45f) // Inner left transition
        val p8 = Offset(w * 0.67f, h * 0.45f) // Inner right transition

        // Facet colors matching the user's uploaded photo
        // Active gems are shiny vibrant neon emeralds, faded gems are deep dark-green forest colors
        val tableColor = if (isFaded) Color(0xFF0B2D19) else Color(0xFF39FF14) // Neon table flat
        val crownLeftColor = if (isFaded) Color(0xFF082414) else Color(0xFF00FF66) // Vibrant mint
        val crownRightColor = if (isFaded) Color(0xFF061E10) else Color(0xFF00D15D) // Medium-vibrant emerald
        val pavilionCenterColor = if (isFaded) Color(0xFF05190D) else Color(0xFF00C853) // Standard rich green
        val pavilionLeftColor = if (isFaded) Color(0xFF04140A) else Color(0xFF00A343) // Medium dark green
        val pavilionRightColor = if (isFaded) Color(0xFF020E07) else Color(0xFF008234) // Darker shadow green

        val strokeColor = if (isFaded) Color(0x3300FF66) else Color(0x66FFFFFF)
        val strokeWidth = if (isFaded) 1f else 2.5f

        // Draw a single facet polygon helper
        fun drawFacet(points: List<Offset>, color: Color) {
            val path = Path().apply {
                if (points.isNotEmpty()) {
                    moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                    close()
                }
            }
            drawPath(path, color)
        }

        // Render the 6 diamond facets
        drawFacet(listOf(p1, p2, p8, p7), tableColor)           // Table (Top Center Hexagon/Trapezoid)
        drawFacet(listOf(p1, p3, p7), crownLeftColor)          // Crown Left Triangle
        drawFacet(listOf(p2, p8, p4), crownRightColor)         // Crown Right Triangle
        drawFacet(listOf(p7, p8, p5), pavilionCenterColor)     // Pavilion Center Polygon
        drawFacet(listOf(p3, p7, p5), pavilionLeftColor)       // Pavilion Left Triangle
        drawFacet(listOf(p4, p5, p8), pavilionRightColor)      // Pavilion Right Triangle

        // Draw crystal clean facet edge guidelines for maximum sparkle and depth!
        val edges = listOf(
            Pair(p1, p2), Pair(p1, p3), Pair(p1, p7),
            Pair(p2, p4), Pair(p2, p8), Pair(p7, p8),
            Pair(p3, p7), Pair(p4, p8),
            Pair(p7, p5), Pair(p8, p5), Pair(p3, p5), Pair(p4, p5)
        )
        for (edge in edges) {
            drawLine(
                color = strokeColor,
                start = edge.first,
                end = edge.second,
                strokeWidth = strokeWidth
            )
        }
    }
}

@Composable
fun BombIcon(
    modifier: Modifier = Modifier,
    isFaded: Boolean = false,
    isSparking: Boolean = false
) {
    Canvas(modifier = modifier.fillMaxSize(0.72f)) {
        val w = size.width
        val h = size.height

        val cx = w * 0.5f
        val cy = h * 0.58f
        val r = w * 0.28f

        // 1. Draw small fuse neck cap
        val capWidth = w * 0.08f
        val capHeight = h * 0.05f
        val capLeft = cx - capWidth / 2
        val capTop = cy - r - capHeight + 2f // slight overlap with sphere

        val capColor = if (isFaded) Color(0xFF1E252B) else Color(0xFF37474F)
        drawRect(
            color = capColor,
            topLeft = Offset(capLeft, capTop),
            size = Size(capWidth, capHeight)
        )

        // 2. Draw curved fuse line
        val fusePath = Path().apply {
            moveTo(cx, capTop)
            cubicTo(
                cx + w * 0.05f, capTop - h * 0.08f,
                cx + w * 0.12f, capTop - h * 0.12f,
                cx + w * 0.16f, capTop - h * 0.06f
            )
        }
        val fuseColor = if (isFaded) Color(0xFF2B363E) else Color(0xFF78909C)
        drawPath(
            path = fusePath,
            color = fuseColor,
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        )

        // 3. Draw solid circular bomb body (using premium 3D radial light gradient!)
        if (isFaded) {
            val fadedGradient = Brush.radialGradient(
                colors = listOf(Color(0xFF531E24), Color(0xFF250C0F)),
                center = Offset(cx - r * 0.2f, cy - r * 0.2f),
                radius = r
            )
            drawCircle(
                brush = fadedGradient,
                radius = r,
                center = Offset(cx, cy)
            )
        } else {
            val activeGradient = Brush.radialGradient(
                colors = listOf(Color(0xFFFF5252), Color(0xFFC2185B)),
                center = Offset(cx - r * 0.2f, cy - r * 0.2f),
                radius = r
            )
            drawCircle(
                brush = activeGradient,
                radius = r,
                center = Offset(cx, cy)
            )
        }

        // 4. Draw burning sparkle starburst at the tip of the fuse
        if (isSparking && !isFaded) {
            val sparkCx = cx + w * 0.16f
            val sparkCy = capTop - h * 0.06f

            // Inner bright white spark nucleus
            drawCircle(
                color = Color.White,
                radius = 3.5.dp.toPx(),
                center = Offset(sparkCx, sparkCy)
            )
            drawCircle(
                color = Color(0xFFFFEA00),
                radius = 7.5.dp.toPx(),
                center = Offset(sparkCx, sparkCy),
                alpha = 0.5f
            )

            // Dynamic 8-ray sparks
            val numSpikes = 8
            val innerR = 4.dp.toPx()
            val outerR = 15.dp.toPx()
            for (idx in 0 until numSpikes) {
                val angle = (idx * 2 * Math.PI / numSpikes).toFloat()
                val startX = sparkCx + innerR * kotlin.math.cos(angle)
                val startY = sparkCy + innerR * kotlin.math.sin(angle)
                val endX = sparkCx + outerR * kotlin.math.cos(angle)
                val endY = sparkCy + outerR * kotlin.math.sin(angle)

                // Orange base spike
                drawLine(
                    color = Color(0xFFFF5722),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Yellow inner spike
                drawLine(
                    color = Color(0xFFFFEA00),
                    start = Offset(startX, startY),
                    end = Offset(
                        sparkCx + (outerR - 3.dp.toPx()) * kotlin.math.cos(angle),
                        sparkCy + (outerR - 3.dp.toPx()) * kotlin.math.sin(angle)
                    ),
                    strokeWidth = 1.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

// -------------------------------------------------------------
// HELPER METHODS FOR VISUAL STATE/COLORS
// -------------------------------------------------------------

@Composable
private fun getTileBackgroundColor(
    tileIdx: Int,
    activeGame: com.example.data.MinesGame?
): Color {
    // Ground/board of cell, matches Stake's dark theme
    if (activeGame == null) return Color(0xFF213743) // Solid lighter blue-gray unrevealed tile

    val isRevealed = activeGame.revealed.contains(tileIdx)
    val status = activeGame.status

    if (isRevealed) {
        return Color(0xFF0F212E) // Revealed/clicked blends perfectly into dark board background
    }

    // At the end of the game, show remaining tiles also as dark background
    if (status == "WON" || status == "LOST") {
        return Color(0xFF0F212E)
    }

    return Color(0xFF213743) // Active unrevealed tile
}

@Composable
private fun RenderTileContent(
    tileIdx: Int,
    activeGame: com.example.data.MinesGame?
) {
    if (activeGame == null) {
        GemstoneIcon(isFaded = true)
        return
    }

    val isRevealed = activeGame.revealed.contains(tileIdx)
    val status = activeGame.status

    if (isRevealed) {
        val board = activeGame.board
        if (board != null) {
            val isMine = board[tileIdx]
            if (isMine) {
                BombIcon(isFaded = false, isSparking = true)
            } else {
                GemstoneIcon(isFaded = false)
            }
        } else {
            // Default to bright gemstone if board not fully loaded
            GemstoneIcon(isFaded = false)
        }
        return
    }

    // At the end of the game (WON or LOST), show remaining tiles faded out
    if (status == "WON" || status == "LOST") {
        val board = activeGame.board
        if (board != null) {
            val isMine = board[tileIdx]
            if (isMine) {
                // Faded out bomb
                BombIcon(isFaded = true, isSparking = false)
            } else {
                // Faded out gemstone
                GemstoneIcon(isFaded = true)
            }
        }
    }
}
