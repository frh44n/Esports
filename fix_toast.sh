cat << 'INNER' >> app/src/main/java/com/example/ui/MainViewModel.kt
    fun showToast(message: String) { _toastMessage.value = message }
INNER
