cat << 'INNER' > patch.py
import re

with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'r') as f:
    content = f.read()

target = """    val adminActiveLudoOpponentName = MutableStateFlow<String>("")

    fun requestLudoMatch(user: com.example.data.User, tournament: com.example.data.Tournament) {
        val req = LudoMatchRequest(user.whatsappNumber, user.name, tournament, System.currentTimeMillis())
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
    }"""

replacement = """    val adminActiveLudoOpponentName = MutableStateFlow<String>("")

    fun requestLudoMatch(user: com.example.data.User, tournament: com.example.data.Tournament) {
        viewModelScope.launch(Dispatchers.IO) {
            com.example.data.SupabaseClient.requestLudoMatch(user.whatsappNumber, user.name, tournament.id)
        }
    }

    fun cancelLudoMatch(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            com.example.data.SupabaseClient.cancelLudoMatch(userId)
        }
    }

    fun adminJoinLudoMatch(req: LudoMatchRequest) {
        val randomNames = listOf("ProGamer", "ShadowNinja", "LudoKing", "AceStriker", "DarkKnight")
        val adminRandomName = randomNames.random()
        viewModelScope.launch(Dispatchers.IO) {
            val success = com.example.data.SupabaseClient.acceptLudoMatchAdmin(req.userId, adminRandomName)
            if (success) {
                adminActiveLudoOpponentName.value = req.userName
                adminActiveLudoTournament.value = req.tournament
            } else {
                _toastMessage.value = "Failed to join match or expired."
            }
        }
    }

    fun endAdminLudoMatch() {
        adminActiveLudoTournament.value = null
        adminActiveLudoOpponentName.value = ""
    }

    fun fetchAdminLudoRequests() {
        viewModelScope.launch(Dispatchers.IO) {
            val jsonString = com.example.data.SupabaseClient.fetchLudoMatchRequestsAdmin()
            try {
                val array = org.json.JSONArray(jsonString)
                val list = mutableListOf<LudoMatchRequest>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.getString("userId")
                    val name = obj.getString("userName")
                    val tId = obj.getInt("tournamentId")
                    val time = obj.getLong("timestamp")
                    // We need a dummy tournament object here, or fetch it.
                    // For simplicity, we just create a minimal tournament.
                    val t = com.example.data.Tournament(
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
                    )
                    list.add(LudoMatchRequest(id, name, t, time))
                }
                ludoMatchRequests.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
"""

content = content.replace(target, replacement)
with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'w') as f:
    f.write(content)
INNER
python3 patch.py
