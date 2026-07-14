sed -i 's/fun revealMinesTile(a: Int) {}/fun revealMinesTile(tileIndex: Int, onGemRevealed: () -> Unit = {}, onMineHit: () -> Unit = {}, onAutoCashout: () -> Unit = {}) {}/' app/src/main/java/com/example/ui/MainViewModel.kt
sed -i 's/fun cashoutMinesGame() {}/fun cashoutMinesGame(onComplete: () -> Unit = {}) { onComplete() }/' app/src/main/java/com/example/ui/MainViewModel.kt
