cat << 'INNER' > patch.py
import re

with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'r') as f:
    content = f.read()

target = "    fun endAdminLudoMatch() {"

replacement = """    suspend fun checkLudoMatchStatus(userId: String): String? {
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            val res = com.example.data.SupabaseClient.getLudoMatchStatus(userId)
            if (res != null && res.optString("status") == "MATCHED") {
                res.optString("opponentName")
            } else null
        }
    }

    fun endAdminLudoMatch() {"""

content = content.replace(target, replacement)
with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'w') as f:
    f.write(content)
INNER
python3 patch.py
