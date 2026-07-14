sed -i '$d' app/src/main/java/com/example/ui/MainViewModel.kt
cat << 'INNER' >> app/src/main/java/com/example/ui/MainViewModel.kt
    val minesActiveGame: StateFlow<Any?> = MutableStateFlow(null)
    val minesLoading: StateFlow<Boolean> = MutableStateFlow(false)
    fun checkActiveMinesGame() {}
    fun revealMinesTile(a: Int) {}
    fun cashoutMinesGame() {}
    fun resetMinesSessionState() {}
    fun startMinesGame(a: Double, b: Int) {}
}
INNER
