package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Tournament
import com.example.data.Transaction
import com.example.data.SupabaseClient
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.animation.core.*

// Centralized premium gaming color tokens
val DarkBg = Color(0xFF0C0F16)
val CardBg = Color(0xFF161B26)
val BorderColor = Color(0xFF262E3E)
val CyanGlow = Color(0xFF00E5FF)
val PurpleGlow = Color(0xFF9D4EDD)
val EmeraldGlow = Color(0xFF00E676)
val AmberGlow = Color(0xFFFFAA00)
val RedGlow = Color(0xFFFF3D00)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUi(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBg)
        ) {
            when (currentScreen) {
                "splash" -> SplashScreen()
                "auth" -> AuthScreen(viewModel)
                "home" -> HomeScreen(viewModel)
                "admin" -> AdminPanelScreen(viewModel)
            }
        }
    }
}

@Composable
fun AppLogo(modifier: Modifier = Modifier, scale: Float = 1f) {
    Image(
        painter = painterResource(id = R.drawable.img_arena_logo),
        contentDescription = "Arena Esports Logo",
        modifier = modifier
            .scale(scale)
            .size(110.dp)
            .clip(RoundedCornerShape(16.dp))
    )
}

@Composable
fun SplashScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "splash_animation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkBg, Color(0xFF1E1035))
                )
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppLogo(scale = scale)

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "ARENA ESPORTS",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 2.sp
        )

        Text(
            text = "Tournament Hub & Gaming Arena",
            fontSize = 14.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        CircularProgressIndicator(
            color = CyanGlow,
            strokeWidth = 3.dp,
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
fun AuthScreen(viewModel: MainViewModel) {
    var isSignUp by remember { mutableStateOf(false) } // false = Login, true = SignUp
    var whatsapp by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var refCode by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .background(DarkBg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppLogo()

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Arena Esports",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isSignUp) "SignUp" else "Login",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSignUp) PurpleGlow else CyanGlow
        )

        Text(
            text = if (isSignUp) "Sign up to join tournament lobbies" else "Sign in to access your wallet and matches",
            fontSize = 13.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        // Segmented Tab Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .background(CardBg, RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (!isSignUp) CyanGlow else Color.Transparent)
                    .clickable { isSignUp = false }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Login",
                    color = if (!isSignUp) DarkBg else Color.LightGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSignUp) PurpleGlow else Color.Transparent)
                    .clickable { isSignUp = true }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sign Up",
                    color = if (isSignUp) Color.White else Color.LightGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        // WhatsApp number
        OutlinedTextField(
            value = whatsapp,
            onValueChange = { if (it.all { char -> char.isDigit() }) whatsapp = it },
            label = { Text("WhatsApp Number", color = Color.Gray) },
            placeholder = { Text("e.g. 9876543210") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone", tint = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isSignUp) PurpleGlow else CyanGlow,
                unfocusedBorderColor = BorderColor,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = if (isSignUp) PurpleGlow else CyanGlow,
                unfocusedLabelColor = Color.Gray,
                cursorColor = if (isSignUp) PurpleGlow else CyanGlow
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("whatsapp_input"),
            shape = RoundedCornerShape(12.dp)
        )

        if (isSignUp) {
            Spacer(modifier = Modifier.height(16.dp))

            // Name Field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name", color = Color.Gray) },
                placeholder = { Text("Enter your name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name", tint = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurpleGlow,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = PurpleGlow,
                    unfocusedLabelColor = Color.Gray,
                    cursorColor = PurpleGlow
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("name_input"),
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Password input
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password", color = Color.Gray) },
            placeholder = { Text("Enter password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password", tint = Color.Gray) },
            trailingIcon = {
                val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = "Toggle password visibility", tint = Color.Gray)
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isSignUp) PurpleGlow else CyanGlow,
                unfocusedBorderColor = BorderColor,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = if (isSignUp) PurpleGlow else CyanGlow,
                unfocusedLabelColor = Color.Gray,
                cursorColor = if (isSignUp) PurpleGlow else CyanGlow
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("password_input"),
            shape = RoundedCornerShape(12.dp)
        )

        if (isSignUp) {
            Spacer(modifier = Modifier.height(16.dp))

            // Confirm Password Input (Again Password)
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Again Password", color = Color.Gray) },
                placeholder = { Text("Verify password") },
                leadingIcon = { Icon(Icons.Default.EnhancedEncryption, contentDescription = "Confirm Password", tint = Color.Gray) },
                trailingIcon = {
                    val image = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(imageVector = image, contentDescription = "Toggle password visibility", tint = Color.Gray)
                    }
                },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurpleGlow,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = PurpleGlow,
                    unfocusedLabelColor = Color.Gray,
                    cursorColor = PurpleGlow
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("confirm_password_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Referral Code input
            OutlinedTextField(
                value = refCode,
                onValueChange = { refCode = it },
                label = { Text("Referral Code (Optional)", color = Color.Gray) },
                placeholder = { Text("Enter 8-digit code") },
                leadingIcon = { Icon(Icons.Default.CardGiftcard, contentDescription = "Referral", tint = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurpleGlow,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = PurpleGlow,
                    unfocusedLabelColor = Color.Gray,
                    cursorColor = PurpleGlow
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("referral_input"),
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (isSignUp) {
                    viewModel.performSignUp(whatsapp, name, password, confirmPassword, refCode.ifBlank { null })
                } else {
                    viewModel.performLogin(whatsapp, password)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = if (isSignUp) PurpleGlow else CyanGlow),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("auth_submit_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (isSignUp) "Register & Sign Up" else "Sign In & Enter",
                color = if (isSignUp) Color.White else DarkBg,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun HomeScreen(viewModel: MainViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val user by viewModel.loggedInUser.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        // App Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "ARENA ESPORTS",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Welcome: ${user?.whatsappNumber ?: "Loading..."}",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 180.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Wallet Quick View
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkBg),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Bal: ₹${"%.2f".format((user?.depositBalance ?: 0.0) + (user?.withdrawalBalance ?: 0.0))}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGlow
                        )
                    }
                }

                // ADMIN Switch Indicator (Super easy access to try Admin features)
                Card(
                    colors = CardDefaults.cardColors(containerColor = PurpleGlow),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clickable { viewModel.setScreen("admin") }
                        .testTag("admin_panel_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Admin Area",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ADMIN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = BorderColor, thickness = 1.dp)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTab) {
                0 -> EsportsSection(viewModel)
                1 -> CasinoSection(viewModel)
                2 -> GameHistorySection(viewModel)
                3 -> MenuSection(viewModel)
            }
        }

        // Custom Navigation Bar adhering to M3 active pill styling
        NavigationBar(
            containerColor = CardBg,
            tonalElevation = 8.dp
        ) {
            NavigationBarItem(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                label = { Text("Esports", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.SportsEsports, contentDescription = "Esports") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = DarkBg,
                    selectedTextColor = CyanGlow,
                    indicatorColor = CyanGlow,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                ),
                modifier = Modifier.testTag("nav_esports")
            )
            NavigationBarItem(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                label = { Text("Casino", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Casino, contentDescription = "Casino") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = DarkBg,
                    selectedTextColor = PurpleGlow,
                    indicatorColor = PurpleGlow,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                ),
                modifier = Modifier.testTag("nav_casino")
            )
            NavigationBarItem(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                label = { Text("History", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Game History") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = DarkBg,
                    selectedTextColor = AmberGlow,
                    indicatorColor = AmberGlow,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                ),
                modifier = Modifier.testTag("nav_history")
            )
            NavigationBarItem(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                label = { Text("Menu", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Menu, contentDescription = "Menu") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = DarkBg,
                    selectedTextColor = Color.White,
                    indicatorColor = Color.White,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                ),
                modifier = Modifier.testTag("nav_menu")
            )
        }
    }
}

@Composable
fun EsportsSection(viewModel: MainViewModel) {
    val tournaments by viewModel.allTournaments.collectAsStateWithLifecycle()
    val selectedGame by viewModel.selectedEsportsGame.collectAsStateWithLifecycle()
    var selectedTournamentForReg by remember { mutableStateOf<Tournament?>(null) }

    // Check if the user is registered for ANY tournament
    val registeredTournaments = tournaments.filter { it.isJoined }
    val latestJoined = registeredTournaments.lastOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // --- UPCOMING TOURNAMENT ROOM DETAILS BOX ---
        // Just above the game selection
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .border(1.dp, if (latestJoined != null) PurpleGlow else BorderColor, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🎯 Match Room Update",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (latestJoined != null) PurpleGlow else Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (latestJoined == null) {
                    Text(
                        text = "No upcoming match",
                        fontSize = 14.sp,
                        color = Color.LightGray
                    )
                } else {
                    if (latestJoined.roomId.isNullOrBlank()) {
                        Text(
                            text = "You will receive update details of tournament shortly.",
                            fontSize = 14.sp,
                            color = CyanGlow,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Column {
                            Text(
                                text = "Tournament: ${latestJoined.title}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Room ID: ${latestJoined.roomId}",
                                    fontSize = 13.sp,
                                    color = CyanGlow,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Password: ${latestJoined.roomPassword}",
                                    fontSize = 13.sp,
                                    color = CyanGlow,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Start Time: ${latestJoined.startTime}",
                                fontSize = 13.sp,
                                color = AmberGlow,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Game Selection Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("BGMI", "PUBG", "FREEFIRE").forEach { game ->
                val isSelected = selectedGame == game
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) CyanGlow else CardBg
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.selectEsportsGame(game) }
                        .border(
                            1.dp,
                            if (isSelected) Color.Transparent else BorderColor,
                            RoundedCornerShape(20.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = game,
                            color = if (isSelected) DarkBg else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Tournaments List
        val filteredTournaments = tournaments.filter { it.game == selectedGame }

        if (filteredTournaments.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "No Match",
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No tournaments available", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredTournaments) { tournament ->
                    TournamentItemTile(
                        tournament = tournament,
                        onRegisterClick = { selectedTournamentForReg = tournament }
                    )
                }
            }
        }
    }

    // Register details sheet controlled by admin
    if (selectedTournamentForReg != null) {
        val tour = selectedTournamentForReg!!
        Dialog(onDismissRequest = { selectedTournamentForReg = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, CyanGlow, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Registration Details",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = tour.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CyanGlow
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Small Font detailed breakdown
                    Text(
                        text = "ENTRY FEE: ₹${tour.entryFee}",
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "TOTAL PRIZE POOL: ₹${tour.prizePool}",
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "1ST PRIZE: ₹${tour.prize1st}",
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "2ND PRIZE: ₹${tour.prize2nd}",
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "3RD PRIZE: ₹${tour.prize3rd}",
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "4TH PRIZE: ₹${tour.prize4th}",
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = BorderColor)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "RULES & INSTRUCTIONS (By Admin)",
                        fontSize = 11.sp,
                        color = PurpleGlow,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tour.rules,
                        fontSize = 10.sp,
                        color = Color.Gray,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { selectedTournamentForReg = null },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.verticalGradient(listOf(BorderColor, BorderColor))),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", fontSize = 14.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.registerTournament(tour.id)
                                selectedTournamentForReg = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("confirm_register_btn")
                        ) {
                            Text("Confirm", color = DarkBg, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Tile is available in vertical, left side has poster, tournament name at top, and statistics on right
@Composable
fun TournamentItemTile(tournament: Tournament, onRegisterClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster Representation with fallbacks
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            colors = when (tournament.game) {
                                "BGMI" -> listOf(Color(0xFFFF5252), Color(0xFFFF7A00))
                                "FREEFIRE" -> listOf(Color(0xFFE040FB), Color(0xFF00E5FF))
                                else -> listOf(Color(0xFF00E676), Color(0xFF00B0FF))
                            }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Gamepad,
                        contentDescription = "Game Poster",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tournament.game,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tournament.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row {
                    Text(text = "Entry Fee: ", fontSize = 12.sp, color = Color.Gray)
                    Text(text = "₹${tournament.entryFee}", fontSize = 12.sp, color = CyanGlow, fontWeight = FontWeight.Bold)
                }

                Row {
                    Text(text = "Prize Pool: ", fontSize = 12.sp, color = Color.Gray)
                    Text(text = "₹${tournament.prizePool}", fontSize = 12.sp, color = EmeraldGlow, fontWeight = FontWeight.Bold)
                }

                // Grid detail layout
                Text(
                    text = "1st: ₹${tournament.prize1st} | 2nd: ₹${tournament.prize2nd} | 3rd: ₹${tournament.prize3rd}",
                    fontSize = 10.sp,
                    color = Color.LightGray,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (tournament.isJoined) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BorderColor),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "REGISTERED",
                            color = EmeraldGlow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = onRegisterClick,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("register_btn_${tournament.id}"),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(text = "Register", color = DarkBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CasinoSection(viewModel: MainViewModel) {
    val casinoGames = listOf(
        Pair("Ludo", Icons.Default.TableChart),
        Pair("Mines", Icons.Default.Whatshot),
        Pair("Chess", Icons.Default.GridOn),
        Pair("Jili", Icons.Default.Stars),
        Pair("Carrom", Icons.Default.Circle)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "🕹️ Arcade & Casino",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Instant multiplayer games with massive payouts",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(casinoGames) { game ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.showToast("Upcoming: ${game.first} game will be live shortly!") }
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Poster in small size
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.radialGradient(listOf(PurpleGlow.copy(alpha = 0.5f), DarkBg))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = game.second,
                                contentDescription = game.first,
                                tint = PurpleGlow,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = game.first,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "High multiplayer stakes",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkBg),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.border(1.dp, BorderColor, RoundedCornerShape(6.dp))
                        ) {
                            Text(
                                text = "UPCOMING",
                                color = AmberGlow,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GameHistorySection(viewModel: MainViewModel) {
    val histories by viewModel.gameHistories.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "🏆 Game History",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "View the outcomes and prizes of your registered matches",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (histories.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tournament history present.",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(histories) { history ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = history.gameName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Status: ${history.status}",
                                    fontSize = 12.sp,
                                    color = if (history.status == "PENDING") AmberGlow else EmeraldGlow
                                )
                            }

                            if (history.status == "PENDING") {
                                Text(
                                    text = "Pending",
                                    fontSize = 14.sp,
                                    color = AmberGlow,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    text = "+ ₹${history.prizeWon ?: 0.0}",
                                    fontSize = 15.sp,
                                    color = EmeraldGlow,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MenuSection(viewModel: MainViewModel) {
    val user by viewModel.loggedInUser.collectAsStateWithLifecycle()
    val personalTx by viewModel.personalTransactions.collectAsStateWithLifecycle()
    val limit by viewModel.txLimit.collectAsStateWithLifecycle()

    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current

    var depositAmount by remember { mutableStateOf("") }
    var depositRef by remember { mutableStateOf("") }

    var withdrawAmount by remember { mutableStateOf("") }
    var withdrawUpi by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- DEPOSIT MONEY ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "💳 Deposit Funds",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBg, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("PAY VIA UPI ID:", fontSize = 11.sp, color = Color.Gray)
                            Text("pay.arenaesports@upi", fontSize = 14.sp, color = CyanGlow, fontWeight = FontWeight.Bold)
                        }
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString("pay.arenaesports@upi"))
                                viewModel.showToast("UPI ID Copied to Clipboard!")
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = CyanGlow)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = depositAmount,
                        onValueChange = { depositAmount = it },
                        label = { Text("Amount (₹)", color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanGlow,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = depositRef,
                        onValueChange = { if (it.length <= 12 && it.all { char -> char.isDigit() }) depositRef = it },
                        label = { Text("12-Digit UPI Reference Number", color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanGlow,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val amt = depositAmount.toDoubleOrNull() ?: 0.0
                            viewModel.submitDeposit(amt, "pay.arenaesports@upi", depositRef)
                            depositAmount = ""
                            depositRef = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Submit Request", color = DarkBg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- WITHDRAWAL BALANCE ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📤 Withdraw Balance",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Withdrawal Balance:", fontSize = 13.sp, color = Color.Gray)
                        Text("₹${"%.2f".format(user?.withdrawalBalance ?: 0.0)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = EmeraldGlow)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = withdrawAmount,
                        onValueChange = { withdrawAmount = it },
                        label = { Text("Withdraw Amount (₹)", color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurpleGlow,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = withdrawUpi,
                        onValueChange = { withdrawUpi = it },
                        label = { Text("Your UPI ID (e.g., user@ybl)", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurpleGlow,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val amt = withdrawAmount.toDoubleOrNull() ?: 0.0
                            viewModel.submitWithdrawal(amt, withdrawUpi)
                            withdrawAmount = ""
                            withdrawUpi = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleGlow),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Submit Request", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- REFER AND EARN ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎁 Refer & Earn",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Share your referral code. Get ₹50.00 instantly upon their signup!",
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .background(DarkBg, RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .border(1.dp, PurpleGlow, RoundedCornerShape(8.dp)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = user?.ownReferralCode ?: "CODE1234",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PurpleGlow,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(user?.ownReferralCode ?: ""))
                                viewModel.showToast("Referral Code copied!")
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code", tint = PurpleGlow)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Referrals:", fontSize = 13.sp, color = Color.Gray)
                        Text("${user?.referredCount ?: 0} Players", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyanGlow)
                    }
                }
            }
        }

        // --- TRANSACTION HISTORY ---
        item {
            Text(
                text = "📜 Transaction History",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        if (personalTx.isEmpty()) {
            item {
                Text(
                    text = "No history available",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        } else {
            items(personalTx) { tx ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = tx.type,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Ref: ${tx.referenceNumber ?: "N/A"}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (tx.type == "WITHDRAWAL" || tx.type == "TOURNAMENT_ENTRY") "- ₹${tx.amount}" else "+ ₹${tx.amount}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (tx.type == "WITHDRAWAL" || tx.type == "TOURNAMENT_ENTRY") RedGlow else EmeraldGlow
                            )
                            Text(
                                text = tx.status,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (tx.status) {
                                    "PENDING" -> AmberGlow
                                    "APPROVED" -> EmeraldGlow
                                    else -> RedGlow
                                }
                            )
                        }
                    }
                }
            }

            // Load More button
            item {
                Button(
                    onClick = { viewModel.loadMoreTransactions() },
                    colors = ButtonDefaults.buttonColors(containerColor = BorderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Load More", color = Color.White)
                }
            }
        }

        // --- CUSTOMER ASSIST & SUPPORT ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🛠️ Customer Assist",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Need help with deposit or withdrawal? Contact our 24/7 support channels.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { uriHandler.openUri("https://wa.me/919999999999") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.SupportAgent, contentDescription = "WhatsApp")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("WhatsApp", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = { uriHandler.openUri("https://t.me/arenaesportssupport") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088CC)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Telegram")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Telegram", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // --- LOGOUT BUTTON ---
        item {
            Button(
                onClick = { viewModel.performLogout() },
                colors = ButtonDefaults.buttonColors(containerColor = RedGlow),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout From Account", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Comprehensive Admin Panel screen inside the app
@Composable
fun AdminPanelScreen(viewModel: MainViewModel) {
    var adminSection by remember { mutableIntStateOf(0) } // 0: Deposits, 1: Withdrawals, 2: Tournaments, 3: Complete Games
    val transactions by viewModel.allTransactionsAdmin.collectAsStateWithLifecycle()
    val tournaments by viewModel.allTournaments.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Top admin bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.setScreen("home") }) {
                    Icon(Icons.AutoMirrored.Filled.TrendingFlat, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Text(
                    text = "Admin Control Panel",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            TextButton(onClick = { viewModel.setScreen("home") }) {
                Text("EXIT ADMIN", color = CyanGlow, fontWeight = FontWeight.Bold)
            }
        }

        // Tabs
        ScrollableTabRow(
            selectedTabIndex = adminSection,
            containerColor = CardBg,
            contentColor = Color.White,
            edgePadding = 16.dp
        ) {
            Tab(selected = adminSection == 0, onClick = { adminSection = 0 }, text = { Text("Deposits") })
            Tab(selected = adminSection == 1, onClick = { adminSection = 1 }, text = { Text("Withdrawals") })
            Tab(selected = adminSection == 2, onClick = { adminSection = 2 }, text = { Text("Tournaments") })
            Tab(selected = adminSection == 3, onClick = { adminSection = 3 }, text = { Text("Win Declarer") })
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            when (adminSection) {
                0 -> {
                    // Deposits list
                    val deposits = transactions.filter { it.type == "DEPOSIT" }
                    if (deposits.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No deposit transactions found", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(deposits) { tx ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = CardBg),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Player: ${tx.whatsappNumber}", fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("Amount: ₹${tx.amount}", color = CyanGlow, fontWeight = FontWeight.Bold)
                                        Text("Ref Num: ${tx.referenceNumber}", color = Color.LightGray)
                                        Text("Status: ${tx.status}", color = if (tx.status == "PENDING") AmberGlow else EmeraldGlow)

                                        if (tx.status == "PENDING") {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 12.dp),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Button(
                                                    onClick = { viewModel.adminRejectDeposit(tx.id) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = RedGlow),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Reject")
                                                }
                                                Button(
                                                    onClick = { viewModel.adminApproveDeposit(tx.id, tx.amount, tx.whatsappNumber) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Approve", color = DarkBg, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Withdrawals list
                    val withdrawals = transactions.filter { it.type == "WITHDRAWAL" }
                    if (withdrawals.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No withdrawal transactions found", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(withdrawals) { tx ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = CardBg),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Player: ${tx.whatsappNumber}", fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("Amount: ₹${tx.amount}", color = PurpleGlow, fontWeight = FontWeight.Bold)
                                        Text("Receiver UPI: ${tx.upiId}", color = Color.LightGray)
                                        Text("Status: ${tx.status}", color = if (tx.status == "PENDING") AmberGlow else EmeraldGlow)

                                        if (tx.status == "PENDING") {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 12.dp),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Button(
                                                    onClick = { viewModel.adminRejectWithdrawal(tx.id, tx.amount, tx.whatsappNumber) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = RedGlow),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Reject")
                                                }
                                                Button(
                                                    onClick = { viewModel.adminApproveWithdrawal(tx.id) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Approve", color = DarkBg, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Manage Esports Room details
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(tournaments) { tour ->
                            var roomId by remember(tour.id) { mutableStateOf(tour.roomId ?: "") }
                            var roomPass by remember(tour.id) { mutableStateOf(tour.roomPassword ?: "") }
                            var startTime by remember(tour.id) { mutableStateOf(tour.startTime) }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = CardBg),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(tour.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                    Text("Game: ${tour.game}", color = CyanGlow, fontSize = 12.sp)

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = roomId,
                                        onValueChange = { roomId = it },
                                        label = { Text("Room ID", color = Color.Gray) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CyanGlow,
                                            unfocusedBorderColor = BorderColor,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = roomPass,
                                        onValueChange = { roomPass = it },
                                        label = { Text("Room Password", color = Color.Gray) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CyanGlow,
                                            unfocusedBorderColor = BorderColor,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = startTime,
                                        onValueChange = { startTime = it },
                                        label = { Text("Start Time", color = Color.Gray) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CyanGlow,
                                            unfocusedBorderColor = BorderColor,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            viewModel.adminUpdateTournament(tour.id, roomId.ifBlank { null }, roomPass.ifBlank { null }, startTime)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Save Match Credentials", color = DarkBg, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // Winning Declarer section to reward winner & Server Settings
                    var winnerWhatsapp by remember { mutableStateOf("") }
                    var prizeAwarded by remember { mutableStateOf("") }
                    var selectedGameName by remember { mutableStateOf("BGMI Sunday Championship") }
                    val context = androidx.compose.ui.platform.LocalContext.current
                    var serverUrlInput by remember { mutableStateOf(SupabaseClient.getServerUrl()) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardBg),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Declare Tournament Winners", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = selectedGameName,
                                    onValueChange = { selectedGameName = it },
                                    label = { Text("Tournament Game Name", color = Color.Gray) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PurpleGlow,
                                        unfocusedBorderColor = BorderColor,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = winnerWhatsapp,
                                    onValueChange = { winnerWhatsapp = it },
                                    label = { Text("Winner WhatsApp Number", color = Color.Gray) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PurpleGlow,
                                        unfocusedBorderColor = BorderColor,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = prizeAwarded,
                                    onValueChange = { prizeAwarded = it },
                                    label = { Text("Prize Award Amount (₹)", color = Color.Gray) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PurpleGlow,
                                        unfocusedBorderColor = BorderColor,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        val amt = prizeAwarded.toDoubleOrNull() ?: 0.0
                                        if (winnerWhatsapp.isBlank() || amt <= 0) {
                                            viewModel.showToast("Invalid winner details")
                                        } else {
                                            viewModel.adminCompleteGame(
                                                gameHistoryId = (1000..9999).random(),
                                                prizeWon = amt,
                                                winnerWhatsapp = winnerWhatsapp
                                            )
                                            winnerWhatsapp = ""
                                            prizeAwarded = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Distribute Prize Money", color = DarkBg, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardBg),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Backend Server Settings", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("This app communicates with a secure external Node/Express backend server to process authentication, tournaments registration, and wallets safely.", color = Color.Gray, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = serverUrlInput,
                                    onValueChange = { serverUrlInput = it },
                                    label = { Text("Server Base URL", color = Color.Gray) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PurpleGlow,
                                        unfocusedBorderColor = BorderColor,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        if (serverUrlInput.isNotBlank() && serverUrlInput.startsWith("http")) {
                                            SupabaseClient.setServerUrl(context, serverUrlInput)
                                            viewModel.showToast("Server URL updated successfully!")
                                        } else {
                                            viewModel.showToast("Invalid URL. Must start with http:// or https://")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PurpleGlow),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Save Server Configuration", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
