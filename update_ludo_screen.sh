cat << 'INNER' > patch.py
import re

with open('app/src/main/java/com/example/ui/LudoScreen.kt', 'r') as f:
    content = f.read()

target = """    if (matchingTournament != null) {
        LudoMatchmakingScreen(
            tournament = matchingTournament!!,
            onMatchFound = {
                selectedTournamentForPlay = matchingTournament
                matchingTournament = null
            },
            onCancel = {
                matchingTournament = null
            }
        )
        return
    }"""

replacement = """    if (matchingTournament != null) {
        LudoMatchmakingScreen(
            viewModel = viewModel,
            tournament = matchingTournament!!,
            onMatchFound = { opponentName ->
                // Start game with this opponent name
                selectedTournamentForPlay = matchingTournament
                matchingTournament = null
            },
            onCancel = {
                matchingTournament = null
            }
        )
        return
    }"""

content = content.replace(target, replacement)
with open('app/src/main/java/com/example/ui/LudoScreen.kt', 'w') as f:
    f.write(content)
INNER
python3 patch.py
