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
        return "https://esports-13p1.onrender.com"
    }

    fun setServerUrl(context: android.content.Context, url: String) {
        // No-op, removed
    }

    fun isConfigured(): Boolean {
        return getServerUrl().isNotBlank()
    }

    data class AuthResponse(
        val accessToken: String,
        val userId: String,
        val whatsappNumber: String
    )

    fun signUp(whatsapp: String, name: String, password: String, referralCode: String?, deviceId: String): Result<AuthResponse> {
        val url = "${getServerUrl()}/api/auth/signup"
        val bodyJson = JSONObject().apply {
            put("whatsapp", whatsapp)
            put("name", name)
            put("password", password)
            put("referral_code", referralCode ?: JSONObject.NULL)
            put("device_id", deviceId)
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
                        whatsappNumber = obj.optString("whatsapp_number", whatsapp),
                        name = obj.optString("name", "Gamer"),
                        referralCodeUsed = if (obj.isNull("referral_code_used")) null else obj.optString("referral_code_used", null),
                        ownReferralCode = obj.optString("own_referral_code", ""),
                        depositBalance = obj.optDouble("deposit_balance", 0.0),
                        withdrawalBalance = obj.optDouble("withdrawal_balance", 0.0),
                        referredCount = obj.optInt("referred_count", 0),
                        isLoggedIn = true,
                        isAdmin = obj.optBoolean("is_admin", false)
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
                    throw Exception("Server returned error ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchTournaments error", e)
            throw e
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
                    throw Exception("Server returned error ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchRegistrations error", e)
            throw e
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
                                id = obj.optInt("id", 0),
                                whatsappNumber = obj.optString("whatsapp_number", ""),
                                type = obj.optString("type", ""),
                                amount = obj.optDouble("amount", 0.0),
                                upiId = obj.optString("upi_id", ""),
                                referenceNumber = if (obj.isNull("reference_number")) null else obj.optString("reference_number", null),
                                status = obj.optString("status", ""),
                                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                            )
                        )
                    }
                    list
                } else {
                    throw Exception("Server returned error ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchTransactions error", e)
            throw e
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
                                id = obj.optInt("id", 0),
                                whatsappNumber = obj.optString("whatsapp_number", ""),
                                type = obj.optString("type", ""),
                                amount = obj.optDouble("amount", 0.0),
                                upiId = obj.optString("upi_id", ""),
                                referenceNumber = if (obj.isNull("reference_number")) null else obj.optString("reference_number", null),
                                status = obj.optString("status", ""),
                                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                            )
                        )
                    }
                    list
                } else {
                    throw Exception("Server returned error ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchAllTransactionsAdmin error", e)
            throw e
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
                    throw Exception("Server returned error ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchGameHistories error", e)
            throw e
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

    fun fetchGlobalUpiId(): String {
        return fetchGlobalSettings().upiId
    }

    fun updateGlobalUpiId(upiId: String): Boolean {
        return updateGlobalSettings(upiId = upiId)
    }

    fun fetchGlobalSettings(): GlobalSettings {
        val url = "${getServerUrl()}/api/settings"
        val request = Request.Builder().url(url).get().build()
        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val jsonObj = JSONObject(bodyStr)
                    val settingsObj = jsonObj.optJSONObject("settings")
                    if (settingsObj != null) {
                        GlobalSettings(
                            upiId = settingsObj.optString("upi_id", "pay.arenaesports@upi"),
                            waUrl = settingsObj.optString("wa_url", "https://wa.me/919999999999"),
                            tgUrl = settingsObj.optString("tg_url", "https://t.me/arenaesportssupport"),
                            referralReward = settingsObj.optDouble("referral_reward", 50.0)
                        )
                    } else {
                        GlobalSettings()
                    }
                } else {
                    GlobalSettings()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchGlobalSettings error", e)
            GlobalSettings()
        }
    }

    fun updateGlobalSettings(upiId: String? = null, waUrl: String? = null, tgUrl: String? = null, referralReward: Double? = null): Boolean {
        val url = "${getServerUrl()}/api/settings"
        val bodyJson = JSONObject().apply {
            upiId?.let { put("upi_id", it) }
            waUrl?.let { put("wa_url", it) }
            tgUrl?.let { put("tg_url", it) }
            referralReward?.let { put("referral_reward", it) }
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
            Log.e(TAG, "updateGlobalSettings error", e)
            false
        }
    }

    fun fetchAdminStats(): AdminStats? {
        val url = "${getServerUrl()}/api/admin/stats"
        val request = Request.Builder().url(url).get().build()
        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val jsonObj = JSONObject(bodyStr)
                    if (jsonObj.optBoolean("success")) {
                        val statsObj = jsonObj.optJSONObject("stats") ?: return null
                        val graphArray = statsObj.optJSONArray("graphData")
                        val graphData = mutableListOf<GraphDataPoint>()
                        if (graphArray != null) {
                            for (i in 0 until graphArray.length()) {
                                val item = graphArray.optJSONObject(i)
                                if (item != null) {
                                    graphData.add(
                                        GraphDataPoint(
                                            date = item.optString("date"),
                                            users = item.optInt("users"),
                                            spent = item.optDouble("spent")
                                        )
                                    )
                                }
                            }
                        }
                        AdminStats(
                            totalUsers = statsObj.optInt("totalUsers"),
                            dailyUsers = statsObj.optInt("dailyUsers"),
                            weeklyUsers = statsObj.optInt("weeklyUsers"),
                            monthlyUsers = statsObj.optInt("monthlyUsers"),
                            totalSpent = statsObj.optDouble("totalSpent"),
                            dailySpent = statsObj.optDouble("dailySpent"),
                            weeklySpent = statsObj.optDouble("weeklySpent"),
                            monthlySpent = statsObj.optDouble("monthlySpent"),
                            graphData = graphData
                        )
                    } else null
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchAdminStats error", e)
            null
        }
    }

    fun searchUserAdmin(whatsapp: String): User? {
        val url = "${getServerUrl()}/api/admin/users/$whatsapp"
        val request = Request.Builder().url(url).get().build()
        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val jsonObj = JSONObject(bodyStr)
                    val obj = jsonObj.optJSONObject("user") ?: return null
                    User(
                        whatsappNumber = obj.optString("whatsapp_number", whatsapp),
                        name = obj.optString("name", "Gamer"),
                        referralCodeUsed = if (obj.isNull("referral_code_used")) null else obj.optString("referral_code_used", null),
                        ownReferralCode = obj.optString("own_referral_code", ""),
                        depositBalance = obj.optDouble("deposit_balance", 0.0),
                        withdrawalBalance = obj.optDouble("withdrawal_balance", 0.0),
                        referredCount = obj.optInt("referred_count", 0),
                        isLoggedIn = false,
                        isAdmin = obj.optBoolean("is_admin", false)
                    )
                } else {
                    fetchProfile(whatsapp)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "searchUserAdmin error, falling back to fetchProfile", e)
            fetchProfile(whatsapp)
        }
    }

    fun updateUserBalanceAdmin(whatsapp: String, deposit: Double?, withdrawal: Double?): Boolean {
        val url = "${getServerUrl()}/api/admin/users/$whatsapp/balance"
        val bodyJson = JSONObject().apply {
            if (deposit != null) put("deposit_balance", deposit)
            if (withdrawal != null) put("withdrawal_balance", withdrawal)
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
            Log.e(TAG, "updateUserBalanceAdmin error", e)
            false
        }
    }

    fun fetchTournamentRegistrations(tournamentId: Int): List<Registration> {
        val url = "${getServerUrl()}/api/admin/tournaments/$tournamentId/registrations"
        val request = Request.Builder().url(url).get().build()
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
                    throw Exception("Server returned error ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchTournamentRegistrations error", e)
            throw e
        }
    }

    fun updateRegistrationAdmin(registrationId: Int, formattedWhatsapp: String): Boolean {
        val url = "${getServerUrl()}/api/admin/registrations/$registrationId"
        val bodyJson = JSONObject().apply {
            put("whatsapp_number", formattedWhatsapp)
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
            Log.e(TAG, "updateRegistrationAdmin error", e)
            false
        }
    }

    fun declarePositionAndReward(registrationId: Int, position: String, prizeAmount: Double, rawWhatsapp: String, tournamentTitle: String): Boolean {
        val url = "${getServerUrl()}/api/admin/registrations/$registrationId/reward"
        val bodyJson = JSONObject().apply {
            put("position", position)
            put("prize_amount", prizeAmount)
            put("raw_whatsapp", rawWhatsapp)
            put("tournament_title", tournamentTitle)
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
            Log.e(TAG, "declarePositionAndReward error", e)
            false
        }
    }

    fun updateTournamentAdmin(
        id: Int,
        game: String,
        title: String,
        posterRes: String,
        entryFee: Double,
        prizePool: Double,
        prize1st: Double,
        prize2nd: Double,
        prize3rd: Double,
        prize4th: Double,
        rules: String,
        startTime: String
    ): Boolean {
        val url = "${getServerUrl()}/api/tournaments/$id"
        val bodyJson = JSONObject().apply {
            put("game", game)
            put("title", title)
            put("poster_res", posterRes)
            put("entry_fee", entryFee)
            put("prize_pool", prizePool)
            put("prize_1st", prize1st)
            put("prize_2nd", prize2nd)
            put("prize_3rd", prize3rd)
            put("prize_4th", prize4th)
            put("rules", rules)
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
            Log.e(TAG, "updateTournamentAdmin error", e)
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
