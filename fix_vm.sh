sed -i '$d' app/src/main/java/com/example/ui/MainViewModel.kt
cat << 'INNER' >> app/src/main/java/com/example/ui/MainViewModel.kt
    val inAppNotifications = MutableStateFlow<List<Any>>(emptyList())
    val showMinesGame = MutableStateFlow(false)
    fun setShowMinesGame(show: Boolean) { showMinesGame.value = show }
    fun performSignUp(a: String, b: String, c: String, d: String, e: String) {}
    fun performLogin(a: String, b: String) {}
    fun markAllNotificationsAsRead() {}
    fun clearNotifications() {}
    fun selectEsportsGame(game: String) {}
    fun submitDeposit(a: String, b: String) {}
    fun submitWithdrawal(a: String, b: String, c: String) {}
    fun performLogout() {}
    fun clearToast() { _toastMessage.value = null }
}
INNER
