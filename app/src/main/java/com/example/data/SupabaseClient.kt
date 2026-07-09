package com.example.data

import android.util.Log
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object SupabaseClient {
    private const val TAG = "SupabaseClient"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private var appContext: android.content.Context? = null

    fun init(context: android.content.Context) {
        appContext = context.applicationContext
    }

    fun getServerUrl(): String {
        val prefs = appContext?.getSharedPreferences("server_prefs", android.content.Context.MODE_PRIVATE)
        val savedUrl = prefs?.getString("server_url", null)
        if (!savedUrl.isNullOrBlank()) return savedUrl

        return try {
            val configUrl = BuildConfig.SERVER_URL
            if (configUrl.startsWith("http")) configUrl else "https://gamer-tournament-api.onrender.com"
        } catch (e: Exception) {
            "https://gamer-tournament-api.onrender.com"
        }
    }

    fun setServerUrl(context: android.content.Context, url: String) {
        val prefs = context.getSharedPreferences("server_prefs", android.content.Context.MODE_PRIVATE)
        val cleanUrl = url.trim().removeSuffix("/")
        prefs.edit().putString("server_url", cleanUrl).apply()
        // Force refresh in memory
        init(context)
    }

    fun isConfigured(): Boolean {
        return getServerUrl().isNotBlank()
    }

    data class AuthResponse(
        val accessToken: String,
        val userId: String,
        val whatsappNumber: String
    )

    fun signUp(whatsapp: String, name: String, password: String, referralCode: String?): Result<AuthResponse> {
        val url = "${getServerUrl()}/api/auth/signup"
        val bodyJson = JSONObject().apply {
            put("whatsapp", whatsapp)
            put("name", name)
            put("password", password)
            put("referral_code", referralCode ?: JSONObject.NULL)
        }

        val request = Request.Builder()
            .url(url)
            .post(bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                Log.d(TAG, "signUp response (${response.code}): $bodyStr")

                if (response.isSuccessful) {
                    val jsonObj = JSONObject(bodyStr)
                    val accessToken = jsonObj.optString("access_token", "")
                    val userObj = jsonObj.optJSONObject("user")
                    val userId = userObj?.optString("id", "") ?: ""
                    Result.success(AuthResponse(accessToken, userId, whatsapp))
                } else {
                    val errMsg = parseError(bodyStr) ?: "SignUp failed (Code: ${response.code})"
                    Result.failure(Exception(errMsg))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "signUp network error", e)
            Result.failure(e)
        }
    }

    fun login(whatsapp: String, password: String): Result<AuthResponse> {
        val url = "${getServerUrl()}/api/auth/login"
        val bodyJson = JSONObject().apply {
            put("whatsapp", whatsapp)
            put("password", password)
        }

        val request = Request.Builder()
            .url(url)
            .post(bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                Log.d(TAG, "login response (${response.code}): $bodyStr")

                if (response.isSuccessful) {
                    val jsonObj = JSONObject(bodyStr)
                    val accessToken = jsonObj.optString("access_token", "")
                    val userObj = jsonObj.optJSONObject("user")
                    val userId = userObj?.optString("id", "") ?: ""
                    Result.success(AuthResponse(accessToken, userId, whatsapp))
                } else {
                    val errMsg = parseError(bodyStr) ?: "Login failed. Check number or password."
                    Result.failure(Exception(errMsg))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "login network error", e)
            Result.failure(e)
        }
    }

    fun fetchProfile(whatsapp: String): User? {
        val url = "${getServerUrl()}/api/users/$whatsapp"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val jsonObj = JSONObject(bodyStr)
                    val obj = jsonObj.optJSONObject("user") ?: return null
                    User(
                        whatsappNumber = obj.getString("whatsapp_number"),
                        name = obj.optString("name", "Gamer"),
                        referralCodeUsed = if (obj.isNull("referral_code_used")) null else obj.getString("referral_code_used"),
                        ownReferralCode = obj.getString("own_referral_code"),
                        depositBalance = obj.optDouble("deposit_balance", 0.0),
                        withdrawalBalance = obj.optDouble("withdrawal_balance", 0.0),
                        referredCount = obj.optInt("referred_count", 0),
                        isLoggedIn = true
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchProfile error", e)
            null
        }
    }

    fun fetchTournaments(): List<Tournament> {
        val url = "${getServerUrl()}/api/tournaments"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val jsonObj = JSONObject(bodyStr)
                    val jsonArr = jsonObj.optJSONArray("tournaments") ?: JSONArray()
                    val list = mutableListOf<Tournament>()
                    for (i in 0 until jsonArr.length()) {
                        val obj = jsonArr.getJSONObject(i)
                        list.add(
                            Tournament(
                                id = obj.getInt("id"),
                                game = obj.getString("game"),
                                title = obj.getString("title"),
                                posterRes = obj.getString("poster_res"),
                                entryFee = obj.getDouble("entry_fee"),
                                prizePool = obj.getDouble("prize_pool"),
                                prize1st = obj.getDouble("prize_1st"),
                                prize2nd = obj.getOptionalDouble("prize_2nd", 0.0),
                                prize3rd = obj.getOptionalDouble("prize_3rd", 0.0),
                                prize4th = obj.getOptionalDouble("prize_4th", 0.0),
                                rules = obj.getString("rules"),
                                roomId = if (obj.isNull("room_id")) null else obj.getString("room_id"),
                                roomPassword = if (obj.isNull("room_password")) null else obj.getString("room_password"),
                                startTime = obj.getString("start_time"),
                                isJoined = obj.optBoolean("is_joined", false)
                            )
                        )
                    }
                    list
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchTournaments error", e)
            emptyList()
        }
    }

    fun insertTournament(tournament: Tournament): Boolean {
        val url = "${getServerUrl()}/api/tournaments"
        val bodyJson = JSONObject().apply {
            put("game", tournament.game)
            put("title", tournament.title)
            put("poster_res", tournament.posterRes)
            put("entry_fee", tournament.entryFee)
            put("prize_pool", tournament.prizePool)
            put("prize_1st", tournament.prize1st)
            put("prize_2nd", tournament.prize2nd)
            put("prize_3rd", tournament.prize3rd)
            put("prize_4th", tournament.prize4th)
            put("rules", tournament.rules)
            put("start_time", tournament.startTime)
        }

        val request = Request.Builder()
            .url(url)
            .post(bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "insertTournament error", e)
            false
        }
    }

    fun updateTournamentRoom(id: Int, roomId: String?, roomPassword: String?, startTime: String): Boolean {
        val url = "${getServerUrl()}/api/tournaments/$id/room"
        val bodyJson = JSONObject().apply {
            put("room_id", roomId ?: JSONObject.NULL)
            put("room_password", roomPassword ?: JSONObject.NULL)
            put("start_time", startTime)
        }

        val request = Request.Builder()
            .url(url)
            .patch(bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateTournamentRoom error", e)
            false
        }
    }

    fun deleteTournament(id: Int): Boolean {
        val url = "${getServerUrl()}/api/tournaments/$id"
        val request = Request.Builder()
            .url(url)
            .delete()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "deleteTournament error", e)
            false
        }
    }

    fun fetchRegistrations(whatsapp: String): List<Registration> {
        val url = "${getServerUrl()}/api/users/$whatsapp/registrations"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val jsonObj = JSONObject(bodyStr)
                    val jsonArr = jsonObj.optJSONArray("registrations") ?: JSONArray()
                    val list = mutableListOf<Registration>()
                    for (i in 0 until jsonArr.length()) {
                        val obj = jsonArr.getJSONObject(i)
                        list.add(
                            Registration(
                                id = obj.getInt("id"),
                                whatsappNumber = obj.getString("whatsapp_number"),
                                tournamentId = obj.getInt("tournament_id"),
                                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                            )
                        )
                    }
                    list
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchRegistrations error", e)
            emptyList()
        }
    }

    fun registerForTournamentOnline(whatsapp: String, tournamentId: Int): Result<Unit> {
        val url = "${getServerUrl()}/api/tournaments/register"
        val bodyJson = JSONObject().apply {
            put("whatsapp_number", whatsapp)
            put("tournament_id", tournamentId)
        }

        val request = Request.Builder()
            .url(url)
            .post(bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                Log.d(TAG, "registerForTournamentOnline response: $bodyStr")
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val errMsg = parseError(bodyStr) ?: "Tournament registration failed."
                    Result.failure(Exception(errMsg))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "registerForTournamentOnline error", e)
            Result.failure(e)
        }
    }

    fun fetchTransactions(whatsapp: String, limit: Int): List<Transaction> {
        val url = "${getServerUrl()}/api/users/$whatsapp/transactions?limit=$limit"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val jsonObj = JSONObject(bodyStr)
                    val jsonArr = jsonObj.optJSONArray("transactions") ?: JSONArray()
                    val list = mutableListOf<Transaction>()
                    for (i in 0 until jsonArr.length()) {
                        val obj = jsonArr.getJSONObject(i)
                        list.add(
                            Transaction(
                                id = obj.getInt("id"),
                                whatsappNumber = obj.getString("whatsapp_number"),
                                type = obj.getString("type"),
                                amount = obj.getDouble("amount"),
                                upiId = obj.getString("upi_id"),
                                referenceNumber = if (obj.isNull("reference_number")) null else obj.getString("reference_number"),
                                status = obj.getString("status"),
                                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                            )
                        )
                    }
                    list
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchTransactions error", e)
            emptyList()
        }
    }

    fun fetchAllTransactionsAdmin(): List<Transaction> {
        val url = "${getServerUrl()}/api/admin/transactions"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val jsonObj = JSONObject(bodyStr)
                    val jsonArr = jsonObj.optJSONArray("transactions") ?: JSONArray()
                    val list = mutableListOf<Transaction>()
                    for (i in 0 until jsonArr.length()) {
                        val obj = jsonArr.getJSONObject(i)
                        list.add(
                            Transaction(
                                id = obj.getInt("id"),
                                whatsappNumber = obj.getString("whatsapp_number"),
                                type = obj.getString("type"),
                                amount = obj.getDouble("amount"),
                                upiId = obj.getString("upi_id"),
                                referenceNumber = if (obj.isNull("reference_number")) null else obj.getString("reference_number"),
                                status = obj.getString("status"),
                                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                            )
                        )
                    }
                    list
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchAllTransactionsAdmin error", e)
            emptyList()
        }
    }

    fun insertTransaction(transaction: Transaction): Boolean {
        val url = "${getServerUrl()}/api/transactions"
        val bodyJson = JSONObject().apply {
            put("whatsapp_number", transaction.whatsappNumber)
            put("type", transaction.type)
            put("amount", transaction.amount)
            put("upi_id", transaction.upiId)
            put("reference_number", transaction.referenceNumber ?: JSONObject.NULL)
        }

        val request = Request.Builder()
            .url(url)
            .post(bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "insertTransaction error", e)
            false
        }
    }

    fun updateTransactionStatus(id: Int, status: String): Boolean {
        val url = "${getServerUrl()}/api/transactions/$id/status"
        val bodyJson = JSONObject().apply {
            put("status", status)
        }

        val request = Request.Builder()
            .url(url)
            .patch(bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateTransactionStatus error", e)
            false
        }
    }

    fun fetchGameHistories(whatsapp: String): List<GameHistory> {
        val url = "${getServerUrl()}/api/users/$whatsapp/game-histories"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val jsonObj = JSONObject(bodyStr)
                    val jsonArr = jsonObj.optJSONArray("gameHistories") ?: JSONArray()
                    val list = mutableListOf<GameHistory>()
                    for (i in 0 until jsonArr.length()) {
                        val obj = jsonArr.getJSONObject(i)
                        list.add(
                            GameHistory(
                                id = obj.getInt("id"),
                                whatsappNumber = obj.getString("whatsapp_number"),
                                gameName = obj.getString("game_name"),
                                prizeWon = if (obj.isNull("prize_won")) null else obj.getDouble("prize_won"),
                                status = obj.getString("status"),
                                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                            )
                        )
                    }
                    list
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchGameHistories error", e)
            emptyList()
        }
    }

    fun fetchAllGameHistoriesAdmin(): List<GameHistory> {
        val url = "${getServerUrl()}/api/admin/game-histories"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val jsonObj = JSONObject(bodyStr)
                    val jsonArr = jsonObj.optJSONArray("gameHistories") ?: JSONArray()
                    val list = mutableListOf<GameHistory>()
                    for (i in 0 until jsonArr.length()) {
                        val obj = jsonArr.getJSONObject(i)
                        list.add(
                            GameHistory(
                                id = obj.getInt("id"),
                                whatsappNumber = obj.getString("whatsapp_number"),
                                gameName = obj.getString("game_name"),
                                prizeWon = if (obj.isNull("prize_won")) null else obj.getDouble("prize_won"),
                                status = obj.getString("status"),
                                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                            )
                        )
                    }
                    list
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchAllGameHistoriesAdmin error", e)
            emptyList()
        }
    }

    fun updateGameHistoryStatus(id: Int, status: String, prizeWon: Double?): Boolean {
        val url = "${getServerUrl()}/api/admin/game-histories/$id/status"
        val bodyJson = JSONObject().apply {
            put("status", status)
            put("prize_won", prizeWon ?: JSONObject.NULL)
        }

        val request = Request.Builder()
            .url(url)
            .patch(bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateGameHistoryStatus error", e)
            false
        }
    }

    private fun parseError(bodyStr: String): String? {
        return try {
            val obj = JSONObject(bodyStr)
            obj.optString("error", obj.optString("message", obj.optString("error_description", null)))
        } catch (e: Exception) {
            null
        }
    }

    private fun JSONObject.getOptionalDouble(key: String, fallback: Double): Double {
        return if (isNull(key)) fallback else optDouble(key, fallback)
    }
}
