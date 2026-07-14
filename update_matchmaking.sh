cat << 'INNER' > app/src/main/java/com/example/ui/LudoMatchmakingScreen.kt
package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Tournament
import kotlinx.coroutines.delay

@Composable
fun LudoMatchmakingScreen(
    viewModel: MainViewModel,
    tournament: Tournament,
    onMatchFound: (String) -> Unit,
    onCancel: () -> Unit
) {
    val user by viewModel.loggedInUser.collectAsStateWithLifecycle()
    val activeMatches by viewModel.activeLudoMatches.collectAsStateWithLifecycle()

    var timer by remember { mutableIntStateOf(20) }
    var slot1 by remember { mutableIntStateOf(0) }
    var slot2 by remember { mutableIntStateOf(1) }
    var slot3 by remember { mutableIntStateOf(2) }
    var foundOpponent by remember { mutableStateOf<String?>(null) }
    
    val avatarIcons = listOf(
        Icons.Default.Person,
        Icons.Default.Face,
        Icons.Default.AccountCircle,
        Icons.Default.EmojiEmotions,
        Icons.Default.SentimentSatisfied
    )

    LaunchedEffect(Unit) {
        if (user != null) {
            viewModel.requestLudoMatch(user!!, tournament)
        }
        var elapsed = 0
        while (timer > 0 && foundOpponent == null) {
            delay(100L) // Fast spinning
            slot1 = (slot1 + 1) % avatarIcons.size
            slot2 = (slot2 + 1) % avatarIcons.size
            slot3 = (slot3 + 1) % avatarIcons.size
            
            elapsed += 100
            if (elapsed % 1000 == 0) {
                timer--
            }

            // Check if admin accepted
            if (user != null && activeMatches.containsKey(user!!.id)) {
                foundOpponent = activeMatches[user!!.id]
            }
        }
        
        if (foundOpponent != null) {
            delay(1500)
            onMatchFound(foundOpponent!!)
        } else {
            // timeout
            if (user != null) {
                viewModel.cancelLudoMatch(user!!.id)
            }
            onCancel()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (user != null && foundOpponent == null) {
                viewModel.cancelLudoMatch(user!!.id)
            }
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
        if (foundOpponent != null) {
            Text("Match Found!", color = EmeraldGlow, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text("VS \$foundOpponent", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        } else {
            Text("Finding Live Player...", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text("00:\${timer.toString().padStart(2, '0')}", color = EmeraldGlow, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
        }
        
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
            if (foundOpponent != null) {
                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = LudoGreen, modifier = Modifier.size(64.dp))
            } else {
                Icon(avatarIcons[slot2], contentDescription = null, tint = CyanGlow, modifier = Modifier.size(64.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(64.dp))
        if (foundOpponent == null) {
            Button(
                onClick = { 
                    if (user != null) viewModel.cancelLudoMatch(user!!.id)
                    onCancel() 
                },
                colors = ButtonDefaults.buttonColors(containerColor = RedGlow)
            ) {
                Text("Cancel & Refund")
            }
        }
    }
}
INNER
