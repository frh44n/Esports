sed -i 's/val inAppNotifications = MutableStateFlow<List<InAppNotification>>(emptyList())/val inAppNotifications: StateFlow<List<InAppNotification>> = MutableStateFlow(emptyList())/' app/src/main/java/com/example/ui/MainViewModel.kt
sed -i 's/val timestamp: String/val timestamp: Long/' app/src/main/java/com/example/ui/MainViewModel.kt
