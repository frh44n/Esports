package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.GameHistory
import com.example.data.Tournament
import com.example.data.Transaction
import com.example.data.User
import com.example.data.AdminStats
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository

    // UI State variables
    val loggedInUser: StateFlow<User?>

    private val _allTournaments = MutableStateFlow<List<Tournament>>(emptyList())
    val allTournaments: StateFlow<List<Tournament>> = _allTournaments.asStateFlow()

    private val _allTransactionsAdmin = MutableStateFlow<List<Transaction>>(emptyList())
    val allTransactionsAdmin: StateFlow<List<Transaction>> = _allTransactionsAdmin.asStateFlow()

    private val _myRegistrations = MutableStateFlow<List<com.example.data.Registration>>(emptyList())
    val myRegistrations: StateFlow<List<com.example.data.Registration>> = _myRegistrations.asStateFlow()

    private val _personalTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val personalTransactions: StateFlow<List<Transaction>> = _personalTransactions.asStateFlow()

    private val _gameHistories = MutableStateFlow<List<GameHistory>>(emptyList())
    val gameHistories: StateFlow<List<GameHistory>> = _gameHistories.asStateFlow()

    // Local pagination limits
    private val _txLimit = MutableStateFlow(10)
    val txLimit: StateFlow<Int> = _txLimit.asStateFlow()

    // Local error/success messages
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // New states for dynamic UPI management, search user, and registrations list
    private val _dynamicUpiId = MutableStateFlow("pay.arenaesports@upi")
    val dynamicUpiId: StateFlow<String> = _dynamicUpiId.asStateFlow()

    private val _globalSettings = MutableStateFlow(com.example.data.GlobalSettings())
    val globalSettings: StateFlow<com.example.data.GlobalSettings> = _globalSettings.asStateFlow()

    private val _searchedUser = MutableStateFlow<User?>(null)
    val searchedUser: StateFlow<User?> = _searchedUser.asStateFlow()

    private val _currentTournamentRegistrations = MutableStateFlow<List<com.example.data.Registration>>(emptyList())
    val currentTournamentRegistrations: StateFlow<List<com.example.data.Registration>> = _currentTournamentRegistrations.asStateFlow()

    private val _adminStats = MutableStateFlow<AdminStats?>(null)
    val adminStats: StateFlow<AdminStats?> = _adminStats.asStateFlow()

    // Selected game category inside Esports section
    private val _selectedEsportsGame = MutableStateFlow("BGMI") // BGMI, PUBG, FREEFIRE
    val selectedEsportsGame: StateFlow<String> = _selectedEsportsGame.asStateFlow()

    // Screen navigation state
    private val _currentScreen = MutableStateFlow<String>("splash") // splash, auth, home, admin
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Fallback local memory for joined tournaments (until backend redeploy takes effect)
    private val localJoinedIds = mutableSetOf<Int>()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AppRepository(database.appDao())
        loadInAppNotifications()

        loggedInUser = repository.loggedInUserFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        // Observe login state and route accordingly after exactly 1 second (1000 ms) delay
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            var initialLoadDone = false
            repository.loggedInUserFlow.collect { user ->
                if (user != null) {
                    if (_currentScreen.value == "splash") {
                        _currentScreen.value = if (user.isAdmin) "admin" else "home"
                    }
                    if (!initialLoadDone) {
                        initialLoadDone = true
                        refreshOnlineData()
                        fetchUpiId()
                    }
                } else {
                    _currentScreen.value = "auth"
                    initialLoadDone = false
                }
            }
        }

        // Auto-refresh loop for normal users
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(5000)
                val user = loggedInUser.value
                if (user != null && !user.isAdmin) {
                    refreshOnlineData(silent = true)
                }
            }
        }

        // Trigger transaction limit change to refresh lists
        viewModelScope.launch {
            _txLimit.collect { limit ->
                val user = loggedInUser.value
                if (user != null) {
                    _personalTransactions.value = repository.getTransactions(user.whatsappNumber, limit)
                }
            }
        }
    }

    fun setScreen(screen: String) {
        if (screen == "history") {
            loadHistory()
        }
        _currentScreen.value = screen
    }

    fun selectEsportsGame(game: String) {
        _selectedEsportsGame.value = game
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun loadHistory() {
        val user = loggedInUser.value ?: return
        viewModelScope.launch {
            try {
                _personalTransactions.value = repository.getTransactions(user.whatsappNumber, _txLimit.value)
                _gameHistories.value = repository.getGameHistories(user.whatsappNumber)
            } catch (e: Exception) {
                // Ignore silently
            }
        }
    }

    /**
     * Freshly fetches all online data from Supabase for the current logged in session
     */
    fun refreshOnlineData(silent: Boolean = false, overrideUser: com.example.data.User? = null) {
        viewModelScope.launch {
            val user = overrideUser ?: loggedInUser.value ?: repository.getLoggedInUser() ?: return@launch
            if (!silent) _isRefreshing.value = true
            try {
                // Fetch dynamic global settings
                val settings = repository.getGlobalSettings()
                _dynamicUpiId.value = settings.upiId
                _globalSettings.value = settings

                // Sync latest profile balances from Supabase
                val updatedUser = repository.syncProfile(user.whatsappNumber)

                // Sync Tournaments and check if the user is registered
                val rawTournaments = repository.getTournaments()
                val regs = repository.getRegistrations(user.whatsappNumber)
                _myRegistrations.value = regs
                val joinedIds = regs.map { it.tournamentId }.toSet() + localJoinedIds
                try {
                    joinedIds.forEach { id ->
                        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("tournament_$id")
                    }
                } catch (e: Exception) { Log.e("FCM", "Topic error", e) }
                _allTournaments.value = rawTournaments.map {
                    it.copy(isJoined = joinedIds.contains(it.id))
                }

                // Load History (Transactions and Games)
                loadHistory()

                // Check and trigger notifications based on fetched data
                checkNotifications(regs, rawTournaments)

                // Sync Admin list if the user is the admin
                if (updatedUser?.isAdmin == true) {
                    _allTransactionsAdmin.value = repository.getAllTransactionsAdmin()
                }
            } catch (e: Exception) {
                if (!silent) _toastMessage.value = "Sync Error: ${e.message}"
            } finally {
                if (!silent) _isRefreshing.value = false
            }
        }
    }

    /**
     * Sign Up a user online via Supabase Auth
     */
    fun performSignUp(whatsappNumber: String, name: String, password: String, confirmPassword: String, referralCode: String?, deviceId: String) {
        viewModelScope.launch {
            if (whatsappNumber.length < 10) {
                _toastMessage.value = "Please enter a valid 10-digit WhatsApp number."
                return@launch
            }
            if (name.isBlank()) {
                _toastMessage.value = "Please enter your Name."
                return@launch
            }
            if (password.length < 6) {
                _toastMessage.value = "Password must be at least 6 characters."
                return@launch
            }
            if (password != confirmPassword) {
                _toastMessage.value = "Passwords do not match. Double check verification."
                return@launch
            }

            _isRefreshing.value = true
            val result = repository.signUp(whatsappNumber, name, password, referralCode, deviceId)
            _isRefreshing.value = false

            result.onSuccess { user ->
                _toastMessage.value = "Welcome! Signed Up and logged in successfully."
                refreshOnlineData(silent = true, overrideUser = user)
                _currentScreen.value = if (user.isAdmin == true) "admin" else "home"
            }.onFailure { err ->
                _toastMessage.value = err.message ?: "Sign Up failed. Please try again."
            }
        }
    }

    /**
     * Log in a user online via Supabase Auth
     */
    fun performLogin(whatsappNumber: String, password: String) {
        viewModelScope.launch {
            if (whatsappNumber.length < 10) {
                _toastMessage.value = "Please enter a valid 10-digit WhatsApp number."
                return@launch
            }
            if (password.isBlank()) {
                _toastMessage.value = "Please enter your password."
                return@launch
            }

            _isRefreshing.value = true
            val result = repository.login(whatsappNumber, password)
            _isRefreshing.value = false

            result.onSuccess { user ->
                _toastMessage.value = "Welcome Back! Logged in successfully."
                refreshOnlineData(silent = true, overrideUser = user)
                _currentScreen.value = if (user.isAdmin == true) "admin" else "home"
            }.onFailure { err ->
                _toastMessage.value = err.message ?: "Invalid WhatsApp number or password."
            }
        }
    }

    // Submit a deposit request
    fun submitDeposit(amount: Double, upiId: String, refNumber: String) {
        val user = loggedInUser.value ?: return
        viewModelScope.launch {
            if (amount <= 0) {
                _toastMessage.value = "Please enter a valid amount."
                return@launch
            }
            if (refNumber.length != 12 || !refNumber.all { it.isDigit() }) {
                _toastMessage.value = "Reference number must be exactly 12 digits."
                return@launch
            }
            if (upiId.isBlank()) {
                _toastMessage.value = "Please enter the UPI ID."
                return@launch
            }
            _isRefreshing.value = true
            val success = repository.submitDeposit(user.whatsappNumber, amount, upiId, refNumber)
            _isRefreshing.value = false
            if (success) {
                _toastMessage.value = "Deposit request submitted. Pending Approval!"
                refreshOnlineData()
            } else {
                _toastMessage.value = "Failed to submit. Check details and retry."
            }
        }
    }

    // Submit a withdrawal request
    fun submitWithdrawal(amount: Double, upiId: String) {
        val user = loggedInUser.value ?: return
        viewModelScope.launch {
            if (amount <= 0) {
                _toastMessage.value = "Please enter a valid amount."
                return@launch
            }
            if (upiId.isBlank()) {
                _toastMessage.value = "Please enter a valid UPI ID."
                return@launch
            }
            _isRefreshing.value = true
            val errorMsg = repository.submitWithdrawal(user.whatsappNumber, amount, upiId)
            _isRefreshing.value = false
            if (errorMsg != null) {
                _toastMessage.value = errorMsg
            } else {
                _toastMessage.value = "Withdrawal request of ₹$amount submitted successfully!"
                refreshOnlineData()
            }
        }
    }

    // Paginate transactions list
    fun loadMoreTransactions() {
        _txLimit.value = _txLimit.value + 10
    }

    // Register for an Esports tournament
    fun registerTournament(tournamentId: Int) {
        val user = loggedInUser.value ?: return
        viewModelScope.launch {
            val errorMsg = repository.registerForTournament(user.whatsappNumber, tournamentId)
            if (errorMsg != null) {
                _toastMessage.value = errorMsg
            } else {
                _toastMessage.value = "Successfully registered for tournament!"
                refreshOnlineData()
            }
        }
    }

    // Admin commands
    fun adminApproveDeposit(txId: Int, txAmount: Double, txUser: String) {
        viewModelScope.launch {
            val ok = repository.approveDeposit(txId, txAmount, txUser)
            if (ok) {
                _toastMessage.value = "Deposit approved! Balance updated."
                refreshOnlineData()
            }
        }
    }

    fun adminRejectDeposit(txId: Int) {
        viewModelScope.launch {
            val ok = repository.rejectDeposit(txId)
            if (ok) {
                _toastMessage.value = "Deposit request rejected."
                refreshOnlineData()
            }
        }
    }

    fun adminUpdateSettings(upiId: String?, waUrl: String?, tgUrl: String?, referralReward: Double?) {
        viewModelScope.launch {
            val ok = repository.updateGlobalSettings(upiId, waUrl, tgUrl, referralReward)
            if (ok) {
                _toastMessage.value = "Settings updated!"
                refreshOnlineData()
            } else {
                _toastMessage.value = "Failed to update settings"
            }
        }
    }

    fun loadAdminStats() {
        viewModelScope.launch {
            _adminStats.value = repository.fetchAdminStats()
        }
    }

    fun adminApproveWithdrawal(txId: Int) {
        viewModelScope.launch {
            val ok = repository.approveWithdrawal(txId)
            if (ok) {
                _toastMessage.value = "Withdrawal approved!"
                refreshOnlineData()
            }
        }
    }

    fun adminRejectWithdrawal(txId: Int, txAmount: Double, txUser: String) {
        viewModelScope.launch {
            val ok = repository.rejectWithdrawal(txId, txAmount, txUser)
            if (ok) {
                _toastMessage.value = "Withdrawal request rejected. Balance refunded."
                refreshOnlineData()
            }
        }
    }

    fun adminUpdateTournament(id: Int, roomId: String?, roomPass: String?, startTime: String) {
        viewModelScope.launch {
            val ok = repository.updateTournamentRoom(id, roomId, roomPass, startTime)
            if (ok) {
                _toastMessage.value = "Tournament updated successfully."
                refreshOnlineData()
            } else {
                _toastMessage.value = "Failed to update tournament."
            }
        }
    }

    fun adminCreateTournament(tournament: Tournament) {
        viewModelScope.launch {
            val ok = repository.createTournament(tournament)
            if (ok) {
                _toastMessage.value = "Created Tournament: ${tournament.title}"
                refreshOnlineData()
            } else {
                _toastMessage.value = "Failed to create tournament."
            }
        }
    }

    fun adminCompleteGame(gameHistoryId: Int, prizeWon: Double, winnerWhatsapp: String) {
        viewModelScope.launch {
            val ok = repository.completeGame(gameHistoryId, prizeWon, winnerWhatsapp)
            if (ok) {
                _toastMessage.value = "Completed game. Prize of ₹$prizeWon credited to $winnerWhatsapp."
                refreshOnlineData()
            } else {
                _toastMessage.value = "Failed to complete game on server."
            }
        }
    }

    fun performLogout() {
        viewModelScope.launch {
            repository.logout()
            _toastMessage.value = "Logged out successfully."
            _currentScreen.value = "auth"
        }
    }

    // Dynamic UPI management methods
    fun fetchUpiId() {
        viewModelScope.launch {
            try {
                val upi = repository.getGlobalUpiId()
                _dynamicUpiId.value = upi
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error fetching global UPI", e)
            }
        }
    }

    fun adminUpdateUpiId(newUpi: String) {
        viewModelScope.launch {
            val ok = repository.updateGlobalUpiId(newUpi)
            if (ok) {
                _dynamicUpiId.value = newUpi
                _toastMessage.value = "UPI ID updated successfully!"
            } else {
                _toastMessage.value = "Failed to update UPI ID."
            }
        }
    }

    // Admin user search and balance update methods
    fun adminSearchUser(whatsapp: String) {
        viewModelScope.launch {
            if (whatsapp.isBlank()) {
                _toastMessage.value = "Please enter a valid number to search."
                return@launch
            }
            _isRefreshing.value = true
            val user = repository.searchUserAdmin(whatsapp.trim())
            _isRefreshing.value = false
            if (user != null) {
                _searchedUser.value = user
                _toastMessage.value = "Found user: ${user.name}"
            } else {
                _searchedUser.value = null
                _toastMessage.value = "User with number $whatsapp not found."
            }
        }
    }

    fun adminUpdateUserBalance(whatsapp: String, deposit: Double, withdrawal: Double) {
        viewModelScope.launch {
            _isRefreshing.value = true
            val ok = repository.updateUserBalanceAdmin(whatsapp, deposit, withdrawal)
            _isRefreshing.value = false
            if (ok) {
                _toastMessage.value = "User balances updated successfully!"
                // Refresh searched user to show updated numbers
                val user = repository.searchUserAdmin(whatsapp)
                _searchedUser.value = user
                refreshOnlineData()
            } else {
                _toastMessage.value = "Failed to update balances on server."
            }
        }
    }

    // Get tournament registrations
    fun adminFetchRegistrations(tournamentId: Int) {
        viewModelScope.launch {
            try {
                val regs = repository.getTournamentRegistrations(tournamentId)
                _currentTournamentRegistrations.value = regs
            } catch (e: Exception) {
                _toastMessage.value = "Error fetching registrations: ${e.message}"
            }
        }
    }

    // Join Esports tournament with team details
    fun registerTournamentWithTeam(tournamentId: Int, teamName: String, teamMembers: String, onComplete: (Boolean) -> Unit = {}) {
        val user = loggedInUser.value ?: return
        viewModelScope.launch {
            val nameClean = if (teamName.isNotBlank()) teamName.trim() else user.name
            val membersClean = if (teamMembers.isNotBlank()) teamMembers.trim() else user.name
            // Format whatsapp_number as raw_whatsapp|team_name|members
            val formattedWhatsapp = "${user.whatsappNumber}|$nameClean|$membersClean"

            _isRefreshing.value = true
            val errorMsg = repository.registerForTournament(formattedWhatsapp, tournamentId)
            _isRefreshing.value = false

            if (errorMsg != null) {
                _toastMessage.value = errorMsg
                onComplete(false)
            } else {
                _toastMessage.value = "Successfully registered as '$nameClean'!"
                localJoinedIds.add(tournamentId)
                refreshOnlineData()
                onComplete(true)
            }
        }
    }

    // Assign slots range to teams
    fun adminAssignSlots(tournamentId: Int, fromSlot: Int, toSlot: Int) {
        viewModelScope.launch {
            try {
                val regs = repository.getTournamentRegistrations(tournamentId)
                if (regs.isEmpty()) {
                    _toastMessage.value = "No teams have joined this tournament yet."
                    return@launch
                }
                _isRefreshing.value = true
                var currentSlot = fromSlot
                var updatedCount = 0
                for (reg in regs) {
                    if (currentSlot > toSlot) {
                        break
                    }
                    // Registration formatted string check
                    val parts = reg.whatsappNumber.split("|").toMutableList()
                    while (parts.size < 3) {
                        parts.add("")
                    }
                    // Put slot number as 4th element (index 3)
                    if (parts.size == 3) {
                        parts.add("Slot $currentSlot")
                    } else {
                        parts[3] = "Slot $currentSlot"
                    }
                    val formattedString = parts.joinToString("|")
                    val ok = repository.updateRegistrationAdmin(reg.id, formattedString)
                    if (ok) {
                        updatedCount++
                        currentSlot++
                    }
                }
                _isRefreshing.value = false
                _toastMessage.value = "Successfully assigned slot numbers to $updatedCount teams!"
                adminFetchRegistrations(tournamentId)
            } catch (e: Exception) {
                _isRefreshing.value = false
                _toastMessage.value = "Error assigning slots: ${e.message}"
            }
        }
    }

    // Declare position and Reward
    fun adminDeclarePositionAndReward(registrationId: Int, position: String, prizeAmount: Double, rawWhatsapp: String, tournamentTitle: String) {
        viewModelScope.launch {
            _isRefreshing.value = true
            val ok = repository.declarePositionAndReward(registrationId, position, prizeAmount, rawWhatsapp, tournamentTitle)
            _isRefreshing.value = false
            if (ok) {
                _toastMessage.value = "Declared Position $position. Reward of ₹$prizeAmount awarded!"
                refreshOnlineData()
            } else {
                _toastMessage.value = "Failed to award reward."
            }
        }
    }

    // Generic full edit of tournament details
    fun adminUpdateTournamentDetails(
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
    ) {
        viewModelScope.launch {
            _isRefreshing.value = true
            val ok = repository.updateTournamentAdmin(id, game, title, posterRes, entryFee, prizePool, prize1st, prize2nd, prize3rd, prize4th, rules, startTime)
            _isRefreshing.value = false
            if (ok) {
                _toastMessage.value = "Tournament updated successfully!"
                refreshOnlineData()
            } else {
                _toastMessage.value = "Failed to update tournament details."
            }
        }
    }

    // Full Delete Tournament
    fun adminDeleteTournament(id: Int) {
        viewModelScope.launch {
            _isRefreshing.value = true
            val ok = repository.deleteTournament(id)
            _isRefreshing.value = false
            if (ok) {
                _toastMessage.value = "Tournament deleted successfully!"
                refreshOnlineData()
            } else {
                _toastMessage.value = "Failed to delete tournament."
            }
        }
    }

    fun uploadPhoto(base64Image: String, filename: String, mimeType: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = repository.uploadPhoto(base64Image, filename, mimeType)
            _isRefreshing.value = false
            result.onSuccess { url ->
                onResult(url)
            }.onFailure { err ->
                _toastMessage.value = "Upload failed: ${err.message}"
                onResult(null)
            }
        }
    }


    // --- NOTIFICATION SYSTEM STATE & LOGIC ---
    private val _inAppNotifications = MutableStateFlow<List<InAppNotification>>(emptyList())
    val inAppNotifications: StateFlow<List<InAppNotification>> = _inAppNotifications.asStateFlow()

    fun loadInAppNotifications() {
        val sharedPrefs = getApplication<Application>().getSharedPreferences("arena_esports_prefs", android.content.Context.MODE_PRIVATE)
        val jsonStr = sharedPrefs.getString("in_app_notifications", "[]") ?: "[]"
        try {
            val array = org.json.JSONArray(jsonStr)
            val list = mutableListOf<InAppNotification>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    InAppNotification(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        content = obj.getString("content"),
                        timestamp = obj.getLong("timestamp"),
                        isRead = obj.getBoolean("isRead")
                    )
                )
            }
            _inAppNotifications.value = list.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveInAppNotifications(list: List<InAppNotification>) {
        val sharedPrefs = getApplication<Application>().getSharedPreferences("arena_esports_prefs", android.content.Context.MODE_PRIVATE)
        try {
            val array = org.json.JSONArray()
            for (item in list) {
                val obj = org.json.JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("content", item.content)
                    put("timestamp", item.timestamp)
                    put("isRead", item.isRead)
                }
                array.put(obj)
            }
            sharedPrefs.edit().putString("in_app_notifications", array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun markAllNotificationsAsRead() {
        val updated = _inAppNotifications.value.map { it.copy(isRead = true) }
        _inAppNotifications.value = updated
        saveInAppNotifications(updated)
    }

    fun clearNotifications() {
        _inAppNotifications.value = emptyList()
        saveInAppNotifications(emptyList())
    }

    private fun checkNotifications(
        regs: List<com.example.data.Registration>,
        tournaments: List<Tournament>
    ) {
        val context = getApplication<Application>()
        val sharedPrefs = context.getSharedPreferences("arena_esports_prefs", android.content.Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()

        val joinedIds = regs.map { it.tournamentId }.toSet()
        val joinedTournaments = tournaments.filter { joinedIds.contains(it.id) }

        var hasNew = false
        val currentNotifications = _inAppNotifications.value.toMutableList()

        for (tour in joinedTournaments) {
            val reg = regs.find { it.tournamentId == tour.id } ?: continue
            
            // 1. Check Slot Number
            val parts = reg.whatsappNumber.split("|")
            val slotNum = parts.getOrNull(3) ?: ""
            if (slotNum.isNotBlank() && !slotNum.equals("Unassigned", ignoreCase = true)) {
                val slotKey = "notified_slot_${tour.id}"
                val lastNotifiedSlot = sharedPrefs.getString(slotKey, "")
                if (lastNotifiedSlot != slotNum) {
                    val title = "🎮 Slot Number Assigned!"
                    val content = "Tournament '${tour.title}': Your team is assigned to $slotNum."
                    
                    sendSystemNotification(title, content)
                    
                    currentNotifications.add(
                        InAppNotification(
                            id = "slot_${tour.id}_${System.currentTimeMillis()}",
                            title = title,
                            content = content
                        )
                    )
                    editor.putString(slotKey, slotNum)
                    hasNew = true
                }
            }

            // 2. Check Room ID and Password
            val roomId = tour.roomId
            val roomPass = tour.roomPassword
            if (!roomId.isNullOrBlank() || !roomPass.isNullOrBlank()) {
                val rId = roomId ?: "N/A"
                val rPass = roomPass ?: "N/A"
                val roomVal = "${rId}|${rPass}"
                val roomKey = "notified_room_${tour.id}"
                val lastNotifiedRoom = sharedPrefs.getString(roomKey, "")
                
                if (lastNotifiedRoom != roomVal) {
                    val title = "🔑 Room Credentials Available!"
                    val content = "Tournament '${tour.title}': Room ID is $rId, Password is $rPass. Join soon!"
                    
                    sendSystemNotification(title, content)
                    
                    currentNotifications.add(
                        InAppNotification(
                            id = "room_${tour.id}_${System.currentTimeMillis()}",
                            title = title,
                            content = content
                        )
                    )
                    editor.putString(roomKey, roomVal)
                    hasNew = true
                }
            }
        }

        if (hasNew) {
            editor.apply()
            _inAppNotifications.value = currentNotifications.sortedByDescending { it.timestamp }
            saveInAppNotifications(currentNotifications)
        }
    }

    private fun sendSystemNotification(title: String, content: String) {
        val context = getApplication<Application>()
        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "Arena Esports Updates"
            val desc = "Notifications for Room credentials and Slot numbers"
            val channel = android.app.NotificationChannel("arena_esports_channel", name, android.app.NotificationManager.IMPORTANCE_HIGH).apply {
                description = desc
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = androidx.core.app.NotificationCompat.Builder(context, "arena_esports_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}

data class InAppNotification(
    val id: String,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
