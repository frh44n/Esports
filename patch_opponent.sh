cat << 'INNER' > patch.py
import re

with open('app/src/main/java/com/example/ui/LudoScreen.kt', 'r') as f:
    content = f.read()

target = """    var matchingTournament by remember { mutableStateOf<Tournament?>(null) }
    var selectedTournamentForPlay by remember { mutableStateOf<Tournament?>(null) }"""

replacement = """    var matchingTournament by remember { mutableStateOf<Tournament?>(null) }
    var selectedTournamentForPlay by remember { mutableStateOf<Tournament?>(null) }
    var activeOpponentName by remember { mutableStateOf("Opponent") }"""

content = content.replace(target, replacement)

target2 = """            onMatchFound = { opponentName ->
                // Start game with this opponent name
                selectedTournamentForPlay = matchingTournament
                matchingTournament = null
            },"""

replacement2 = """            onMatchFound = { opponentName ->
                activeOpponentName = opponentName
                selectedTournamentForPlay = matchingTournament
                matchingTournament = null
            },"""

content = content.replace(target2, replacement2)

target3 = """    if (selectedTournamentForPlay != null) {
        LudoGameManager(
            viewModel = viewModel,
            tournament = selectedTournamentForPlay!!,
            onBack = { selectedTournamentForPlay = null }
        )"""

replacement3 = """    if (selectedTournamentForPlay != null) {
        LudoGameManager(
            viewModel = viewModel,
            tournament = selectedTournamentForPlay!!,
            opponentName = activeOpponentName,
            onBack = { selectedTournamentForPlay = null }
        )"""

content = content.replace(target3, replacement3)

target4 = """fun LudoGameManager(
    viewModel: MainViewModel,
    tournament: Tournament,
    onBack: () -> Unit
) {"""

replacement4 = """fun LudoGameManager(
    viewModel: MainViewModel,
    tournament: Tournament,
    opponentName: String = "Opponent",
    onBack: () -> Unit
) {"""

content = content.replace(target4, replacement4)

target5 = """            LudoGamePlayScreen(
                viewModel = viewModel,
                tournament = tournament,
                onGameFinished = { score, isWinner ->"""

replacement5 = """            LudoGamePlayScreen(
                viewModel = viewModel,
                tournament = tournament,
                opponentName = opponentName,
                onGameFinished = { score, isWinner ->"""

content = content.replace(target5, replacement5)

target6 = """fun LudoGamePlayScreen(
    viewModel: MainViewModel,
    tournament: Tournament,
    onGameFinished: (Int, Boolean) -> Unit
) {"""

replacement6 = """fun LudoGamePlayScreen(
    viewModel: MainViewModel,
    tournament: Tournament,
    opponentName: String = "Opponent",
    onGameFinished: (Int, Boolean) -> Unit
) {"""

content = content.replace(target6, replacement6)

target7 = """    val opponentName = remember { 
        "Opponent" 
    }"""

replacement7 = """    val botName = opponentName"""

content = content.replace(target7, replacement7)

with open('app/src/main/java/com/example/ui/LudoScreen.kt', 'w') as f:
    f.write(content)
INNER
python3 patch.py
