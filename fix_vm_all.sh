sed -i '/val inAppNotifications/d' app/src/main/java/com/example/ui/MainViewModel.kt
sed -i 's/fun submitDeposit(a: String, b: Double, c: String? = null) {}/fun submitDeposit(a: Double, b: String, c: String? = null) {}/' app/src/main/java/com/example/ui/MainViewModel.kt
sed -i 's/fun submitWithdrawal(a: String, b: Double, c: String? = null) {}/fun submitWithdrawal(a: Double, b: String, c: String? = null) {}/' app/src/main/java/com/example/ui/MainViewModel.kt

sed -i '$d' app/src/main/java/com/example/ui/MainViewModel.kt
cat << 'INNER' >> app/src/main/java/com/example/ui/MainViewModel.kt
    val inAppNotifications: StateFlow<List<InAppNotification>> = MutableStateFlow(emptyList())
}
INNER
