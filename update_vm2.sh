cat << 'INNER' > patch.py
import re

with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'r') as f:
    content = f.read()

target = """    fun startMinesGame(a: Double, b: Int) {}
}"""

replacement = """    fun startMinesGame(a: Double, b: Int) {}

    val ludoMatchRequests = MutableStateFlow<List<LudoMatchRequest>>(emptyList())
    val activeLudoMatches = MutableStateFlow<Map<String, String>>(emptyMap())
    val adminActiveLudoTournament = MutableStateFlow<com.example.data.Tournament?>(null)
    val adminActiveLudoOpponentName = MutableStateFlow<String>("")

    fun requestLudoMatch(user: com.example.data.User, tournament: com.example.data.Tournament) {
        val req = LudoMatchRequest(user.id, user.name, tournament, System.currentTimeMillis())
        ludoMatchRequests.value = ludoMatchRequests.value + listOf(req)
    }

    fun cancelLudoMatch(userId: String) {
        ludoMatchRequests.value = ludoMatchRequests.value.filter { it.userId != userId }
        val updatedMap = activeLudoMatches.value.toMutableMap()
        updatedMap.remove(userId)
        activeLudoMatches.value = updatedMap
    }

    fun adminJoinLudoMatch(req: LudoMatchRequest) {
        val randomNames = listOf("ProGamer", "ShadowNinja", "LudoKing", "AceStriker", "DarkKnight")
        val adminRandomName = randomNames.random()
        activeLudoMatches.value = activeLudoMatches.value + (req.userId to adminRandomName)
        ludoMatchRequests.value = ludoMatchRequests.value.filter { it.userId != req.userId }
        adminActiveLudoOpponentName.value = req.userName
        adminActiveLudoTournament.value = req.tournament
    }

    fun endAdminLudoMatch() {
        adminActiveLudoTournament.value = null
        adminActiveLudoOpponentName.value = ""
    }
}

data class LudoMatchRequest(val userId: String, val userName: String, val tournament: com.example.data.Tournament, val timestamp: Long)
"""

content = content.replace(target, replacement)
with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'w') as f:
    f.write(content)
INNER
python3 patch.py
