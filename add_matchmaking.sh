cat << 'INNER' >> app/src/main/java/com/example/ui/LudoScreen.kt

@Composable
fun LudoMatchmakingScreen(
    tournament: com.example.data.Tournament,
    onMatchFound: () -> Unit,
    onCancel: () -> Unit
) {
    var timer by remember { mutableIntStateOf(60) }
    var slot1 by remember { mutableIntStateOf(0) }
    var slot2 by remember { mutableIntStateOf(1) }
    var slot3 by remember { mutableIntStateOf(2) }
    
    val avatarIcons = listOf(
        Icons.Default.Person,
        Icons.Default.Face,
        Icons.Default.AccountCircle,
        Icons.Default.EmojiEmotions,
        Icons.Default.SentimentSatisfied
    )

    LaunchedEffect(Unit) {
        var elapsed = 0
        while (timer > 0) {
            kotlinx.coroutines.delay(100L) // Fast spinning
            slot1 = (slot1 + 1) % avatarIcons.size
            slot2 = (slot2 + 1) % avatarIcons.size
            slot3 = (slot3 + 1) % avatarIcons.size
            
            elapsed += 100
            if (elapsed % 1000 == 0) {
                timer--
                // 10% chance to find match each second after 3 seconds
                if (timer < 57 && kotlin.random.Random.nextFloat() < 0.1f) {
                    onMatchFound()
                    return@LaunchedEffect
                }
            }
        }
        // If timer runs out
        onCancel()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Finding Opponent...", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("00:\${timer.toString().padStart(2, '0')}", color = EmeraldGlow, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(avatarIcons[slot1], contentDescription = null, tint = PurpleGlow, modifier = Modifier.size(64.dp))
            Text("VS", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Icon(avatarIcons[slot2], contentDescription = null, tint = CyanGlow, modifier = Modifier.size(64.dp))
        }
        
        Spacer(modifier = Modifier.height(64.dp))
        Button(
            onClick = onCancel,
            colors = ButtonDefaults.buttonColors(containerColor = RedGlow)
        ) {
            Text("Cancel & Refund")
        }
    }
}
INNER
