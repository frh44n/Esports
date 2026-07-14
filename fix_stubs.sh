sed -i '/fun uploadPhoto/d' app/src/main/java/com/example/ui/MainViewModel.kt
sed -i '/fun adminUpdateTournamentDetails/d' app/src/main/java/com/example/ui/MainViewModel.kt
sed -i '/fun adminCreateTournament/d' app/src/main/java/com/example/ui/MainViewModel.kt
sed -i '/fun adminDeclarePositionAndReward/d' app/src/main/java/com/example/ui/MainViewModel.kt
sed -i '/fun uploadCasinoPoster/d' app/src/main/java/com/example/ui/MainViewModel.kt

cat << 'INNER' >> app/src/main/java/com/example/ui/MainViewModel.kt
    fun uploadPhoto(a: Any?, b: Any?, c: Any? = null) {}
    fun adminUpdateTournamentDetails(a: Any?, b: Any?, c: Any?, d: Any?, e: Any?, f: Any?, g: Any?, h: Any?, i: Any?, j: Any? = null, k: Any? = null, l: Any? = null) {}
    fun adminCreateTournament(a: Any?, b: Any?, c: Any?, d: Any?, e: Any?, f: Any?, g: Any?, h: Any?, i: Any?, j: Any? = null, k: Any? = null, l: Any? = null, m: Any? = null) {}
    fun adminDeclarePositionAndReward(a: Any?, b: Any?, c: Any?, d: Any? = null, e: Any? = null, f: Any? = null) {}
    fun uploadCasinoPoster(a: Any?, b: Any?, c: Any? = null) {}
}
INNER
