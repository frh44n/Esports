package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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

    val isGameActive = activeGame != null && activeGame!!.status == "ACTIVE"
    val isGameFinished = activeGame != null && (activeGame!!.status == "LOST" || activeGame!!.status == "WON")

    LaunchedEffect(Unit) {
        viewModel.checkActiveMinesGame()
    }

    // Direct play action: starts game immediately without needing to reset or click bet again
    fun onPlayAgainOrBet() {
        val bet = betAmountStr.toDoubleOrNull() ?: activeGame?.betAmount ?: 10.0
        if (bet <= 0) {
            viewModel.showToast("Enter a valid bet amount")
            return
        }
        user?.let { u ->
            val totalBal = u.depositBalance + u.withdrawalBalance
            if (bet > totalBal) {
                viewModel.showToast("Insufficient balance for ₹$bet")
                return
            }
        }
        val count = if (minesCount in 1..24) minesCount else (activeGame?.minesCount ?: 3)
        viewModel.startMinesGame(bet, count)
    }

    BackHandler {
        if (showHelpDialog) {
            showHelpDialog = false
        } else {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            // Compact Header Bar with Back, Title, Live Wallet balance pill, and Help
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "STAKE MINES",
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 17.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Sleek wallet balance badge
                    user?.let { u ->
                        Surface(
                            color = CardBg,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, BorderColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("₹", color = EmeraldGlow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    String.format("%.2f", u.depositBalance + u.withdrawalBalance),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    IconButton(onClick = { showHelpDialog = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Info, contentDescription = "Help", tint = CyanGlow)
                    }
                }
            }
        },
        containerColor = DarkBg
    ) { padding ->
        // Full screen non-scrolling layout: everything fits on screen at once!
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Status & Multiplier strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF141A26))
                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isGameActive) {
                    Text(
                        "💎 ${activeGame!!.revealed.size} Gems Found",
                        color = EmeraldGlow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${String.format("%.2f", activeGame!!.multiplier)}x",
                            color = AmberGlow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Next: ${String.format("%.2f", activeGame!!.nextMultiplier)}x",
                            color = CyanGlow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                } else if (activeGame != null && activeGame!!.status == "LOST") {
                    Text(
                        "💥 Hit a Mine! (-₹${String.format("%.2f", activeGame!!.betAmount)})",
                        color = RedGlow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        "Tap Play Again below",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                } else if (activeGame != null && activeGame!!.status == "WON") {
                    Text(
                        "🎉 Won ₹${String.format("%.2f", activeGame!!.betAmount * activeGame!!.multiplier)}!",
                        color = EmeraldGlow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        "${String.format("%.2f", activeGame!!.multiplier)}x",
                        color = AmberGlow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                } else {
                    Text(
                        "💣 $minesCount Mines  •  💎 ${25 - minesCount} Gems",
                        color = Color.LightGray,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                    Text(
                        "Start: 1.00x",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }

            // 2. 5x5 Mines Grid Board (Responsive size that dynamically fits available space)
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                val boardSize = minOf(maxWidth, maxHeight, 360.dp)
                Box(
                    modifier = Modifier
                        .size(boardSize)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F212E))
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .padding(6.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (row in 0 until 5) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (col in 0 until 5) {
                                    val tileIdx = row * 5 + col
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                getTileBackgroundColor(
                                                    tileIdx = tileIdx,
                                                    activeGame = activeGame
                                                )
                                            )
                                            .clickable(
                                                enabled = isGameActive &&
                                                        !activeGame!!.revealed.contains(tileIdx) &&
                                                        !loading
                                            ) {
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
            }

            // 3. Action Section: Primary Button & Compact Controls (Never offscreen!)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Primary Action Button (Cashout / Play Again / Bet)
                if (isGameActive) {
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
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            "CASHOUT  ₹${String.format("%.2f", winAmount)} (${String.format("%.2f", activeGame!!.multiplier)}x)",
                            color = DarkBg,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                    }
                } else if (isGameFinished) {
                    // USER REQUIREMENT: When a player losses they can play again by just clicking on button Play again, no need to click on Bet button again.
                    Button(
                        onClick = {
                            MinesSoundPlayer.playClickSound()
                            onPlayAgainOrBet()
                        },
                        enabled = !loading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeGame?.status == "LOST") EmeraldGlow else CyanGlow
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            if (loading) "Starting..." else "PLAY AGAIN (₹${betAmountStr})",
                            color = DarkBg,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            MinesSoundPlayer.playClickSound()
                            onPlayAgainOrBet()
                        },
                        enabled = !loading,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            if (loading) "Loading..." else "BET (₹${betAmountStr})",
                            color = DarkBg,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                    }
                }

                // Compact Bet & Mines Controls Strip
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Bet Amount Column (Left)
                        Column(modifier = Modifier.weight(1.2f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Bet (₹)", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        "1/2",
                                        color = if (!isGameActive) Color.White else Color.DarkGray,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF263242))
                                            .clickable(enabled = !isGameActive) {
                                                val currentBet = betAmountStr.toDoubleOrNull() ?: 10.0
                                                betAmountStr = String.format("%.0f", (currentBet / 2).coerceAtLeast(1.0))
                                            }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                    Text(
                                        "2x",
                                        color = if (!isGameActive) Color.White else Color.DarkGray,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF263242))
                                            .clickable(enabled = !isGameActive) {
                                                val currentBet = betAmountStr.toDoubleOrNull() ?: 10.0
                                                betAmountStr = String.format("%.0f", currentBet * 2)
                                            }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                    Text(
                                        "Max",
                                        color = if (!isGameActive) CyanGlow else Color.DarkGray,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF263242))
                                            .clickable(enabled = !isGameActive) {
                                                user?.let { u ->
                                                    val maxBet = u.depositBalance + u.withdrawalBalance
                                                    betAmountStr = String.format("%.0f", maxBet)
                                                }
                                            }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            BasicTextField(
                                value = betAmountStr,
                                onValueChange = { if (!isGameActive) betAmountStr = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                enabled = !isGameActive,
                                textStyle = LocalTextStyle.current.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF0F1522))
                                    .border(1.dp, if (!isGameActive) BorderColor else Color.Transparent, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }

                        // Mines Stepper Column (Right)
                        Column(
                            modifier = Modifier.weight(0.9f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Mines Count",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF0F1522))
                                    .border(1.dp, if (!isGameActive) BorderColor else Color.Transparent, RoundedCornerShape(6.dp)),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clickable(enabled = !isGameActive && minesCount > 1) {
                                            minesCount--
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "-",
                                        color = if (!isGameActive && minesCount > 1) CyanGlow else Color.DarkGray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }

                                Text(
                                    "$minesCount 💣",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clickable(enabled = !isGameActive && minesCount < 24) {
                                            minesCount++
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "+",
                                        color = if (!isGameActive && minesCount < 24) CyanGlow else Color.DarkGray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
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
                            "6. If you hit a MINE 💥, you lose the bet and the game ends.\n" +
                            "7. Tap Play Again to immediately jump into the next round!",
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
