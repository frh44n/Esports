import re

with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'r') as f:
    content = f.read()

# Add import
if "import kotlinx.coroutines.Dispatchers" not in content:
    content = content.replace("import kotlinx.coroutines.flow.StateFlow", "import kotlinx.coroutines.flow.StateFlow\nimport kotlinx.coroutines.Dispatchers")

# Fix Tournament constructor
target = """                    val t = com.example.data.Tournament(
                        id = tId,
                        game = "Ludo",
                        title = "Ludo Live Match",
                        time = "Live",
                        prizePool = 0.0,
                        perKill = 0.0,
                        entryFee = 0.0,
                        type = "Solo",
                        version = "Global",
                        map = "Ludo",
                        status = "Upcoming",
                        roomId = null,
                        roomPassword = null
                    )"""

replacement = """                    val t = com.example.data.Tournament(
                        id = tId,
                        game = "Ludo",
                        title = "Ludo Live Match",
                        posterRes = "",
                        entryFee = 0.0,
                        prizePool = 0.0,
                        prize1st = 0.0,
                        prize2nd = 0.0,
                        prize3rd = 0.0,
                        prize4th = 0.0,
                        rules = "",
                        roomId = null,
                        roomPassword = null,
                        startTime = "Live"
                    )"""

content = content.replace(target, replacement)
with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'w') as f:
    f.write(content)
