cat << 'INNER' > patch.py
import re

with open('app/src/main/java/com/example/ui/LudoScreen.kt', 'r') as f:
    content = f.read()

target = """                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val p1Glow = if (currentTurn == 1) Modifier.border(2.dp, LudoBlue, RoundedCornerShape(4.dp)).padding(4.dp) else Modifier.padding(4.dp)
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
                    val p2Glow = if (currentTurn == 2) Modifier.border(2.dp, LudoGreen, RoundedCornerShape(4.dp)).padding(4.dp) else Modifier.padding(4.dp)
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
                }"""

replacement = """                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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
                }"""

content = content.replace(target, replacement)
with open('app/src/main/java/com/example/ui/LudoScreen.kt', 'w') as f:
    f.write(content)
INNER
python3 patch.py
