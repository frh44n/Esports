cat << 'INNER' > patch.py
import re

with open('app/src/main/java/com/example/ui/LudoScreen.kt', 'r') as f:
    content = f.read()

target = """                        // Breathe scale animation for playable pawns
                        val scale by animateFloatAsState(
                            targetValue = if (isPawnPlayable) 1.25f else 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ), label = ""
                        )"""

replacement = """                        // Breathe scale animation for playable pawns
                        val infiniteTransition = rememberInfiniteTransition(label = "")
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = if (isPawnPlayable) 1.25f else 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ), label = ""
                        )"""

content = content.replace(target, replacement)
with open('app/src/main/java/com/example/ui/LudoScreen.kt', 'w') as f:
    f.write(content)
INNER
python3 patch.py
