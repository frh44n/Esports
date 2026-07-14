sed -i '183,188d' app/src/main/java/com/example/ui/MainViewModel.kt
cat << 'INNER' >> app/src/main/java/com/example/ui/MainViewModel.kt
    val inAppNotifications = MutableStateFlow<List<InAppNotification>>(emptyList())
}
data class InAppNotification(val title: String, val content: String, val timestamp: Long, val isRead: Boolean = false)
INNER
