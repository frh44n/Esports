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
    var showMinesGame by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadCasinoGames()
    }

    if (showMinesGame) {
        MinesGameScreen(viewModel = viewModel, onBack = { showMinesGame = false })
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
                    subtext = if (game1 != null) "Prize Pool: ₹${game1.prizePool} • Entry: ₹${game1.entryFee}" else "Instant multiplayer board game with massive payouts!",
                    isPlayable = game1 != null,
                    onClick = {
                        if (game1 != null) {
                            selectedCasinoTour = game1
                        } else {
                            viewModel.showToast("Upcoming: $ludoName game will be live shortly!")
                        }
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
                        showMinesGame = true
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
@Composable
fun AdminPanelScreen(viewModel: MainViewModel) {
    var adminSection by remember { mutableIntStateOf(0) } // 0: Deposits, 1: Withdrawals, 2: Settings, 3: Users, 4: Tournaments, 5: Teams & Rewards, 6: Tour Search
    val transactions by viewModel.allTransactionsAdmin.collectAsStateWithLifecycle()
    val tournaments by viewModel.allTournaments.collectAsStateWithLifecycle()
    val searchedUser by viewModel.searchedUser.collectAsStateWithLifecycle()
    val currentRegistrations by viewModel.currentTournamentRegistrations.collectAsStateWithLifecycle()
    val dynamicUpiId by viewModel.dynamicUpiId.collectAsStateWithLifecycle()
    val globalSettings by viewModel.globalSettings.collectAsStateWithLifecycle()
    val operatingTxIds by viewModel.operatingTxIds.collectAsStateWithLifecycle()

    LaunchedEffect(adminSection) {
        viewModel.refreshOnlineData(silent = true)
    }

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

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.refreshOnlineData() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh Data", tint = CyanGlow, modifier = Modifier.size(24.dp))
                }
                TextButton(onClick = { viewModel.setScreen("home") }) {
                    Text("EXIT", color = CyanGlow, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Tabs
        ScrollableTabRow(
            selectedTabIndex = adminSection,
            containerColor = CardBg,
            contentColor = Color.White,
            edgePadding = 16.dp
        ) {
            Tab(selected = adminSection == 0, onClick = { adminSection = 0 }, text = { Text("Deposits", fontSize = 12.sp) })
            Tab(selected = adminSection == 1, onClick = { adminSection = 1 }, text = { Text("Withdrawals", fontSize = 12.sp) })
            Tab(selected = adminSection == 2, onClick = { adminSection = 2 }, text = { Text("Settings", fontSize = 12.sp) })
            Tab(selected = adminSection == 3, onClick = { adminSection = 3 }, text = { Text("Users", fontSize = 12.sp) })
            Tab(selected = adminSection == 4, onClick = { adminSection = 4 }, text = { Text("Tournaments", fontSize = 12.sp) })
            Tab(selected = adminSection == 5, onClick = { adminSection = 5 }, text = { Text("Teams & Rewards", fontSize = 12.sp) })
            Tab(selected = adminSection == 6, onClick = { adminSection = 6 }, text = { Text("Tour Search", fontSize = 12.sp) })
            Tab(selected = adminSection == 7, onClick = { adminSection = 7 }, text = { Text("Records", fontSize = 12.sp) })
            Tab(selected = adminSection == 8, onClick = { adminSection = 8 }, text = { Text("Casino Games", fontSize = 12.sp) })
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
                                                val isTxLoading = operatingTxIds.contains(tx.id)
                                                Button(
                                                    onClick = { viewModel.adminRejectDeposit(tx.id) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = RedGlow),
                                                    modifier = Modifier.weight(1f),
                                                    enabled = !isTxLoading
                                                ) {
                                                    Text("Reject")
                                                }
                                                Button(
                                                    onClick = { viewModel.adminApproveDeposit(tx.id, tx.amount, tx.whatsappNumber) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow),
                                                    modifier = Modifier.weight(1f),
                                                    enabled = !isTxLoading
                                                ) {
                                                    if (isTxLoading) {
                                                        androidx.compose.material3.CircularProgressIndicator(
                                                            modifier = Modifier.size(18.dp),
                                                            color = DarkBg,
                                                            strokeWidth = 2.dp
                                                        )
                                                    } else {
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
                                                val isTxLoading = operatingTxIds.contains(tx.id)
                                                Button(
                                                    onClick = { viewModel.adminRejectWithdrawal(tx.id, tx.amount, tx.whatsappNumber) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = RedGlow),
                                                    modifier = Modifier.weight(1f),
                                                    enabled = !isTxLoading
                                                ) {
                                                    Text("Reject")
                                                }
                                                Button(
                                                    onClick = { viewModel.adminApproveWithdrawal(tx.id) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow),
                                                    modifier = Modifier.weight(1f),
                                                    enabled = !isTxLoading
                                                ) {
                                                    if (isTxLoading) {
                                                        androidx.compose.material3.CircularProgressIndicator(
                                                            modifier = Modifier.size(18.dp),
                                                            color = DarkBg,
                                                            strokeWidth = 2.dp
                                                        )
                                                     } else {
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
                }
                2 -> {
                    // Global Settings panel
                    var upiInput by remember(globalSettings) { mutableStateOf(globalSettings.upiId) }
                    var waUrlInput by remember(globalSettings) { mutableStateOf(globalSettings.waUrl) }
                    var tgUrlInput by remember(globalSettings) { mutableStateOf(globalSettings.tgUrl) }
                    var referralRewardInput by remember(globalSettings) { mutableStateOf(globalSettings.referralReward.toString()) }
                    var referralMinDepositInput by remember(globalSettings) { mutableStateOf(globalSettings.referralMinDeposit.toString()) }
                    var minesHouseEdgeSlider by remember(globalSettings) { mutableStateOf(globalSettings.minesHouseEdge.toFloat()) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardBg),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Manage Global Settings", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                Text("This is the UPI ID players will pay to during money deposits, and the URLs for social media support.", color = Color.Gray, fontSize = 12.sp)

                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = upiInput,
                                    onValueChange = { upiInput = it },
                                    label = { Text("Global UPI ID", color = Color.Gray) },
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

                                OutlinedTextField(
                                    value = waUrlInput,
                                    onValueChange = { waUrlInput = it },
                                    label = { Text("WhatsApp URL", color = Color.Gray) },
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

                                OutlinedTextField(
                                    value = tgUrlInput,
                                    onValueChange = { tgUrlInput = it },
                                    label = { Text("Telegram URL", color = Color.Gray) },
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

                                OutlinedTextField(
                                    value = referralRewardInput,
                                    onValueChange = { referralRewardInput = it },
                                    label = { Text("Referral Reward (₹)", color = Color.Gray) },
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

                                OutlinedTextField(
                                    value = referralMinDepositInput,
                                    onValueChange = { referralMinDepositInput = it },
                                    label = { Text("Min Deposit for Referral Reward (₹)", color = Color.Gray) },
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

                                Spacer(modifier = Modifier.height(16.dp))

                                Text("Mines Return to Player (RTP): ${minesHouseEdgeSlider.toInt()}%", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                Text("Controls house edge: 100% means perfectly fair payouts (0% house edge); 0% means maximum house edge (100% house edge).", color = Color.Gray, fontSize = 11.sp)
                                Slider(
                                    value = minesHouseEdgeSlider,
                                    onValueChange = { minesHouseEdgeSlider = it },
                                    valueRange = 0f..100f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = CyanGlow,
                                        activeTrackColor = CyanGlow,
                                        inactiveTrackColor = Color.DarkGray
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        if (upiInput.isNotBlank()) {
                                            val rwd = referralRewardInput.toDoubleOrNull() ?: 50.0
                                            val minDep = referralMinDepositInput.toDoubleOrNull() ?: 20.0
                                            viewModel.adminUpdateSettings(
                                                upiInput.trim(),
                                                waUrlInput.trim(),
                                                tgUrlInput.trim(),
                                                rwd,
                                                minDep,
                                                minesHouseEdgeSlider.toDouble()
                                            )
                                        } else {
                                            viewModel.showToast("UPI ID cannot be blank")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Update Settings", color = DarkBg, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // Manage Users panel
                    var searchPhone by remember { mutableStateOf("") }
                    var depositAmtInput by remember { mutableStateOf("") }
                    var withdrawalAmtInput by remember { mutableStateOf("") }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardBg),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Search Registered User", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = searchPhone,
                                    onValueChange = { searchPhone = it },
                                    label = { Text("WhatsApp Mobile Number", color = Color.Gray) },
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
                                    onClick = { viewModel.adminSearchUser(searchPhone) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Search User", color = DarkBg, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        searchedUser?.let { user ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CardBg),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("User Account: ${user.name}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                    Text("WhatsApp: ${user.whatsappNumber}", color = Color.Gray, fontSize = 12.sp)

                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = BorderColor)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Deposit Balance", fontSize = 11.sp, color = Color.Gray)
                                            Text("₹${user.depositBalance}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CyanGlow)
                                        }
                                        Column {
                                            Text("Withdrawal Balance", fontSize = 11.sp, color = Color.Gray)
                                            Text("₹${user.withdrawalBalance}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PurpleGlow)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    OutlinedTextField(
                                        value = depositAmtInput,
                                        onValueChange = { depositAmtInput = it },
                                        label = { Text("Set Deposit Balance (₹)", color = Color.Gray) },
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
                                        value = withdrawalAmtInput,
                                        onValueChange = { withdrawalAmtInput = it },
                                        label = { Text("Set Withdrawal Balance (₹)", color = Color.Gray) },
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

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            val dep = depositAmtInput.toDoubleOrNull() ?: user.depositBalance
                                            val wit = withdrawalAmtInput.toDoubleOrNull() ?: user.withdrawalBalance
                                            viewModel.adminUpdateUserBalance(user.whatsappNumber, dep, wit)
                                            depositAmtInput = ""
                                            withdrawalAmtInput = ""
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Update Balances", color = DarkBg, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
                4 -> {
                    // Manage Tournaments panel (Create, Edit, Delete)
                    var createMode by remember { mutableStateOf(false) }
                    var editingTourId by remember { mutableStateOf<Int?>(null) }

                    // Form Fields
                    var game by remember { mutableStateOf("BGMI") }
                    var title by remember { mutableStateOf("") }
                    var posterUrl by remember { mutableStateOf("") }
                    var entryFee by remember { mutableStateOf("") }
                    var prizePool by remember { mutableStateOf("") }
                    var prize1st by remember { mutableStateOf("") }
                    var prize2nd by remember { mutableStateOf("") }
                    var prize3rd by remember { mutableStateOf("") }
                    var prize4th by remember { mutableStateOf("") }
                    var maxTeams by remember { mutableStateOf("") }
                    var rules by remember { mutableStateOf("") }
                    var startTime by remember { mutableStateOf("") }
                    var extraPrizesList by remember { mutableStateOf(listOf<Pair<String, String>>()) }
                    var matchType by remember { mutableStateOf("Squad") }

                    val context = androidx.compose.ui.platform.LocalContext.current
                    val posterPicker = androidx.activity.compose.rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
                    ) { uri ->
                        uri?.let {
                            val helper = uriToBase64(context, it)
                            if (helper != null) {
                                viewModel.showToast("Uploading poster image to Supabase...")
                                viewModel.uploadPhoto(helper.first, "poster_${System.currentTimeMillis()}.jpg", helper.second) { url ->
                                    if (url != null) {
                                        posterUrl = url
                                        viewModel.showToast("Poster uploaded successfully!")
                                    }
                                }
                            } else {
                                viewModel.showToast("Failed to process picked image.")
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tournaments List", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Button(
                                onClick = {
                                    createMode = !createMode
                                    editingTourId = null
                                    if (createMode) {
                                        // clear values
                                        title = ""
                                        posterUrl = ""
                                        entryFee = ""
                                        prizePool = ""
                                        prize1st = ""
                                        prize2nd = ""
                                        prize3rd = ""
                                        prize4th = ""
                                        maxTeams = ""
                                        rules = ""
                                        startTime = ""
                                        matchType = "Squad"
                                        extraPrizesList = emptyList()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanGlow)
                            ) {
                                Text(if (createMode) "View All" else "+ Create New", color = DarkBg)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (createMode || editingTourId != null) {
                            // Edit or Create Form
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (editingTourId != null) "Edit Tournament ID: $editingTourId" else "Create New Tournament",
                                    color = CyanGlow,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )

                                OutlinedTextField(
                                    value = game,
                                    onValueChange = { game = it },
                                    label = { Text("Game Name (e.g. BGMI, PUBG, FREEFIRE)", color = Color.Gray) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyanGlow, unfocusedBorderColor = BorderColor),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Text("Match Type", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("Solo", "Duo", "Squad").forEach { type ->
                                        OutlinedButton(
                                            onClick = { matchType = type },
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = if (matchType == type) CyanGlow else Color.Gray,
                                                containerColor = if (matchType == type) CyanGlow.copy(alpha = 0.1f) else Color.Transparent
                                            ),
                                            border = BorderStroke(1.dp, if (matchType == type) CyanGlow else BorderColor),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(type, fontSize = 12.sp)
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = title,
                                    onValueChange = { title = it },
                                    label = { Text("Tournament Title", color = Color.Gray) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyanGlow, unfocusedBorderColor = BorderColor),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = posterUrl,
                                    onValueChange = { posterUrl = it },
                                    label = { Text("Poster Image URL / Link", color = Color.Gray) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyanGlow, unfocusedBorderColor = BorderColor),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = { posterPicker.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanGlow.copy(alpha = 0.15f), contentColor = CyanGlow),
                                    border = BorderStroke(1.dp, CyanGlow),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = "Upload Poster", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Upload Poster from Device", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }


                                OutlinedTextField(
                                    value = entryFee,
                                    onValueChange = { entryFee = it },
                                    label = { Text("Entry Fee (₹)", color = Color.Gray) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyanGlow, unfocusedBorderColor = BorderColor),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = prizePool,
                                    onValueChange = { prizePool = it },
                                    label = { Text("Total Prize Pool (₹)", color = Color.Gray) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyanGlow, unfocusedBorderColor = BorderColor),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                         value = prize1st,
                                         onValueChange = { prize1st = it },
                                         label = { Text("1st Prize", color = Color.Gray) },
                                         colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyanGlow, unfocusedBorderColor = BorderColor),
                                         singleLine = true,
                                         modifier = Modifier.weight(1f)
                                     )
                                    OutlinedTextField(
                                         value = prize2nd,
                                         onValueChange = { prize2nd = it },
                                         label = { Text("2nd Prize", color = Color.Gray) },
                                         colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyanGlow, unfocusedBorderColor = BorderColor),
                                         singleLine = true,
                                         modifier = Modifier.weight(1f)
                                     )
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                         value = prize3rd,
                                         onValueChange = { prize3rd = it },
                                         label = { Text("3rd Prize", color = Color.Gray) },
                                         colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyanGlow, unfocusedBorderColor = BorderColor),
                                         singleLine = true,
                                         modifier = Modifier.weight(1f)
                                     )
                                    OutlinedTextField(
                                         value = prize4th,
                                         onValueChange = { prize4th = it },
                                         label = { Text("4th Prize", color = Color.Gray) },
                                         colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyanGlow, unfocusedBorderColor = BorderColor),
                                         singleLine = true,
                                         modifier = Modifier.weight(1f)
                                     )
                                }

                                Text("Extra Positions (Optional)", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                extraPrizesList.forEachIndexed { index, pair ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        OutlinedTextField(
                                            value = pair.first,
                                            onValueChange = { newVal -> extraPrizesList = extraPrizesList.toMutableList().apply { set(index, newVal to pair.second) } },
                                            label = { Text("Pos (e.g. 5th)") },
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyanGlow, unfocusedBorderColor = BorderColor),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = pair.second,
                                            onValueChange = { newVal -> extraPrizesList = extraPrizesList.toMutableList().apply { set(index, pair.first to newVal) } },
                                            label = { Text("Prize (₹)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyanGlow, unfocusedBorderColor = BorderColor),
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = { extraPrizesList = extraPrizesList.toMutableList().apply { removeAt(index) } }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = RedGlow)
                                        }
                                    }
                                }
                                Button(onClick = { extraPrizesList = extraPrizesList + ("" to "") }, colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanGlow), border = BorderStroke(1.dp, CyanGlow), modifier = Modifier.fillMaxWidth()) {
                                    Text("+ Add Position")
                                }

                                OutlinedTextField(
                                    value = maxTeams,
                                    onValueChange = { maxTeams = it },
                                    label = { Text("Maximum Teams Allowed", color = Color.Gray) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyanGlow, unfocusedBorderColor = BorderColor),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = rules,
                                    onValueChange = { rules = it },
                                    label = { Text("Rules & Instructions", color = Color.Gray) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyanGlow, unfocusedBorderColor = BorderColor),
                                    minLines = 3,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = startTime,
                                    onValueChange = { startTime = it },
                                    label = { Text("Start Time (e.g. Tonight 09:00 PM)", color = Color.Gray) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyanGlow, unfocusedBorderColor = BorderColor),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        if (title.isBlank() || startTime.isBlank()) {
                                            viewModel.showToast("Title and Start Time are required fields")
                                            return@Button
                                        }
                                        val maxCount = maxTeams.toIntOrNull() ?: 100
                                         val extraPrizesStr = if (extraPrizesList.isNotEmpty()) {
                                             "\n\n--- Extra Prizes ---\n" + extraPrizesList.filter { it.first.isNotBlank() }.joinToString("\n") { "${it.first}: ₹${it.second}" }
                                         } else ""
                                         
                                         // Custom Rank Prizes Serialization
                                         val customRankPrizesStr = "\n\n--- Custom Rank Prizes ---\n" +
                                             "1st: $prize1st\n" +
                                             "2nd: $prize2nd\n" +
                                             "3rd: $prize3rd\n" +
                                             "4th: $prize4th"
                                             
                                         val combinedRules = "Match Type: $matchType\nMax Teams allowed: $maxCount\n${rules}${extraPrizesStr}${customRankPrizesStr}"

                                         // Extract numeric values for DB compatibility
                                         fun extractNum(t: String): Double {
                                             val regex = """\\d+""".toRegex()
                                             val match = regex.find(t)
                                             return match?.value?.toDoubleOrNull() ?: 0.0
                                         }

                                         if (editingTourId != null) {
                                             viewModel.adminUpdateTournamentDetails(
                                                 id = editingTourId!!,
                                                 game = game,
                                                 title = title,
                                                 posterRes = posterUrl,
                                                 entryFee = entryFee.toDoubleOrNull() ?: 0.0,
                                                 prizePool = prizePool.toDoubleOrNull() ?: 0.0,
                                                 prize1st = extractNum(prize1st),
                                                 prize2nd = extractNum(prize2nd),
                                                 prize3rd = extractNum(prize3rd),
                                                 prize4th = extractNum(prize4th),
                                                 rules = combinedRules,
                                                 startTime = startTime
                                             )
                                         } else {
                                             viewModel.adminCreateTournament(
                                                 Tournament(
                                                     game = game,
                                                     title = title,
                                                     posterRes = posterUrl.ifBlank { game.lowercase() },
                                                     entryFee = entryFee.toDoubleOrNull() ?: 0.0,
                                                     prizePool = prizePool.toDoubleOrNull() ?: 0.0,
                                                     prize1st = extractNum(prize1st),
                                                     prize2nd = extractNum(prize2nd),
                                                     prize3rd = extractNum(prize3rd),
                                                     prize4th = extractNum(prize4th),
                                                     rules = combinedRules,
                                                     startTime = startTime
                                                 )
                                             )
                                         }
                                         createMode = false
                                        editingTourId = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (editingTourId != null) "Update Tournament" else "Save & Create Tournament", color = DarkBg, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            // List mode
                            if (tournaments.isEmpty()) {
                                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text("No tournaments found", color = Color.Gray)
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f).fillMaxWidth()
                                ) {
                                    items(tournaments) { tour ->
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = CardBg),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                val isStarted = tour.startTime.contains("[STARTED]")
                                                val isFinished = tour.startTime.contains("[FINISHED]")
                                                val displayStartTime = tour.startTime.replace("[STARTED]", "").replace("[FINISHED]", "").trim()

                                                Text("[ID: ${tour.id}] ${tour.title}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                 ) {
                                                     Text("Game: ${tour.game} | Start: $displayStartTime", color = Color.Gray, fontSize = 11.sp)
                                                     if (isStarted) {
                                                         Card(
                                                             colors = CardDefaults.cardColors(containerColor = EmeraldGlow.copy(alpha = 0.2f)),
                                                             shape = RoundedCornerShape(4.dp),
                                                             border = BorderStroke(1.dp, EmeraldGlow)
                                                         ) {
                                                             Text(
                                                                 text = "STARTED",
                                                                 color = EmeraldGlow,
                                                                 fontSize = 9.sp,
                                                                 fontWeight = FontWeight.Bold,
                                                                 modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                             )
                                                         }
                                                     } else if (isFinished) {
                                                         Card(
                                                             colors = CardDefaults.cardColors(containerColor = Color.Gray.copy(alpha = 0.2f)),
                                                             shape = RoundedCornerShape(4.dp),
                                                             border = BorderStroke(1.dp, Color.Gray)
                                                         ) {
                                                             Text(
                                                                 text = "FINISHED",
                                                                 color = Color.Gray,
                                                                 fontSize = 9.sp,
                                                                 fontWeight = FontWeight.Bold,
                                                                 modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                             )
                                                         }
                                                     } else {
                                                         Card(
                                                             colors = CardDefaults.cardColors(containerColor = CyanGlow.copy(alpha = 0.2f)),
                                                             shape = RoundedCornerShape(4.dp),
                                                             border = BorderStroke(1.dp, CyanGlow)
                                                         ) {
                                                             Text(
                                                                 text = "UPCOMING",
                                                                 color = CyanGlow,
                                                                 fontSize = 9.sp,
                                                                 fontWeight = FontWeight.Bold,
                                                                 modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                             )
                                                         }
                                                     }
                                                 }
                                                Text("Fee: ₹${tour.entryFee} | Pool: ₹${tour.prizePool}", color = CyanGlow, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                                                Spacer(modifier = Modifier.height(10.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            // Populate edit
                                                            editingTourId = tour.id
                                                            game = tour.game
                                                            title = tour.title
                                                            posterUrl = tour.posterRes
                                                            entryFee = tour.entryFee.toString()
                                                            prizePool = tour.prizePool.toString()
                                                            // Extract custom rank prizes if present
                                                             val customRankPrizes = tour.getCustomRankPrizes()
                                                             if (customRankPrizes != null) {
                                                                 prize1st = customRankPrizes["1st"] ?: ""
                                                                 prize2nd = customRankPrizes["2nd"] ?: ""
                                                                 prize3rd = customRankPrizes["3rd"] ?: ""
                                                                 prize4th = customRankPrizes["4th"] ?: ""
                                                             } else {
                                                                 prize1st = tour.prize1st.toString()
                                                                 prize2nd = tour.prize2nd.toString()
                                                                 prize3rd = tour.prize3rd.toString()
                                                                 prize4th = tour.prize4th.toString()
                                                             }
                                                             startTime = tour.startTime

                                                             // Extract max teams if formatted
                                                             val regex = "Max Teams allowed: (\\d+)".toRegex()
                                                             val match = regex.find(tour.rules)
                                                             var parsedRules = tour.rules
                                                             if (match != null) {
                                                                 maxTeams = match.groupValues[1]
                                                                 parsedRules = tour.rules.replace("Max Teams allowed: ${maxTeams}\n", "")
                                                             } else {
                                                                 maxTeams = ""
                                                             }
                                                             
                                                             val matchTypeMatch = "Match Type: (Solo|Duo|Squad)".toRegex().find(parsedRules)
                                                             if (matchTypeMatch != null) {
                                                                 matchType = matchTypeMatch.groupValues[1]
                                                                 parsedRules = parsedRules.replace("Match Type: $matchType\n", "")
                                                             } else {
                                                                 matchType = "Squad"
                                                             }

                                                             // Strip point table link if editing
                                                             val baseRulesWithoutPt = parsedRules.substringBefore("\n\n--- Point Table ---\n")

                                                             // Strip custom rank prizes if editing
                                                             val baseRulesWithoutCustom = baseRulesWithoutPt.substringBefore("\n\n--- Custom Rank Prizes ---\n")

                                                             val parts = baseRulesWithoutCustom.split("\n\n--- Extra Prizes ---\n")
                                                             if (parts.size > 1) {
                                                                 rules = parts[0]
                                                                 extraPrizesList = parts[1].split("\n").map { line ->
                                                                     val p = line.split(": ₹")
                                                                     if (p.size == 2) p[0] to p[1] else "" to ""
                                                                 }.filter { it.first.isNotBlank() }
                                                             } else {
                                                                 rules = baseRulesWithoutCustom
                                                                 extraPrizesList = emptyList()
                                                             }
                                                        },
                                                                                                                 colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                                         border = BorderStroke(1.dp, BorderColor),
                                                         modifier = Modifier.weight(1f)
                                                     ) {
                                                         Text("Edit", fontSize = 11.sp)
                                                     }

                                                     Button(
                                                         onClick = { viewModel.adminDeleteTournament(tour.id) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = RedGlow),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text("Delete", fontSize = 11.sp)
                                                    }
                                                }

                                                if (!isFinished) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        if (!isStarted) {
                                                            Button(
                                                                onClick = { viewModel.adminStartTournament(tour.id) },
                                                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow),
                                                                modifier = Modifier.weight(1f)
                                                            ) {
                                                                Text("Start Match", color = DarkBg, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                            }
                                                        } else {
                                                            Button(
                                                                onClick = { viewModel.adminFinishTournament(tour.id) },
                                                                colors = ButtonDefaults.buttonColors(containerColor = AmberGlow),
                                                                modifier = Modifier.weight(1f)
                                                            ) {
                                                                Text("Mark Finished", color = DarkBg, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                            }
                        }
                    }
                }
                5 -> {
                    // Teams & Rewards panel (Manage Joined teams, Positions, Rewards and assign Slots range)
                    var selectedTourForTeams by remember { mutableStateOf<Tournament?>(null) }
                    var fromSlotText by remember { mutableStateOf("") }
                    var toSlotText by remember { mutableStateOf("") }
                    var roomIdText by remember { mutableStateOf("") }
                    var roomPassText by remember { mutableStateOf("") }

                    val context = androidx.compose.ui.platform.LocalContext.current
                    var uploadingPtTourId by remember { mutableStateOf<Int?>(null) }
                    val pointTablePicker = androidx.activity.compose.rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
                    ) { uri ->
                        uri?.let {
                            val helper = uriToBase64(context, it)
                            val targetTourId = uploadingPtTourId
                            if (helper != null && targetTourId != null) {
                                val tour = tournaments.find { t -> t.id == targetTourId }
                                if (tour != null) {
                                    viewModel.showToast("Uploading points table image...")
                                    viewModel.uploadPhoto(helper.first, "points_${targetTourId}_${System.currentTimeMillis()}.jpg", helper.second) { url ->
                                        if (url != null) {
                                            val baseRulesWithoutPt = tour.rules.substringBefore("\n\n--- Point Table ---\n")
                                            val newRules = baseRulesWithoutPt + "\n\n--- Point Table ---\n" + url
                                            viewModel.adminUpdateTournamentDetails(
                                                id = tour.id,
                                                game = tour.game,
                                                title = tour.title,
                                                posterRes = tour.posterRes,
                                                entryFee = tour.entryFee,
                                                prizePool = tour.prizePool,
                                                prize1st = tour.prize1st,
                                                prize2nd = tour.prize2nd,
                                                prize3rd = tour.prize3rd,
                                                prize4th = tour.prize4th,
                                                rules = newRules,
                                                startTime = tour.startTime
                                            )
                                            selectedTourForTeams = tour.copy(rules = newRules)
                                            viewModel.showToast("Points table uploaded successfully!")
                                        }
                                    }
                                }
                            } else {
                                viewModel.showToast("Failed to process picked image.")
                            }
                            uploadingPtTourId = null
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text("Select Tournament to View Joined Teams:", color = Color.Gray, fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(6.dp))

                        // Horizontally scrollable Tournament Picker using simple custom Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(tournaments) { tour ->
                                val isSelected = selectedTourForTeams?.id == tour.id
                                Box(
                                    modifier = Modifier
                                        .background(if (isSelected) CyanGlow else CardBg, RoundedCornerShape(16.dp))
                                        .border(1.dp, if (isSelected) CyanGlow else BorderColor, RoundedCornerShape(16.dp))
                                        .clickable {
                                            selectedTourForTeams = tour
                                            roomIdText = tour.roomId ?: ""
                                            roomPassText = tour.roomPassword ?: ""
                                            viewModel.adminFetchRegistrations(tour.id)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(tour.title, color = if (isSelected) DarkBg else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        selectedTourForTeams?.let { tour ->
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                item {
                                    Text("Tournament ID: ${tour.id} - ${tour.title}", color = CyanGlow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Total Teams Joined: ${currentRegistrations.size}", color = Color.White, fontSize = 12.sp)
                                }

                                // Room Details updater
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = CardBg),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("Declare Room Details", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                            Spacer(modifier = Modifier.height(8.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = roomIdText,
                                                    onValueChange = { roomIdText = it },
                                                    label = { Text("Room ID", fontSize = 11.sp, color = Color.Gray) },
                                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyanGlow, unfocusedBorderColor = BorderColor),
                                                    modifier = Modifier.weight(1f)
                                                )

                                                OutlinedTextField(
                                                    value = roomPassText,
                                                    onValueChange = { roomPassText = it },
                                                    label = { Text("Password", fontSize = 11.sp, color = Color.Gray) },
                                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyanGlow, unfocusedBorderColor = BorderColor),
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Button(
                                                onClick = {
                                                    viewModel.adminUpdateTournament(tour.id, roomIdText, roomPassText, tour.startTime)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Update Room Details", color = DarkBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }

                                // Slot range updater
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = CardBg),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("Bulk Assign Unique Slots Range", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                            Spacer(modifier = Modifier.height(8.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = fromSlotText,
                                                    onValueChange = { fromSlotText = it },
                                                    label = { Text("From Slot", fontSize = 11.sp, color = Color.Gray) },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyanGlow, unfocusedBorderColor = BorderColor),
                                                    modifier = Modifier.weight(1f)
                                                )

                                                OutlinedTextField(
                                                    value = toSlotText,
                                                    onValueChange = { toSlotText = it },
                                                    label = { Text("To Slot", fontSize = 11.sp, color = Color.Gray) },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyanGlow, unfocusedBorderColor = BorderColor),
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Button(
                                                onClick = {
                                                    val from = fromSlotText.toIntOrNull() ?: 1
                                                    val to = toSlotText.toIntOrNull() ?: 100
                                                    viewModel.adminAssignSlots(tour.id, from, to)
                                                    fromSlotText = ""
                                                    toSlotText = ""
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Assign Slots Range", color = DarkBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }

                                // Point Table manager inside Teams & Rewards section
                                item {
                                    val pointsTableUrl = tour.rules.substringAfter("\n\n--- Point Table ---\n", "").ifBlank { null }

                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = CardBg),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("📊 Tournament Point Table", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                            Spacer(modifier = Modifier.height(8.dp))

                                            if (pointsTableUrl != null) {
                                                coil.compose.SubcomposeAsyncImage(
                                                    model = pointsTableUrl,
                                                    contentDescription = "Current Point Table",
                                                    loading = {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(150.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            CircularProgressIndicator(color = CyanGlow)
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(max = 180.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(60.dp)
                                                        .background(DarkBg.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                                        .border(1.dp, BorderColor, RoundedCornerShape(6.dp)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        "No point table image uploaded yet.",
                                                        color = Color.Gray,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        uploadingPtTourId = tour.id
                                                        pointTablePicker.launch("image/*")
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(
                                                        text = if (pointsTableUrl == null) "Upload Point Table" else "Change Image",
                                                        color = DarkBg,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp
                                                    )
                                                }

                                                if (pointsTableUrl != null) {
                                                    Button(
                                                        onClick = {
                                                            val baseRulesWithoutPt = tour.rules.substringBefore("\n\n--- Point Table ---\n")
                                                            viewModel.adminUpdateTournamentDetails(
                                                                id = tour.id,
                                                                game = tour.game,
                                                                title = tour.title,
                                                                posterRes = tour.posterRes,
                                                                entryFee = tour.entryFee,
                                                                prizePool = tour.prizePool,
                                                                prize1st = tour.prize1st,
                                                                prize2nd = tour.prize2nd,
                                                                prize3rd = tour.prize3rd,
                                                                prize4th = tour.prize4th,
                                                                rules = baseRulesWithoutPt,
                                                                startTime = tour.startTime
                                                            )
                                                            selectedTourForTeams = tour.copy(rules = baseRulesWithoutPt)
                                                            viewModel.showToast("Points table removed successfully!")
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = RedGlow),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text("Remove", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                item {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("🏆 Prize Distribution & Joined Teams", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                if (currentRegistrations.isEmpty()) {
                                    item {
                                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                                            Text("No teams have joined this tournament yet", color = Color.Gray, fontSize = 12.sp)
                                        }
                                    }
                                } else {
                                    items(currentRegistrations) { reg ->
                                        // Decode formatted details
                                        val parts = reg.whatsappNumber.split("|")
                                        val rawWhatsapp = parts.getOrNull(0) ?: reg.whatsappNumber
                                        val teamName = parts.getOrNull(1) ?: "Solo"
                                        val members = parts.getOrNull(2) ?: "Solo Player"
                                        val slotNum = parts.getOrNull(3) ?: "Unassigned"
                                        val declaredPos = parts.getOrNull(4)
                                        val declaredPrize = parts.getOrNull(5)

                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = CardBg),
                                            border = BorderStroke(1.dp, BorderColor),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(teamName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        if (!declaredPos.isNullOrBlank()) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .background(EmeraldGlow, RoundedCornerShape(4.dp))
                                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                                            ) {
                                                                Text("$declaredPos - ₹$declaredPrize", color = DarkBg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                        Box(
                                                            modifier = Modifier
                                                                .background(PurpleGlow, RoundedCornerShape(4.dp))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(slotNum, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }

                                                Text("Leader Mobile: $rawWhatsapp", color = Color.LightGray, fontSize = 11.sp)
                                                Text("Players: $members", color = Color.Gray, fontSize = 11.sp)

                                                Spacer(modifier = Modifier.height(8.dp))
                                                HorizontalDivider(color = BorderColor)
                                                Spacer(modifier = Modifier.height(8.dp))

                                                // Award controls
                                                var declarePositionText by remember(declaredPos) { mutableStateOf(declaredPos ?: "1st") }
                                                var declarePrizeText by remember(declaredPrize) { mutableStateOf(declaredPrize ?: "") }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    OutlinedTextField(
                                                        value = declarePositionText,
                                                        onValueChange = { declarePositionText = it },
                                                        label = { Text("Pos", fontSize = 9.sp, color = Color.Gray) },
                                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyanGlow, unfocusedBorderColor = BorderColor),
                                                        modifier = Modifier.weight(0.4f)
                                                    )

                                                    OutlinedTextField(
                                                        value = declarePrizeText,
                                                        onValueChange = { declarePrizeText = it },
                                                        label = { Text("Prize (₹)", fontSize = 9.sp, color = Color.Gray) },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyanGlow, unfocusedBorderColor = BorderColor),
                                                        modifier = Modifier.weight(0.6f)
                                                    )

                                                    Button(
                                                        onClick = {
                                                            val prize = declarePrizeText.toDoubleOrNull() ?: 0.0
                                                            viewModel.adminDeclarePositionAndReward(
                                                                registrationId = reg.id,
                                                                position = declarePositionText,
                                                                prizeAmount = prize,
                                                                rawWhatsapp = rawWhatsapp,
                                                                tournamentTitle = tour.title
                                                            )
                                                            declarePrizeText = ""
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text("Reward", color = DarkBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } ?: Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("Please select a tournament above to get started.", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
                6 -> {
                    // Tournament details database search by Tournament ID
                    var searchIdText by remember { mutableStateOf("") }
                    var foundTour by remember { mutableStateOf<Tournament?>(null) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardBg),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Search Tournament Database", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = searchIdText,
                                    onValueChange = { searchIdText = it },
                                    label = { Text("Enter Tournament ID", color = Color.Gray) },
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
                                        val tid = searchIdText.toIntOrNull()
                                        if (tid != null) {
                                            foundTour = tournaments.find { it.id == tid }
                                            if (foundTour == null) {
                                                viewModel.showToast("No tournament with ID $tid found in active list.")
                                            }
                                        } else {
                                            viewModel.showToast("Please enter a valid numeric ID.")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Query Tournament ID", color = DarkBg, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        foundTour?.let { tour ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CardBg),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Tournament Data Sheet [ID: ${tour.id}]", fontWeight = FontWeight.Bold, color = CyanGlow, fontSize = 15.sp)
                                    Text("Title: ${tour.title}", color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("Game Category: ${tour.game}", color = Color.LightGray)
                                    Text("Entry Fee: ₹${tour.entryFee}", color = Color.LightGray)
                                    Text("Start Date/Time: ${tour.startTime.replace("[STARTED]", "").replace("[FINISHED]", "").trim()}", color = Color.LightGray)
                                    Text("Total Prize Pool: ₹${tour.prizePool}", color = Color.LightGray)

                                    Spacer(modifier = Modifier.height(6.dp))
                                    HorizontalDivider(color = BorderColor)
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text("Rank Prizes Structure:", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 12.sp)
                                     val customRankPrizes = tour.getCustomRankPrizes()
                                     if (customRankPrizes != null) {
                                         val p1 = customRankPrizes["1st"] ?: ""
                                         val p2 = customRankPrizes["2nd"] ?: ""
                                         val p3 = customRankPrizes["3rd"] ?: ""
                                         val p4 = customRankPrizes["4th"] ?: ""
                                         if (p1.isNotBlank()) Text("- 1st Prize Winner: $p1", color = Color.Gray, fontSize = 11.sp)
                                         if (p2.isNotBlank()) Text("- 2nd Prize Winner: $p2", color = Color.Gray, fontSize = 11.sp)
                                         if (p3.isNotBlank()) Text("- 3rd Prize Winner: $p3", color = Color.Gray, fontSize = 11.sp)
                                         if (p4.isNotBlank()) Text("- 4th Prize Winner: $p4", color = Color.Gray, fontSize = 11.sp)
                                     } else {
                                         Text("- 1st Prize Winner: ₹${tour.prize1st}", color = Color.Gray, fontSize = 11.sp)
                                         Text("- 2nd Prize Winner: ₹${tour.prize2nd}", color = Color.Gray, fontSize = 11.sp)
                                         Text("- 3rd Prize Winner: ₹${tour.prize3rd}", color = Color.Gray, fontSize = 11.sp)
                                         Text("- 4th Prize Winner: ₹${tour.prize4th}", color = Color.Gray, fontSize = 11.sp)
                                     }
                                     
                                     val parts = tour.rules.split("\n\n--- Extra Prizes ---\n")
                                     if (parts.size > 1) {
                                         val extraPart = parts[1].substringBefore("\n\n--- Custom Rank Prizes ---\n").substringBefore("\n\n--- Point Table ---\n")
                                         extraPart.split("\n").forEach { line ->
                                             if (line.isNotBlank()) {
                                                 Text("- $line", color = Color.Gray, fontSize = 11.sp)
                                             }
                                         }
                                     }

                                     Spacer(modifier = Modifier.height(6.dp))
                                     HorizontalDivider(color = BorderColor)
                                     Spacer(modifier = Modifier.height(6.dp))

                                     Text("Rules and Terms:", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 12.sp)
                                     Text(tour.getCleanedRules(), color = Color.Gray, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
                7 -> {
                    AdminRecordsScreen(viewModel)
                }
                8 -> {
                    AdminCasinoGamesScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun AdminCasinoGamesScreen(viewModel: MainViewModel) {
    val casinoGames by viewModel.casinoGames.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    var selectedGame by remember { mutableStateOf<com.example.data.CasinoGame?>(null) }
    var editingName by remember { mutableStateOf("") }
    var editingPosterUrl by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadCasinoGames()
    }

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            val helper = uriToBase64(context, it)
            if (helper != null) {
                scope.launch {
                    try {
                        isUploading = true
                        viewModel.showToast("Uploading poster image to Supabase...")
                        val uploadedUrl = viewModel.uploadCasinoPoster(helper.first, "casino_${selectedGame?.id ?: System.currentTimeMillis()}.jpg", helper.second)
                        isUploading = false
                        if (uploadedUrl != null) {
                            editingPosterUrl = uploadedUrl
                            viewModel.showToast("Poster uploaded successfully!")
                        } else {
                            viewModel.showToast("Upload failed")
                        }
                    } catch (e: Exception) {
                        isUploading = false
                        viewModel.showToast("Error processing upload: ${e.message}")
                    }
                }
            } else {
                viewModel.showToast("Failed to process picked image.")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "🎮 Manage Casino Posters",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Edit names and upload posters to Supabase Storage",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Select a Game to Edit",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CyanGlow,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                casinoGames.forEach { game ->
                    val isCurrent = selectedGame?.id == game.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedGame = game
                                editingName = game.name
                                editingPosterUrl = game.posterUrl
                            }
                            .background(
                                if (isCurrent) Color(0xFF2E174D) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Game poster thumbnail preview
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            coil.compose.SubcomposeAsyncImage(
                                model = game.posterUrl,
                                contentDescription = game.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                loading = {
                                    CircularProgressIndicator(
                                        color = PurpleGlow,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = game.name,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "ID: ${game.id} • Active: ${game.isActive}",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        if (selectedGame != null) {
            val game = selectedGame!!
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PurpleGlow, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Editing: ${game.name}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = editingName,
                        onValueChange = { editingName = it },
                        label = { Text("Game Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurpleGlow,
                            unfocusedBorderColor = BorderColor,
                            focusedLabelColor = PurpleGlow,
                            unfocusedLabelColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Poster Image",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Big poster preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        if (editingPosterUrl.isNotBlank()) {
                            coil.compose.SubcomposeAsyncImage(
                                model = editingPosterUrl,
                                contentDescription = "New Poster Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                loading = {
                                    CircularProgressIndicator(color = PurpleGlow)
                                }
                            )
                        } else {
                            Text("No Poster Selected", color = Color.Gray)
                        }

                        if (isUploading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = CyanGlow)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Uploading to Supabase...", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { launcher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            enabled = !isUploading,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = "Upload")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload Poster", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.adminUpdateCasinoGame(
                                    id = game.id,
                                    name = editingName,
                                    posterUrl = editingPosterUrl,
                                    isActive = true
                                )
                                selectedGame = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleGlow),
                            enabled = !isUploading && editingName.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun HistoryScreen(viewModel: MainViewModel) {
    val personalTx by viewModel.personalTransactions.collectAsStateWithLifecycle()
    var currentPage by remember { mutableIntStateOf(0) }
    val pageSize = 10

    val startIndex = currentPage * pageSize
    val endIndex = minOf(startIndex + pageSize, personalTx.size)
    val paginatedTx = if (startIndex < personalTx.size) personalTx.subList(startIndex, endIndex) else emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.setScreen("home") }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Transaction History",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Page count indicator
            val totalPages = maxOf(1, (personalTx.size + pageSize - 1) / pageSize)
            Text(
                text = "Page ${currentPage + 1} of $totalPages",
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (paginatedTx.isEmpty()) {
                item {
                    Text(
                        text = "No history available on this page",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                items(paginatedTx) { tx ->
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
                                val displayType = when (tx.type) {
                                    "DEPOSIT" -> "Deposit Money"
                                    "WITHDRAWAL" -> "Withdrawal Request"
                                    "REFERRAL_REWARD" -> "Referral Reward Earned"
                                    "TOURNAMENT_ENTRY" -> "Tournament Entry Fee"
                                    "PRIZE_WON" -> "Prize Won"
                                    "BALANCE_ADJUST" -> "Balance Adjustment"
                                    "DEVICE_REGISTRATION" -> "Device Registration"
                                    else -> tx.type
                                }
                                val displayRef = if (tx.type == "REFERRAL_REWARD" && tx.referenceNumber?.startsWith("REF-") == true) {
                                    val wa = tx.referenceNumber.substring(4)
                                    val last4 = if (wa.length >= 4) wa.substring(wa.length - 4) else wa
                                    val masked = "*".repeat(maxOf(0, wa.length - 4)) + last4
                                    "Referred Friend: $masked"
                                } else {
                                    "Ref: ${tx.referenceNumber ?: "N/A"}"
                                }
                                Text(
                                    text = displayType,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = displayRef,
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
            }
        }

        // Pagination buttons row at the bottom
        if (personalTx.size > pageSize) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { if (currentPage > 0) currentPage-- },
                    enabled = currentPage > 0,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, if (currentPage > 0) BorderColor else Color.DarkGray),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Previous Page", fontSize = 14.sp)
                }

                Button(
                    onClick = { if (endIndex < personalTx.size) currentPage++ },
                    enabled = endIndex < personalTx.size,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Next Page", color = DarkBg, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun AdminRecordsScreen(viewModel: MainViewModel) {
    val stats by viewModel.adminStats.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadAdminStats()
    }

    val currentStats = stats
    if (currentStats == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = CyanGlow)
        }
        return
    }

    val s = currentStats
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Text("User Records", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CyanGlow)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecordCard("Total", "${s.totalUsers}", Modifier.weight(1f))
                RecordCard("Today", "${s.dailyUsers}", Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecordCard("This Week", "${s.weeklyUsers}", Modifier.weight(1f))
                RecordCard("This Month", "${s.monthlyUsers}", Modifier.weight(1f))
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Revenue Records (Deposits)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = EmeraldGlow)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecordCard("Total", "₹${s.totalSpent}", Modifier.weight(1f), EmeraldGlow)
                RecordCard("Today", "₹${s.dailySpent}", Modifier.weight(1f), EmeraldGlow)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecordCard("This Week", "₹${s.weeklySpent}", Modifier.weight(1f), EmeraldGlow)
                RecordCard("This Month", "₹${s.monthlySpent}", Modifier.weight(1f), EmeraldGlow)
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Last 7 Days Chart", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PurpleGlow)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                modifier = Modifier.fillMaxWidth().height(250.dp),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                // simple bar chart representation
                Row(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val maxSpent = s.graphData.maxOfOrNull { it.spent }?.coerceAtLeast(1.0) ?: 1.0
                    s.graphData.forEach { point ->
                        val heightFraction = (point.spent / maxSpent).toFloat()
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Text("₹${point.spent.toInt()}", color = EmeraldGlow, fontSize = 9.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .fillMaxHeight(heightFraction)
                                    .background(PurpleGlow, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(point.date.takeLast(5), color = Color.Gray, fontSize = 9.sp) // MM-DD
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecordCard(title: String, value: String, modifier: Modifier = Modifier, color: Color = CyanGlow) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        modifier = modifier,
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

fun uriToBase64(context: android.content.Context, uri: android.net.Uri): Pair<String, String>? {
    return try {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
        val inputStream = contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()
        if (bytes != null) {
            val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            base64 to mimeType
        } else null
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
        null
    }
}


@Composable
fun ReferralDetailsScreen(viewModel: MainViewModel) {
    val user by viewModel.loggedInUser.collectAsStateWithLifecycle()
    val referredUsers by viewModel.referredUsers.collectAsStateWithLifecycle()
    val totalCount by viewModel.referredTotalCount.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingReferred.collectAsStateWithLifecycle()
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val globalSettings by viewModel.globalSettings.collectAsStateWithLifecycle()

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val isScrollAtEnd = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (layoutInfo.totalItemsCount == 0) {
                false
            } else {
                val lastVisibleItem = visibleItemsInfo.lastOrNull()
                lastVisibleItem != null && lastVisibleItem.index >= layoutInfo.totalItemsCount - 2
            }
        }
    }

    LaunchedEffect(isScrollAtEnd.value) {
        if (isScrollAtEnd.value) {
            viewModel.loadMoreReferredUsers()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.setScreen("home") }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Referral Details",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Referral Code Card & Total Joined Stat
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Your Referral Code",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(DarkBg, RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .border(1.dp, PurpleGlow, RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = user?.ownReferralCode ?: "CODE1234",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PurpleGlow,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(user?.ownReferralCode ?: ""))
                            viewModel.showToast("Referral Code copied!")
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code", tint = PurpleGlow, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Joined Users",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Joined under your code",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    Text(
                        text = "$totalCount Joined",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CyanGlow
                    )
                }
            }
        }

        Text(
            text = "Joined User Details (Rule: Min ₹${globalSettings.referralMinDeposit} deposit)",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Paginated List of referred users
        androidx.compose.foundation.lazy.LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(referredUsers) { refUser ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    modifier = Modifier.fillMaxWidth(),
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
                            Text(
                                text = "Gamer: ${refUser.name}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "WhatsApp: ${refUser.maskedWhatsapp}",
                                fontSize = 12.sp,
                                color = Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Joined: ${if (refUser.createdAt.length >= 10) refUser.createdAt.take(10) else refUser.createdAt}",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Deposited Amount",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "₹${String.format("%.2f", refUser.totalDeposited)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (refUser.totalDeposited >= globalSettings.referralMinDeposit) EmeraldGlow else Color.Gray
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(color = PurpleGlow, modifier = Modifier.size(24.dp))
                    }
                }
            }

            if (!isLoading && referredUsers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No referred users found yet.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

