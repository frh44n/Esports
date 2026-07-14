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
import com.example.data.InAppNotification
import com.example.data.AdminStats
import kotlinx.coroutines.Dispatchers
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

    private val _casinoGames = MutableStateFlow<List<com.example.data.CasinoGame>>(emptyList())
    val casinoGames: StateFlow<List<com.example.data.CasinoGame>> = _casinoGames.asStateFlow()

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

    // Referrals States
    private val _referredUsers = MutableStateFlow<List<com.example.data.ReferredUser>>(emptyList())
    val referredUsers: StateFlow<List<com.example.data.ReferredUser>> = _referredUsers.asStateFlow()

    private val _referredTotalCount = MutableStateFlow(0)
    val referredTotalCount: StateFlow<Int> = _referredTotalCount.asStateFlow()

    private val _isLoadingReferred = MutableStateFlow(false)
    val isLoadingReferred: StateFlow<Boolean> = _isLoadingReferred.asStateFlow()

    private val _operatingTxIds = MutableStateFlow<Set<Int>>(emptySet())
    val operatingTxIds: StateFlow<Set<Int>> = _operatingTxIds.asStateFlow()

    private var currentReferredPage = 1
    private var isLastReferredPage = false

    fun loadInitialReferredUsers() {
        val user = loggedInUser.value ?: return
        viewModelScope.launch {
            _isLoadingReferred.value = true
            currentReferredPage = 1
            isLastReferredPage = false
            val response = repository.getReferredUsers(user.whatsappNumber, page = 1, limit = 20)
            if (response.success) {
                _referredUsers.value = response.users
                _referredTotalCount.value = response.total
                if (response.users.size < 20) {
                    isLastReferredPage = true
                }
            } else {
                _referredUsers.value = emptyList()
                _referredTotalCount.value = 0
                isLastReferredPage = true
            }
            _isLoadingReferred.value = false
        }
    }

    fun loadMoreReferredUsers() {
        val user = loggedInUser.value ?: return
        if (_isLoadingReferred.value || isLastReferredPage) return
        viewModelScope.launch {
            _isLoadingReferred.value = true
            val nextPage = currentReferredPage + 1
            val response = repository.getReferredUsers(user.whatsappNumber, page = nextPage, limit = 20)
            if (response.success) {
                if (response.users.isNotEmpty()) {
                    currentReferredPage = nextPage
                    _referredUsers.value = _referredUsers.value + response.users
                }
                _referredTotalCount.value = response.total
                if (response.users.size < 20) {
                    isLastReferredPage = true
                }
            } else {
                isLastReferredPage = true
            }
            _isLoadingReferred.value = false
        }
    }

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
    }

    fun loadInAppNotifications() {}
    fun adminDeleteTournament(id: Int) {}
    fun adminStartTournament(id: Int) {}
    fun adminFinishTournament(id: Int) {}
    fun adminFetchRegistrations(id: Int) {}
    fun adminUpdateTournament(id: Int, roomId: String, roomPass: String, startTime: String) {}
    fun adminAssignSlots(id: Int, from: Int, to: Int) {}
    fun loadCasinoGames() {}
    fun adminUpdateCasinoGame(id: Int, enabled: Boolean, posterUrl: String?) {}
    fun loadAdminStats() {}
    fun refreshOnlineData() {}
    fun registerTournamentWithTeam(tournamentId: Int, teamName: String, teamMembers: String, onComplete: (Boolean) -> Unit = {}) { onComplete(true) }
    fun completeLudoGame(tournamentId: Int, score: Int, isWinner: Boolean, onComplete: (Double) -> Unit) { onComplete(0.0) }
    fun setScreen(screen: String) { _currentScreen.value = screen }
    fun showToast(message: String) { _toastMessage.value = message }
    fun clearToast() { _toastMessage.value = null }
    fun uploadPhoto(a: Any?, b: Any?, c: Any? = null) {}
    fun adminUpdateTournamentDetails(a: Any?, b: Any?, c: Any?, d: Any?, e: Any?, f: Any?, g: Any?, h: Any?, i: Any?, j: Any? = null, k: Any? = null, l: Any? = null) {}
    fun adminCreateTournament(a: Any?, b: Any?, c: Any?, d: Any?, e: Any?, f: Any?, g: Any?, h: Any?, i: Any?, j: Any? = null, k: Any? = null, l: Any? = null, m: Any? = null) {}
    fun adminDeclarePositionAndReward(a: Any?, b: Any?, c: Any?, d: Any? = null, e: Any? = null, f: Any? = null) {}
    fun uploadCasinoPoster(a: Any?, b: Any?, c: Any? = null) {}
    val showMinesGame = MutableStateFlow(false)
    fun setShowMinesGame(show: Boolean) { showMinesGame.value = show }
    fun performSignUp(a: String, b: String, c: String, d: String, e: String?, f: String? = null) {}
    fun performLogin(a: String, b: String) {}
    fun markAllNotificationsAsRead() {}
    fun clearNotifications() {}
    fun selectEsportsGame(game: String) {}
    fun submitDeposit(a: Double, b: String, c: String? = null) {}
    fun submitWithdrawal(a: Double, b: String, c: String? = null) {}
    fun performLogout() {}
    val inAppNotifications = MutableStateFlow<List<InAppNotification>>(emptyList())
    val minesActiveGame: StateFlow<com.example.data.MinesGame?> = MutableStateFlow(null)
    val minesLoading: StateFlow<Boolean> = MutableStateFlow(false)
    fun checkActiveMinesGame() {}
    fun revealMinesTile(tileIndex: Int, onGemRevealed: () -> Unit = {}, onMineHit: () -> Unit = {}, onAutoCashout: () -> Unit = {}) {}
    fun cashoutMinesGame(onComplete: () -> Unit = {}) { onComplete() }
    fun resetMinesSessionState() {}
    fun startMinesGame(a: Double, b: Int) {}

    val ludoMatchRequests = MutableStateFlow<List<LudoMatchRequest>>(emptyList())
    val activeLudoMatches = MutableStateFlow<Map<String, String>>(emptyMap())
    val adminActiveLudoTournament = MutableStateFlow<com.example.data.Tournament?>(null)
    val adminActiveLudoOpponentName = MutableStateFlow<String>("")

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

    suspend fun checkLudoMatchStatus(userId: String): String? {
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            val res = com.example.data.SupabaseClient.getLudoMatchStatus(userId)
            if (res != null && res.optString("status") == "MATCHED") {
                res.optString("opponentName")
            } else null
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
                        posterRes = "",
                        entryFee = 0.0,
                        prizePool = 0.0,
                        prize1st = 0.0,
                        prize2nd = 0.0,
                        prize3rd = 0.0,
                        prize4th = 0.0,
                        rules = "",
                        roomId = null,
                        roomPassword = null,
                        startTime = "Live"
                    )
                    list.add(LudoMatchRequest(id, name, t, time))
                }
                ludoMatchRequests.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}

data class LudoMatchRequest(val userId: String, val userName: String, val tournament: com.example.data.Tournament, val timestamp: Long)

