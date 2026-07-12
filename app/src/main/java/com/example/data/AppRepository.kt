package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AppRepository(private val appDao: AppDao) {

    // local logged-in session flow for UI session tracking
    val loggedInUserFlow: Flow<User?> = appDao.getLoggedInUserFlow()

    suspend fun getLoggedInUser(): User? = withContext(Dispatchers.IO) {
        appDao.getLoggedInUser()
    }

    /**
     * SignUp online via Supabase
     */
    suspend fun signUp(whatsappNumber: String, name: String, password: String, referralCode: String?, deviceId: String): Result<User> = withContext(Dispatchers.IO) {
        val result = SupabaseClient.signUp(whatsappNumber, name, password, referralCode, deviceId)
        result.mapCatching { authResp ->
            // Fetch the newly created profile to get own referral code and default values
            val profile = SupabaseClient.fetchProfile(authResp.whatsappNumber)
                ?: throw Exception("User profile could not be found after signup.")

            // Clear any old logged-in users on device
            appDao.logoutAllUsers()
            
            // Save user session locally for session tracking
            val sessionUser = profile.copy(isLoggedIn = true)
            appDao.insertUser(sessionUser)
            sessionUser
        }
    }

    /**
     * Login online via Supabase
     */
    suspend fun login(whatsappNumber: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        val result = SupabaseClient.login(whatsappNumber, password)
        result.mapCatching { authResp ->
            val profile = SupabaseClient.fetchProfile(authResp.whatsappNumber)
                ?: throw Exception("User profile could not be found.")

            // Clear old session
            appDao.logoutAllUsers()

            // Save user session locally
            val sessionUser = profile.copy(isLoggedIn = true)
            appDao.insertUser(sessionUser)
            sessionUser
        }
    }

    /**
     * Synchronize profile stats and balances from Supabase to keep session up to date
     */
    suspend fun syncProfile(whatsappNumber: String): User? = withContext(Dispatchers.IO) {
        val profile = SupabaseClient.fetchProfile(whatsappNumber)
        if (profile != null) {
            val sessionUser = profile.copy(isLoggedIn = true)
            appDao.insertUser(sessionUser)
            sessionUser
        } else {
            null
        }
    }

    /**
     * Logout and destroy session tracking
     */
    suspend fun logout() = withContext(Dispatchers.IO) {
        appDao.logoutAllUsers()
    }

    /**
     * Fetch all Tournaments from Supabase
     */
    suspend fun getTournaments(): List<Tournament> = withContext(Dispatchers.IO) {
        SupabaseClient.fetchTournaments()
    }

    /**
     * Fetch user's registered tournaments from Supabase
     */
    suspend fun getRegistrations(whatsappNumber: String): List<Registration> = withContext(Dispatchers.IO) {
        SupabaseClient.fetchRegistrations(whatsappNumber)
    }

    /**
     * Register for tournament using Supabase DB and handle wallet deduction
     */
    suspend fun registerForTournament(whatsappNumber: String, tournamentId: Int): String? = withContext(Dispatchers.IO) {
        val result = SupabaseClient.registerForTournamentOnline(whatsappNumber, tournamentId)
        if (result.isSuccess) {
            // Sync local user session
            syncProfile(whatsappNumber)
            null // Success
        } else {
            result.exceptionOrNull()?.message ?: "Failed to register"
        }
    }

    /**
     * Submit a Deposit request online
     */
    suspend fun submitDeposit(whatsappNumber: String, amount: Double, upiId: String, referenceNumber: String): Boolean = withContext(Dispatchers.IO) {
        if (amount <= 0 || referenceNumber.length != 12 || !referenceNumber.all { it.isDigit() }) {
            return@withContext false
        }
        val transaction = Transaction(
            whatsappNumber = whatsappNumber,
            type = "DEPOSIT",
            amount = amount,
            upiId = upiId,
            referenceNumber = referenceNumber,
            status = "PENDING",
            timestamp = System.currentTimeMillis()
        )
        SupabaseClient.insertTransaction(transaction)
    }

    /**
     * Submit a Withdrawal request online
     */
    suspend fun submitWithdrawal(whatsappNumber: String, amount: Double, upiId: String): String? = withContext(Dispatchers.IO) {
        if (amount <= 0) return@withContext "Please enter a valid amount"
        val transaction = Transaction(
            whatsappNumber = whatsappNumber,
            type = "WITHDRAWAL",
            amount = amount,
            upiId = upiId,
            referenceNumber = null,
            status = "PENDING",
            timestamp = System.currentTimeMillis()
        )
        val success = SupabaseClient.insertTransaction(transaction)
        if (success) {
            // Sync local user session
            syncProfile(whatsappNumber)
            null // Success
        } else {
            "Failed to submit withdrawal request (check balance)."
        }
    }

    /**
     * Fetch user transactions online
     */
    suspend fun getTransactions(whatsappNumber: String, limit: Int): List<Transaction> = withContext(Dispatchers.IO) {
        SupabaseClient.fetchTransactions(whatsappNumber, limit)
    }

    /**
     * Fetch paginated referred users list
     */
    suspend fun getReferredUsers(whatsappNumber: String, page: Int, limit: Int): ReferredUsersResponse = withContext(Dispatchers.IO) {
        SupabaseClient.fetchReferredUsers(whatsappNumber, page, limit)
    }

    /**
     * Fetch all transactions for admin online
     */
    suspend fun getAllTransactionsAdmin(): List<Transaction> = withContext(Dispatchers.IO) {
        SupabaseClient.fetchAllTransactionsAdmin()
    }

    /**
     * Fetch game histories online
     */
    suspend fun getGameHistories(whatsappNumber: String): List<GameHistory> = withContext(Dispatchers.IO) {
        SupabaseClient.fetchGameHistories(whatsappNumber)
    }

    // Admin Commands

    suspend fun approveDeposit(transactionId: Int, txAmount: Double, txUser: String): Boolean = withContext(Dispatchers.IO) {
        val statusSuccess = SupabaseClient.updateTransactionStatus(transactionId, "APPROVED")
        if (statusSuccess) {
            syncProfile(txUser)
        }
        statusSuccess
    }

    suspend fun rejectDeposit(transactionId: Int): Boolean = withContext(Dispatchers.IO) {
        SupabaseClient.updateTransactionStatus(transactionId, "REJECTED")
    }

    suspend fun approveWithdrawal(transactionId: Int): Boolean = withContext(Dispatchers.IO) {
        SupabaseClient.updateTransactionStatus(transactionId, "APPROVED")
    }

    suspend fun rejectWithdrawal(transactionId: Int, txAmount: Double, txUser: String): Boolean = withContext(Dispatchers.IO) {
        val statusSuccess = SupabaseClient.updateTransactionStatus(transactionId, "REJECTED")
        if (statusSuccess) {
            syncProfile(txUser)
        }
        statusSuccess
    }

    suspend fun createTournament(tournament: Tournament): Boolean = withContext(Dispatchers.IO) {
        SupabaseClient.insertTournament(tournament)
    }

    suspend fun updateTournamentRoom(id: Int, roomId: String?, roomPassword: String?, startTime: String): Boolean = withContext(Dispatchers.IO) {
        SupabaseClient.updateTournamentRoom(id, roomId, roomPassword, startTime)
    }

    suspend fun deleteTournament(id: Int): Boolean = withContext(Dispatchers.IO) {
        SupabaseClient.deleteTournament(id)
    }

    suspend fun startTournament(id: Int): Boolean = withContext(Dispatchers.IO) {
        SupabaseClient.startTournament(id)
    }

    suspend fun finishTournament(id: Int): Boolean = withContext(Dispatchers.IO) {
        SupabaseClient.finishTournament(id)
    }

    suspend fun completeGame(gameHistoryId: Int, prizeWon: Double, winnerWhatsapp: String): Boolean = withContext(Dispatchers.IO) {
        val success = SupabaseClient.updateGameHistoryStatus(gameHistoryId, "COMPLETED", prizeWon)
        if (success) {
            syncProfile(winnerWhatsapp)
        }
        success
    }

    suspend fun getGlobalUpiId(): String = withContext(Dispatchers.IO) {
        SupabaseClient.fetchGlobalUpiId()
    }

    suspend fun updateGlobalUpiId(upiId: String): Boolean = withContext(Dispatchers.IO) {
        SupabaseClient.updateGlobalUpiId(upiId)
    }

    suspend fun getGlobalSettings(): GlobalSettings = withContext(Dispatchers.IO) {
        SupabaseClient.fetchGlobalSettings()
    }

    suspend fun updateGlobalSettings(
        upiId: String? = null,
        waUrl: String? = null,
        tgUrl: String? = null,
        referralReward: Double? = null,
        referralMinDeposit: Double? = null
    ): Boolean = withContext(Dispatchers.IO) {
        SupabaseClient.updateGlobalSettings(upiId, waUrl, tgUrl, referralReward, referralMinDeposit)
    }

    suspend fun fetchAdminStats(): AdminStats? = withContext(Dispatchers.IO) {
        SupabaseClient.fetchAdminStats()
    }

    suspend fun searchUserAdmin(whatsapp: String): User? = withContext(Dispatchers.IO) {
        SupabaseClient.searchUserAdmin(whatsapp)
    }

    suspend fun updateUserBalanceAdmin(whatsapp: String, deposit: Double?, withdrawal: Double?): Boolean = withContext(Dispatchers.IO) {
        SupabaseClient.updateUserBalanceAdmin(whatsapp, deposit, withdrawal)
    }

    suspend fun getTournamentRegistrations(tournamentId: Int): List<Registration> = withContext(Dispatchers.IO) {
        SupabaseClient.fetchTournamentRegistrations(tournamentId)
    }

    suspend fun updateRegistrationAdmin(registrationId: Int, formattedWhatsapp: String): Boolean = withContext(Dispatchers.IO) {
        SupabaseClient.updateRegistrationAdmin(registrationId, formattedWhatsapp)
    }

    suspend fun declarePositionAndReward(registrationId: Int, position: String, prizeAmount: Double, rawWhatsapp: String, tournamentTitle: String): Boolean = withContext(Dispatchers.IO) {
        SupabaseClient.declarePositionAndReward(registrationId, position, prizeAmount, rawWhatsapp, tournamentTitle)
    }

    suspend fun updateTournamentAdmin(
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
    ): Boolean = withContext(Dispatchers.IO) {
        SupabaseClient.updateTournamentAdmin(id, game, title, posterRes, entryFee, prizePool, prize1st, prize2nd, prize3rd, prize4th, rules, startTime)
    }

    suspend fun uploadPhoto(base64Image: String, filename: String, mimeType: String): Result<String> = withContext(Dispatchers.IO) {
        SupabaseClient.uploadPhoto(base64Image, filename, mimeType)
    }

    private fun prePopulateTournamentsOnline() {
        SupabaseClient.insertTournament(
            Tournament(
                game = "BGMI",
                title = "BGMI Sunday Championship",
                posterRes = "bgmi",
                entryFee = 50.0,
                prizePool = 5000.0,
                prize1st = 2500.0,
                prize2nd = 1500.0,
                prize3rd = 700.0,
                prize4th = 300.0,
                rules = "1. Mobile devices only.\n2. No hacks or emulator play.\n3. Arrive 15 minutes before start.\n4. Decisions of admins are final.",
                startTime = "Sunday 06:00 PM"
            )
        )
        SupabaseClient.insertTournament(
            Tournament(
                game = "FREEFIRE",
                title = "Free Fire Guild Clash",
                posterRes = "freefire",
                entryFee = 30.0,
                prizePool = 3000.0,
                prize1st = 1500.0,
                prize2nd = 900.0,
                prize3rd = 400.0,
                prize4th = 200.0,
                rules = "1. Squad battles, classic mode.\n2. Emulators blocked.\n3. Play fair, respect everyone.",
                startTime = "Saturday 08:00 PM"
            )
        )
        SupabaseClient.insertTournament(
            Tournament(
                game = "PUBG",
                title = "PUBG Mobile Ultimate Cup",
                posterRes = "pubg",
                entryFee = 100.0,
                prizePool = 10000.0,
                prize1st = 5000.0,
                prize2nd = 3000.0,
                prize3rd = 1500.0,
                prize4th = 500.0,
                rules = "1. Erangel map TPP.\n2. No teaming. Teaming results in instant ban.",
                startTime = "Tonight 09:00 PM"
            )
        )
    }
}
