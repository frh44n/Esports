package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.launch
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

fun Tournament.getCustomRankPrizes(): Map<String, String>? {
    val customPrizesMatch = this.rules.substringAfter("\n\n--- Custom Rank Prizes ---\n", "")
    if (customPrizesMatch.isNotBlank() && customPrizesMatch != this.rules) {
        val actualCustomPart = customPrizesMatch.substringBefore("\n\n---")
        val lines = actualCustomPart.split("\n")
        var p1 = ""
        var p2 = ""
        var p3 = ""
        var p4 = ""
        lines.forEach { line ->
            if (line.startsWith("1st: ")) p1 = line.removePrefix("1st: ")
            if (line.startsWith("2nd: ")) p2 = line.removePrefix("2nd: ")
            if (line.startsWith("3rd: ")) p3 = line.removePrefix("3rd: ")
            if (line.startsWith("4th: ")) p4 = line.removePrefix("4th: ")
        }
        if (p1.isNotBlank() || p2.isNotBlank() || p3.isNotBlank() || p4.isNotBlank()) {
            return mapOf("1st" to p1, "2nd" to p2, "3rd" to p3, "4th" to p4)
        }
    }
    return null
}

fun Tournament.getCleanedRules(): String {
    return this.rules
        .substringBefore("\n\n--- Extra Prizes ---\n")
        .substringBefore("\n\n--- Custom Rank Prizes ---\n")
        .substringBefore("\n\n--- Point Table ---\n")
}

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
                .imePadding()
                .background(DarkBg)
        ) {
            when (currentScreen) {
                "splash" -> SplashScreen()
                "auth" -> AuthScreen(viewModel)
                "home" -> HomeScreen(viewModel)
                "admin" -> AdminPanelScreen(viewModel)
                "history" -> HistoryScreen(viewModel)
                "referral_details" -> ReferralDetailsScreen(viewModel)
                "ludo_tournaments" -> LudoTournamentsScreen(viewModel, onBack = { viewModel.setScreen("home") })
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val deviceId = remember {
        android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "unknown_device"
    }

    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

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
                    viewModel.performSignUp(whatsapp, name, password, confirmPassword, refCode.ifBlank { null }, deviceId)
                } else {
                    viewModel.performLogin(whatsapp, password)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = if (isSignUp) PurpleGlow else CyanGlow),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("auth_submit_button"),
            shape = RoundedCornerShape(12.dp),
            enabled = !isRefreshing
        ) {
            if (isRefreshing) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = if (isSignUp) Color.White else DarkBg,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (isSignUp) "Register & Sign Up" else "Sign In & Enter",
                    color = if (isSignUp) Color.White else DarkBg,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun HomeScreen(viewModel: MainViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val user by viewModel.loggedInUser.collectAsStateWithLifecycle()
    val notifications by viewModel.inAppNotifications.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val unreadCount = notifications.count { !it.isRead }
    var showNotificationCenter by remember { mutableStateOf(false) }
    val showMinesGame by viewModel.showMinesGame.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        // App Top Bar
        if (!(showMinesGame && selectedTab == 1)) {
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

                Column(horizontalAlignment = Alignment.End) {
                    // Notifications Bell Icon
                    Box(contentAlignment = Alignment.TopEnd) {
                        IconButton(
                            onClick = { showNotificationCenter = true },
                            modifier = Modifier.size(36.dp).testTag("notification_bell_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = if (unreadCount > 0) CyanGlow else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp, end = 2.dp)
                                    .size(14.dp)
                                    .background(Color.Red, RoundedCornerShape(7.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = unreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Wallet Quick View
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkBg),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
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
                }
            }
            HorizontalDivider(color = BorderColor, thickness = 1.dp)
        }

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

    if (showNotificationCenter) {
        NotificationCenterDialog(viewModel = viewModel, onDismiss = { showNotificationCenter = false })
    }
}

@Composable
fun NotificationCenterDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val notifications by viewModel.inAppNotifications.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current

    // Mark all as read when opening
    LaunchedEffect(Unit) {
        viewModel.markAllNotificationsAsRead()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔔 Notifications",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (notifications.isNotEmpty()) {
                    TextButton(
                        onClick = { viewModel.clearNotifications() }
                    ) {
                        Text("Clear All", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        text = {
            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = "No notifications",
                            tint = Color.Gray,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No updates yet. Check back later!",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(notifications) { notification ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkBg),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = notification.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (notification.title.contains("Room")) CyanGlow else PurpleGlow
                                    )
                                    
                                    val formattedTime = try {
                                        java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(notification.timestamp))
                                    } catch (e: Exception) {
                                        ""
                                    }
                                    Text(
                                        text = formattedTime,
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = notification.content,
                                    fontSize = 12.sp,
                                    color = Color.LightGray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Button(
                                    onClick = {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(notification.content))
                                        viewModel.showToast("Copied details!")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CardBg),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(30.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy",
                                            tint = CyanGlow,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copy Details", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyanGlow)
            ) {
                Text("Close", color = DarkBg, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = CardBg,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.border(1.dp, BorderColor, RoundedCornerShape(16.dp))
    )
}

@Composable
fun EsportsSection(viewModel: MainViewModel) {
    val tournaments by viewModel.allTournaments.collectAsStateWithLifecycle()
    val myRegistrations by viewModel.myRegistrations.collectAsStateWithLifecycle()
    val selectedGame by viewModel.selectedEsportsGame.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    var selectedTournamentForReg by remember { mutableStateOf<Tournament?>(null) }
    var selectedTournamentForInfo by remember { mutableStateOf<Tournament?>(null) }

    // Check if the user is registered for ANY tournament
    val registeredTournaments = tournaments.filter { it.isJoined && !it.startTime.contains("[FINISHED]") }
    val latestJoined = registeredTournaments.lastOrNull()
    val userRegistration = latestJoined?.let { tour -> myRegistrations.find { it.tournamentId == tour.id } }
    
    // Extract Slot if assigned
    var mySlot = ""
    if (userRegistration != null) {
        val parts = userRegistration.whatsappNumber.split("|")
        if (parts.size >= 4 && parts[3].startsWith("Slot")) {
            mySlot = parts[3]
        }
    }

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
                            text = "Room details will available shortly.",
                            fontSize = 14.sp,
                            color = CyanGlow,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Join by: (5 minutes early than start time).",
                            fontSize = 12.sp,
                            color = Color.LightGray
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
                            if (mySlot.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Your Slot: $mySlot",
                                    fontSize = 13.sp,
                                    color = EmeraldGlow,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Start Time: ${latestJoined.startTime.replace("[STARTED]", "").replace("[FINISHED]", "").trim()}",
                                fontSize = 13.sp,
                                color = AmberGlow,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Join by: (5 minutes early than start time).",
                                fontSize = 12.sp,
                                color = Color.LightGray
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
        val filteredTournaments = tournaments.filter { 
            it.game == selectedGame && 
            !it.startTime.contains("[STARTED]") && 
            !it.startTime.contains("[FINISHED]") 
        }

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
                        onRegisterClick = { selectedTournamentForReg = tournament },
                        onInfoClick = { selectedTournamentForInfo = tournament }
                    )
                }
            }
        }
    }

    // Info/Details sheet controlled by Info button
    if (selectedTournamentForInfo != null) {
        val tour = selectedTournamentForInfo!!
        Dialog(onDismissRequest = { selectedTournamentForInfo = null }) {
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
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Tournament Info",
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

                    Text(
                        text = "GAME: ${tour.game.uppercase()}",
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "START TIME: ${tour.startTime.replace("[STARTED]", "").replace("[FINISHED]", "").trim()}",
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Medium
                    )
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
                    val customRankPrizes = tour.getCustomRankPrizes()
                    if (customRankPrizes != null) {
                        val p1 = customRankPrizes["1st"] ?: ""
                        val p2 = customRankPrizes["2nd"] ?: ""
                        val p3 = customRankPrizes["3rd"] ?: ""
                        val p4 = customRankPrizes["4th"] ?: ""
                        if (p1.isNotBlank()) Text(text = "1ST PRIZE: $p1", fontSize = 12.sp, color = Color.LightGray, fontWeight = FontWeight.Medium)
                        if (p2.isNotBlank()) Text(text = "2ND PRIZE: $p2", fontSize = 12.sp, color = Color.LightGray, fontWeight = FontWeight.Medium)
                        if (p3.isNotBlank()) Text(text = "3RD PRIZE: $p3", fontSize = 12.sp, color = Color.LightGray, fontWeight = FontWeight.Medium)
                        if (p4.isNotBlank()) Text(text = "4TH PRIZE: $p4", fontSize = 12.sp, color = Color.LightGray, fontWeight = FontWeight.Medium)
                    } else {
                        Text(text = "1ST PRIZE: ₹${tour.prize1st}", fontSize = 12.sp, color = Color.LightGray, fontWeight = FontWeight.Medium)
                        Text(text = "2ND PRIZE: ₹${tour.prize2nd}", fontSize = 12.sp, color = Color.LightGray, fontWeight = FontWeight.Medium)
                        Text(text = "3RD PRIZE: ₹${tour.prize3rd}", fontSize = 12.sp, color = Color.LightGray, fontWeight = FontWeight.Medium)
                        Text(text = "4TH PRIZE: ₹${tour.prize4th}", fontSize = 12.sp, color = Color.LightGray, fontWeight = FontWeight.Medium)
                    }

                    val parts = tour.rules.split("\n\n--- Extra Prizes ---\n")
                    if (parts.size > 1) {
                        val extraPart = parts[1].substringBefore("\n\n--- Custom Rank Prizes ---\n").substringBefore("\n\n--- Point Table ---\n")
                        extraPart.split("\n").forEach { line ->
                            if (line.isNotBlank()) {
                                Text(
                                    text = line.uppercase(),
                                    fontSize = 12.sp,
                                    color = Color.LightGray,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = BorderColor)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "RULES & INSTRUCTIONS",
                        fontSize = 11.sp,
                        color = PurpleGlow,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tour.getCleanedRules(),
                        fontSize = 11.sp,
                        color = Color.Gray,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { selectedTournamentForInfo = null },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close", color = DarkBg, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Register details sheet showing ONLY registration fields
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
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Registration",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tour.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CyanGlow
                    )
                    Text(
                        text = "Entry Fee: ₹${tour.entryFee}",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    var teamName by remember { mutableStateOf("") }
                    var p1 by remember { mutableStateOf("") }
                    var p2 by remember { mutableStateOf("") }
                    var p3 by remember { mutableStateOf("") }
                    var p4 by remember { mutableStateOf("") }
                    var p5 by remember { mutableStateOf("") }
                    var p6 by remember { mutableStateOf("") }

                    val matchTypeMatch = "Match Type: (Solo|Duo|Squad)".toRegex().find(tour.rules)
                    val matchType = matchTypeMatch?.groupValues?.get(1) ?: "Squad"

                    Text(
                        text = "TEAM REGISTRATION (${matchType.uppercase()})",
                        fontSize = 11.sp,
                        color = CyanGlow,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (matchType != "Solo") {
                        OutlinedTextField(
                            value = teamName,
                            onValueChange = { teamName = it },
                            label = { Text("Team Name (Optional)", color = Color.Gray, fontSize = 11.sp) },
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
                    }

                    val mandatoryCount = when (matchType) {
                        "Solo" -> 1
                        "Duo" -> 2
                        else -> 4
                    }

                    Text(
                        text = "MANDATORY PLAYERS ($mandatoryCount)",
                        fontSize = 11.sp,
                        color = PurpleGlow,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = p1,
                        onValueChange = { p1 = it },
                        label = { Text(if (matchType == "Solo") "Player Name *" else "Player 1 Name (Leader) *", color = Color.Gray, fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanGlow,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (matchType == "Duo" || matchType == "Squad") {
                        OutlinedTextField(
                            value = p2,
                            onValueChange = { p2 = it },
                            label = { Text("Player 2 Name *", color = Color.Gray, fontSize = 11.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanGlow,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    if (matchType == "Squad") {
                        OutlinedTextField(
                            value = p3,
                            onValueChange = { p3 = it },
                            label = { Text("Player 3 Name *", color = Color.Gray, fontSize = 11.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanGlow,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = p4,
                            onValueChange = { p4 = it },
                            label = { Text("Player 4 Name *", color = Color.Gray, fontSize = 11.sp) },
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

                        Text(
                            text = "EXTRA PLAYERS (2 OPTIONAL)",
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = p5,
                            onValueChange = { p5 = it },
                            label = { Text("Player 5 Name (Optional)", color = Color.Gray, fontSize = 11.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanGlow,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = p6,
                            onValueChange = { p6 = it },
                            label = { Text("Player 6 Name (Optional)", color = Color.Gray, fontSize = 11.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanGlow,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

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
                            Text("Cancel", fontSize = 14.sp)
                        }

                        Button(
                            onClick = {
                                val isInvalid = when (matchType) {
                                    "Solo" -> p1.isBlank()
                                    "Duo" -> p1.isBlank() || p2.isBlank()
                                    else -> p1.isBlank() || p2.isBlank() || p3.isBlank() || p4.isBlank()
                                }

                                if (isInvalid) {
                                    viewModel.showToast("Please fill all mandatory players!")
                                } else {
                                    val playersList = when (matchType) {
                                        "Solo" -> listOf(p1.trim())
                                        "Duo" -> listOf(p1.trim(), p2.trim())
                                        else -> listOf(p1.trim(), p2.trim(), p3.trim(), p4.trim()) + listOf(p5.trim(), p6.trim()).filter { it.isNotBlank() }
                                    }
                                    val finalTeamName = if (matchType == "Solo") "Solo Player" else teamName.trim().ifBlank { "Unknown Team" }
                                    val teamMembersString = playersList.joinToString(", ")
                                    viewModel.registerTournamentWithTeam(tour.id, finalTeamName, teamMembersString) { success ->
                                        if (success) {
                                            selectedTournamentForReg = null
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("confirm_register_btn"),
                            enabled = !isRefreshing
                        ) {
                            if (isRefreshing) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    color = DarkBg,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Confirm", color = DarkBg, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Tile is available in vertical, left side has poster, tournament name at top, and statistics on right
@Composable
fun TournamentItemTile(
    tournament: Tournament,
    onRegisterClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            // Show tournament name in full box from left to right (Requirement 5)
            Text(
                text = tournament.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Show poster in left side of box in full size with AsyncImage support (Requirement 4)
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardBg),
                    contentAlignment = Alignment.Center
                ) {
                    val rawPoster = tournament.posterRes.trim()
                    val posterUrlToLoad = when {
                        rawPoster.startsWith("http://", ignoreCase = true) || rawPoster.startsWith("https://", ignoreCase = true) -> rawPoster
                        rawPoster.equals("bgmi", ignoreCase = true) -> "https://images.unsplash.com/photo-1542751371-adc38448a05e?auto=format&fit=crop&w=300&q=80"
                        rawPoster.equals("freefire", ignoreCase = true) -> "https://images.unsplash.com/photo-1511512578047-dfb367046420?auto=format&fit=crop&w=300&q=80"
                        rawPoster.equals("pubg", ignoreCase = true) -> "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?auto=format&fit=crop&w=300&q=80"
                        else -> "https://images.unsplash.com/photo-1550745165-9bc0b252726f?auto=format&fit=crop&w=300&q=80"
                    }

                    coil.compose.SubcomposeAsyncImage(
                        model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                            .data(posterUrlToLoad)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Tournament Poster",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(CardBg),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    color = CyanGlow,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            colors = when (tournament.game.uppercase()) {
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
                        }
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row {
                        Text(text = "Entry Fee: ", fontSize = 12.sp, color = Color.Gray)
                        Text(text = "₹${tournament.entryFee}", fontSize = 12.sp, color = CyanGlow, fontWeight = FontWeight.Bold)
                    }

                    Row {
                        Text(text = "Prize Pool: ", fontSize = 12.sp, color = Color.Gray)
                        Text(text = "₹${tournament.prizePool}", fontSize = 12.sp, color = EmeraldGlow, fontWeight = FontWeight.Bold)
                    }

                    // Start time and date instead of prize distribution list
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Start Time",
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tournament.startTime.replace("[STARTED]", "").replace("[FINISHED]", "").trim(),
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Info Button
                        OutlinedButton(
                            onClick = onInfoClick,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanGlow),
                            border = BorderStroke(1.dp, CyanGlow),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("info_btn_${tournament.id}"),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(text = "Info", color = CyanGlow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

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
    }
}

@Composable
fun CasinoSection(viewModel: MainViewModel) {
    val showMinesGame by viewModel.showMinesGame.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadCasinoGames()
    }

    if (showMinesGame) {
        MinesGameScreen(viewModel = viewModel, onBack = { viewModel.setShowMinesGame(false) })
        return
    }

    val tournaments by viewModel.allTournaments.collectAsStateWithLifecycle()
    val casinoGames by viewModel.casinoGames.collectAsStateWithLifecycle()
    var selectedCasinoTour by remember { mutableStateOf<Tournament?>(null) }

    // Filter tournaments created specifically as "Casino" game type (case-insensitive)
    val dbCasinoGames = remember(tournaments) {
        tournaments.filter {
            it.game.equals("Casino", ignoreCase = true) ||
            it.game.lowercase().contains("casino")
        }.sortedByDescending { it.id }
    }

    val ludoGame = remember(casinoGames) {
        casinoGames.firstOrNull { it.name.lowercase().contains("ludo") }
    }
    val minesGame = remember(casinoGames) {
        casinoGames.firstOrNull { it.name.lowercase().contains("mines") }
    }

    val ludoPoster = ludoGame?.posterUrl?.trim()?.ifBlank { null } ?: "https://images.unsplash.com/photo-1611195974226-a6a9be9dd763?auto=format&fit=crop&w=600&q=80"
    val ludoName = ludoGame?.name ?: "Ludo Classic"

    val minesPoster = minesGame?.posterUrl?.trim()?.ifBlank { null } ?: "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=600&q=80"
    val minesName = minesGame?.name ?: "Mines Sweeper"

    // Prepare exactly 2 games to display
    val game1 = dbCasinoGames.getOrNull(0)
    val game2 = dbCasinoGames.getOrNull(1)

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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Render Game 1
            item {
                CasinoGameCard(
                    gameName = ludoName,
                    posterUrl = ludoPoster,
                    subtext = "Play 1v1 & 2v2 tournaments, score points in real-time, and win prizes!",
                    isPlayable = true,
                    onClick = {
                        viewModel.setScreen("ludo_tournaments")
                    }
                )
            }

            // Render Game 2
            item {
                CasinoGameCard(
                    gameName = minesName,
                    posterUrl = minesPoster,
                    subtext = if (game2 != null) "Prize Pool: ₹${game2.prizePool} • Entry: ₹${game2.entryFee}" else "Predict safe spots, avoid explosives, multiply your stakes!",
                    isPlayable = true, // Force playable as we have integrated the live game!
                    onClick = {
                        viewModel.setShowMinesGame(true)
                    }
                )
            }
        }
    }

    // Self-contained registration modal inside CasinoSection
    if (selectedCasinoTour != null) {
        val tour = selectedCasinoTour!!
        Dialog(onDismissRequest = { selectedCasinoTour = null }) {
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
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Casino Registration",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tour.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CyanGlow
                    )
                    Text(
                        text = "Entry Fee: ₹${tour.entryFee} • Prize Pool: ₹${tour.prizePool}",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    var teamName by remember { mutableStateOf("") }
                    var p1 by remember { mutableStateOf("") }
                    var p2 by remember { mutableStateOf("") }
                    var p3 by remember { mutableStateOf("") }
                    var p4 by remember { mutableStateOf("") }

                    val matchTypeMatch = "Match Type: (Solo|Duo|Squad)".toRegex().find(tour.rules)
                    val matchType = matchTypeMatch?.groupValues?.get(1) ?: "Solo"

                    Text(
                        text = "REGISTRATION DETAILS (${matchType.uppercase()})",
                        fontSize = 11.sp,
                        color = CyanGlow,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (matchType != "Solo") {
                        OutlinedTextField(
                            value = teamName,
                            onValueChange = { teamName = it },
                            label = { Text("Team Name", color = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyanGlow, unfocusedBorderColor = BorderColor),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                    }

                    OutlinedTextField(
                        value = p1,
                        onValueChange = { p1 = it },
                        label = { Text("Player 1 Name (Leader)", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyanGlow, unfocusedBorderColor = BorderColor),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    if (matchType == "Duo" || matchType == "Squad") {
                        OutlinedTextField(
                            value = p2,
                            onValueChange = { p2 = it },
                            label = { Text("Player 2 Name", color = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyanGlow, unfocusedBorderColor = BorderColor),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                    }

                    if (matchType == "Squad") {
                        OutlinedTextField(
                            value = p3,
                            onValueChange = { p3 = it },
                            label = { Text("Player 3 Name", color = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyanGlow, unfocusedBorderColor = BorderColor),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = p4,
                            onValueChange = { p4 = it },
                            label = { Text("Player 4 Name", color = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyanGlow, unfocusedBorderColor = BorderColor),
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
                            onClick = { selectedCasinoTour = null },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, BorderColor),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", fontSize = 14.sp)
                        }

                        Button(
                            onClick = {
                                val isInvalid = when (matchType) {
                                    "Solo" -> p1.isBlank()
                                    "Duo" -> p1.isBlank() || p2.isBlank()
                                    else -> p1.isBlank() || p2.isBlank() || p3.isBlank() || p4.isBlank()
                                }

                                if (isInvalid) {
                                    viewModel.showToast("Please fill all players!")
                                } else {
                                    val playersList = when (matchType) {
                                        "Solo" -> listOf(p1.trim())
                                        "Duo" -> listOf(p1.trim(), p2.trim())
                                        else -> listOf(p1.trim(), p2.trim(), p3.trim(), p4.trim())
                                    }
                                    val finalTeamName = if (matchType == "Solo") "Solo Player" else teamName.trim().ifBlank { "Unknown Team" }
                                    val teamMembersString = playersList.joinToString(", ")
                                    viewModel.registerTournamentWithTeam(tour.id, finalTeamName, teamMembersString) { success ->
                                        if (success) {
                                            selectedCasinoTour = null
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Confirm", color = DarkBg, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CasinoGameCard(
    gameName: String,
    posterUrl: String,
    subtext: String,
    isPlayable: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable { onClick() }
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            coil.compose.AsyncImage(
                model = posterUrl,
                contentDescription = "$gameName Poster",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Dark subtle overlay gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                            startY = 100f
                        )
                    )
            )

            // Info text overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = gameName,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtext,
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }

            // Top right status badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(
                        if (isPlayable) EmeraldGlow.copy(alpha = 0.9f) else AmberGlow.copy(alpha = 0.9f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isPlayable) "PLAY NOW" else "UPCOMING",
                    color = DarkBg,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun GameHistorySection(viewModel: MainViewModel) {
    val histories by viewModel.gameHistories.collectAsStateWithLifecycle()
    val tournaments by viewModel.allTournaments.collectAsStateWithLifecycle()
    var showPointTableUrl by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // "only show earliest 10 game history" -> sorted by timestamp/id ascending (earliest first), taking 10.
    val earliestHistories = remember(histories) {
        histories.sortedBy { it.timestamp }.take(10)
    }

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
            text = "View the outcomes and prizes of your earliest 10 registered matches",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (earliestHistories.isEmpty()) {
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
                items(earliestHistories) { history ->
                    // Parse tournament details and metadata
                    val parts = history.gameName.split("|")
                    val cleanGameName = parts.getOrNull(0) ?: history.gameName
                    var tourId: Int? = null
                    var position: String? = null

                    for (i in 1 until parts.size) {
                        val part = parts[i].trim()
                        if (part.startsWith("tourId:")) {
                            tourId = part.substringAfter("tourId:").toIntOrNull()
                        } else if (part.startsWith("pos:")) {
                            position = part.substringAfter("pos:")
                        }
                    }

                    val associatedTour = tournaments.find { it.id == tourId }
                    val pointsTableUrl = associatedTour?.rules?.substringAfter("\n\n--- Point Table ---\n", "")?.ifBlank { null }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cleanGameName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Status: ${history.status}",
                                        fontSize = 12.sp,
                                        color = if (history.status == "PENDING") AmberGlow else EmeraldGlow
                                    )
                                    if (position != null) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Position: $position",
                                            fontSize = 12.sp,
                                            color = CyanGlow,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
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

                            if (pointsTableUrl != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { showPointTableUrl = pointsTableUrl },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = CyanGlow,
                                            containerColor = CyanGlow.copy(alpha = 0.05f)
                                        ),
                                        border = BorderStroke(1.dp, CyanGlow),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.List,
                                            contentDescription = "Point Table",
                                            modifier = Modifier.size(16.dp),
                                            tint = CyanGlow
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("View Point Table", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    IconButton(
                                        onClick = {
                                            downloadFile(
                                                context,
                                                pointsTableUrl,
                                                "points_table_${System.currentTimeMillis()}.jpg"
                                            )
                                        },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(CyanGlow.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                            .border(1.dp, CyanGlow, RoundedCornerShape(8.dp))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = "Download Point Table",
                                            tint = CyanGlow,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPointTableUrl != null) {
        Dialog(onDismissRequest = { showPointTableUrl = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "📊 Point Table",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        IconButton(
                            onClick = {
                                downloadFile(
                                    context,
                                    showPointTableUrl!!,
                                    "points_table_${System.currentTimeMillis()}.jpg"
                                )
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(CyanGlow.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download Point Table",
                                tint = CyanGlow,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    coil.compose.SubcomposeAsyncImage(
                        model = showPointTableUrl,
                        contentDescription = "Points Table",
                        loading = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = CyanGlow)
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Error Loading Photo",
                                    tint = Color.Red,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showPointTableUrl = null },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close", color = DarkBg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun downloadFile(context: android.content.Context, url: String, fileName: String) {
    try {
        val request = android.app.DownloadManager.Request(android.net.Uri.parse(url)).apply {
            setTitle(fileName)
            setDescription("Downloading Points Table...")
            setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, fileName)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }
        val downloadManager = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
        downloadManager.enqueue(request)
        android.widget.Toast.makeText(context, "Download started...", android.widget.Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Failed to start download: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}

@Composable
fun MenuSection(viewModel: MainViewModel) {
    val user by viewModel.loggedInUser.collectAsStateWithLifecycle()
    val personalTx by viewModel.personalTransactions.collectAsStateWithLifecycle()
    val limit by viewModel.txLimit.collectAsStateWithLifecycle()
    val currentUpiId by viewModel.dynamicUpiId.collectAsStateWithLifecycle()
    val globalSettings by viewModel.globalSettings.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current

    var depositAmount by remember { mutableStateOf("") }
    var depositRef by remember { mutableStateOf("") }

    var withdrawAmount by remember { mutableStateOf("") }
    var withdrawUpi by remember { mutableStateOf("") }

    var expandedSection by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- DEPOSIT MONEY ---
        item {
            MenuAccordionItem(
                title = "💳 Deposit Funds",
                isExpanded = expandedSection == "deposit",
                onToggle = { expandedSection = if (expandedSection == "deposit") null else "deposit" }
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        text = "Current Deposit Balance: ₹${user?.depositBalance ?: 0.0}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGlow,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
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
                            Text(currentUpiId, fontSize = 14.sp, color = CyanGlow, fontWeight = FontWeight.Bold)
                        }
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(currentUpiId))
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
                            viewModel.submitDeposit(amt, currentUpiId, depositRef)
                            depositAmount = ""
                            depositRef = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isRefreshing
                    ) {
                        if (isRefreshing) {
                            androidx.compose.material3.CircularProgressIndicator(
                                color = DarkBg,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Submit Request", color = DarkBg, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- WITHDRAWAL BALANCE ---
        item {
            MenuAccordionItem(
                title = "📤 Withdraw Balance",
                isExpanded = expandedSection == "withdraw",
                onToggle = { expandedSection = if (expandedSection == "withdraw") null else "withdraw" }
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
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
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isRefreshing
                    ) {
                        if (isRefreshing) {
                            androidx.compose.material3.CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Submit Request", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- REFER AND EARN ---
        item {
            MenuAccordionItem(
                title = "🎁 Refer & Earn",
                isExpanded = expandedSection == "refer",
                onToggle = { expandedSection = if (expandedSection == "refer") null else "refer" }
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Share your referral code. Get ₹${globalSettings.referralReward} when your referred friend deposits a minimum of ₹${globalSettings.referralMinDeposit}!",
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

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setScreen("referral_details") },
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Total Joined Users", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Click to view details and deposits", fontSize = 11.sp, color = Color.Gray)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${user?.referredCount ?: 0} Players", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyanGlow)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "View Details", tint = CyanGlow)
                            }
                        }
                    }
                }
            }
        }

        // --- TRANSACTION HISTORY ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .clickable { viewModel.setScreen("history") }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📜 Transaction History",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Open History",
                        tint = Color.Gray
                    )
                }
            }
        }

        // --- CUSTOMER ASSIST & SUPPORT ---
        item {
            MenuAccordionItem(
                title = "🛠️ Customer Assist",
                isExpanded = expandedSection == "support",
                onToggle = { expandedSection = if (expandedSection == "support") null else "support" }
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
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
                            onClick = { uriHandler.openUri(globalSettings.waUrl) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.SupportAgent, contentDescription = "WhatsApp")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("WhatsApp", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = { uriHandler.openUri(globalSettings.tgUrl) },
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

        // --- ADMIN PANEL ACCESS (Conditional) ---
        if (user?.whatsappNumber == "6202778501") {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyanGlow, RoundedCornerShape(12.dp))
                        .clickable { viewModel.setScreen("admin") }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🛠️ Admin Control Panel",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanGlow
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Open Admin Panel",
                            tint = CyanGlow
                        )
                    }
                }
            }
        }

        // --- LOGOUT BUTTON ---
        item {
            Button(
                onClick = { viewModel.performLogout() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .border(1.dp, RedGlow, RoundedCornerShape(8.dp)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Logout, contentDescription = "Logout", tint = RedGlow)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout From Account", color = RedGlow, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MenuAccordionItem(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .clickable { onToggle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Toggle",
                    tint = Color.Gray
                )
            }
            if (isExpanded) {
                content()
            }
        }
    }
}

// Comprehensive Admin Panel screen inside the app
@Composable fun HistoryScreen(viewModel: MainViewModel) {}
@Composable fun ReferralDetailsScreen(viewModel: MainViewModel) {}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(viewModel: MainViewModel) {
    val ludoRequests by viewModel.ludoMatchRequests.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        while(true) {
            viewModel.fetchAdminLudoRequests()
            kotlinx.coroutines.delay(2000)
        }
    }
    val adminActiveTournament by viewModel.adminActiveLudoTournament.collectAsStateWithLifecycle()
    val adminActiveOpponentName by viewModel.adminActiveLudoOpponentName.collectAsStateWithLifecycle()

    if (adminActiveTournament != null) {
        LudoGameManager(
            viewModel = viewModel,
            tournament = adminActiveTournament!!,
            opponentName = adminActiveOpponentName,
            onBack = {
                viewModel.endAdminLudoMatch()
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        TopAppBar(
            title = { Text("Admin Panel", color = Color.White) },
            navigationIcon = {
                IconButton(onClick = { viewModel.setScreen("home") }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBg)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Text(
                    "Live Ludo Players",
                    color = PurpleGlow,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            if (ludoRequests.isEmpty()) {
                item {
                    Text("No live players currently waiting.", color = Color.Gray)
                }
            } else {
                items(ludoRequests) { req ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(req.userName, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Waiting for match...", color = CyanGlow, fontSize = 12.sp)
                            }
                            Button(
                                onClick = { viewModel.adminJoinLudoMatch(req) },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow)
                            ) {
                                Text("Join Now", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
