cat << 'INNER' > patch.py
import re

with open('app/src/main/java/com/example/data/SupabaseClient.kt', 'r') as f:
    content = f.read()

new_methods = """
    fun requestLudoMatch(whatsapp: String, userName: String, tournamentId: Int): Boolean {
        return try {
            val json = JSONObject().apply {
                put("whatsapp", whatsapp)
                put("userName", userName)
                put("tournamentId", tournamentId)
            }
            val request = Request.Builder()
                .url(getServerUrl() + "/api/ludo/request-match")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun cancelLudoMatch(whatsapp: String): Boolean {
        return try {
            val json = JSONObject().apply {
                put("whatsapp", whatsapp)
            }
            val request = Request.Builder()
                .url(getServerUrl() + "/api/ludo/cancel-match")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getLudoMatchStatus(whatsapp: String): JSONObject? {
        return try {
            val request = Request.Builder()
                .url(getServerUrl() + "/api/ludo/match-status?whatsapp=" + whatsapp)
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    JSONObject(response.body?.string() ?: "{}")
                } else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun fetchLudoMatchRequestsAdmin(): String {
        return try {
            val request = Request.Builder()
                .url(getServerUrl() + "/api/admin/ludo/requests")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string() ?: "[]"
                } else "[]"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "[]"
        }
    }

    fun acceptLudoMatchAdmin(whatsapp: String, opponentName: String): Boolean {
        return try {
            val json = JSONObject().apply {
                put("whatsapp", whatsapp)
                put("opponentName", opponentName)
            }
            val request = Request.Builder()
                .url(getServerUrl() + "/api/admin/ludo/accept")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

}"""

content = content.replace("}\n// End of SupabaseClient (Assumed closing bracket, but doing it safely by replacing last })", new_methods)
# Find the last closing bracket
last_brace = content.rfind('}')
if last_brace != -1:
    content = content[:last_brace] + new_methods

with open('app/src/main/java/com/example/data/SupabaseClient.kt', 'w') as f:
    f.write(content)
INNER
python3 patch.py
