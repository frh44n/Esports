sed -i 's/fun performSignUp(a: String, b: String, c: String, d: String, e: String) {}/fun performSignUp(a: String, b: String, c: String, d: String, e: String?, f: String? = null) {}/' app/src/main/java/com/example/ui/MainViewModel.kt
sed -i 's/fun submitDeposit(a: String, b: String) {}/fun submitDeposit(a: String, b: Double, c: String? = null) {}/' app/src/main/java/com/example/ui/MainViewModel.kt
sed -i 's/fun submitWithdrawal(a: String, b: String, c: String) {}/fun submitWithdrawal(a: String, b: Double, c: String? = null) {}/' app/src/main/java/com/example/ui/MainViewModel.kt
