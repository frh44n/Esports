cat << 'INNER' > patch.py
import re

# 1. AppUi.kt - OptIn and TopAppBar
with open('app/src/main/java/com/example/ui/AppUi.kt', 'r') as f:
    content = f.read()

content = content.replace("@Composable\nfun AdminPanelScreen(viewModel: MainViewModel) {", "@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nfun AdminPanelScreen(viewModel: MainViewModel) {")
with open('app/src/main/java/com/example/ui/AppUi.kt', 'w') as f:
    f.write(content)

# 2. LudoMatchmakingScreen.kt - user.whatsappNumber
with open('app/src/main/java/com/example/ui/LudoMatchmakingScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("user!!.id", "user!!.whatsappNumber")
with open('app/src/main/java/com/example/ui/LudoMatchmakingScreen.kt', 'w') as f:
    f.write(content)

# 3. MainViewModel.kt - user.whatsappNumber
with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("user.id", "user.whatsappNumber")
with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'w') as f:
    f.write(content)

# 4. LudoScreen.kt - activeOpponentName
with open('app/src/main/java/com/example/ui/LudoScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("var matchingTournament by remember { mutableStateOf<Tournament?>(null) }\n    var matchingTimer by remember { mutableStateOf(10) }", "var matchingTournament by remember { mutableStateOf<Tournament?>(null) }\n    var matchingTimer by remember { mutableStateOf(10) }\n    var activeOpponentName by remember { mutableStateOf(\"Opponent\") }")
content = content.replace("var selectedTournamentForPlay by remember { mutableStateOf<Tournament?>(null) }", "var selectedTournamentForPlay by remember { mutableStateOf<Tournament?>(null) }\n    var activeOpponentName by remember { mutableStateOf(\"Opponent\") }")
with open('app/src/main/java/com/example/ui/LudoScreen.kt', 'w') as f:
    f.write(content)

INNER
python3 patch.py
