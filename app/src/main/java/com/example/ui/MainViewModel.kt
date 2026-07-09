package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.GameHistory
import com.example.data.Tournament
import com.example.data.Transaction
import com.example.data.User
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

    // Loading / refreshing states
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Selected game category inside Esports section
    private val _selectedEsportsGame = MutableStateFlow("BGMI") // BGMI, PUBG, FREEFIRE
    val selectedEsportsGame: StateFlow<String> = _selectedEsportsGame.asStateFlow()

    // Screen navigation state
    private val _currentScreen = MutableStateFlow<String>("splash") // splash, auth, home, admin
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AppRepository(database.appDao())

        loggedInUser = repository.loggedInUserFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        // Observe login state and route accordingly after exactly 1 second (1000 ms) delay
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            repository.loggedInUserFlow.collect { user ->
                if (user != null) {
                    _currentScreen.value = "home"
                    refreshOnlineData()
                } else {
                    _currentScreen.value = "auth"
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

    /**
     * Freshly fetches all online data from Supabase for the current logged in session
     */
    fun refreshOnlineData() {
        val user = loggedInUser.value ?: return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // Sync latest profile balances from Supabase
                val updatedUser = repository.syncProfile(user.whatsappNumber)

                // Sync Tournaments and check if the user is registered
                val rawTournaments = repository.getTournaments()
                val regs = repository.getRegistrations(user.whatsappNumber)
                val joinedIds = regs.map { it.tournamentId }.toSet()
                _allTournaments.value = rawTournaments.map {
                    it.copy(isJoined = joinedIds.contains(it.id))
                }

                // Sync Personal Transactions
                _personalTransactions.value = repository.getTransactions(user.whatsappNumber, _txLimit.value)

                // Sync Personal Game Histories
                _gameHistories.value = repository.getGameHistories(user.whatsappNumber)

                // Sync Admin list if the user is the admin
                if (updatedUser?.whatsappNumber == "9876543210") {
                    _allTransactionsAdmin.value = repository.getAllTransactionsAdmin()
                }
            } catch (e: Exception) {
                _toastMessage.value = "Sync Error: ${e.message}"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Sign Up a user online via Supabase Auth
     */
    fun performSignUp(whatsappNumber: String, name: String, password: String, confirmPassword: String, referralCode: String?) {
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
            val result = repository.signUp(whatsappNumber, name, password, referralCode)
            _isRefreshing.value = false

            result.onSuccess {
                _toastMessage.value = "Welcome! Signed Up and logged in successfully."
                _currentScreen.value = "home"
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

            result.onSuccess {
                _toastMessage.value = "Welcome Back! Logged in successfully."
                _currentScreen.value = "home"
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
            val success = repository.submitDeposit(user.whatsappNumber, amount, upiId, refNumber)
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
            val errorMsg = repository.submitWithdrawal(user.whatsappNumber, amount, upiId)
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
}
