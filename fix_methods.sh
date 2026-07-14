cat << 'INNER' >> app/src/main/java/com/example/ui/MainViewModel.kt
    fun loadInAppNotifications() {}
    fun uploadPhoto(uri: android.net.Uri, onResult: (String?) -> Unit) { onResult(null) }
    fun adminUpdateTournamentDetails(id: Int, title: String, prizePool: String, entryFee: String, prize1st: String, prize2nd: String, prize3rd: String, rules: String, posterUrl: String?) {}
    fun adminCreateTournament(title: String, prizePool: String, entryFee: String, prize1st: String, prize2nd: String, prize3rd: String, rules: String, posterUrl: String?, is2v2: Boolean, game: String = "Ludo") {}
    fun adminDeleteTournament(id: Int) {}
    fun adminStartTournament(id: Int) {}
    fun adminFinishTournament(id: Int) {}
    fun adminFetchRegistrations(id: Int) {}
    fun adminUpdateTournament(id: Int, roomId: String, roomPass: String, startTime: String) {}
    fun adminAssignSlots(id: Int, from: Int, to: Int) {}
    fun adminDeclarePositionAndReward(id: Int, regId: Int, position: Int) {}
    fun loadCasinoGames() {}
    fun adminUpdateCasinoGame(id: Int, enabled: Boolean, posterUrl: String?) {}
    fun uploadCasinoPoster(uri: android.net.Uri, onResult: (String?) -> Unit) { onResult(null) }
    fun loadAdminStats() {}
    fun refreshOnlineData() {}
    fun registerTournamentWithTeam(tournamentId: Int, teamName: String, teamMembers: String, onComplete: (Boolean) -> Unit = {}) { onComplete(true) }
    fun completeLudoGame(tournamentId: Int, score: Int, isWinner: Boolean, onComplete: (Double) -> Unit) { onComplete(0.0) }
    fun setScreen(screen: String) { _currentScreen.value = screen }
}
INNER
