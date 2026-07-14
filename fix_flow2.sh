sed -i 's/val showMinesGame = MutableStateFlow(false)/val showMinesGame: StateFlow<Boolean> = MutableStateFlow(false)/' app/src/main/java/com/example/ui/MainViewModel.kt
