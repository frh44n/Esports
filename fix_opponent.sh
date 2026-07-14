sed -i 's/fun executeBotTurn/fun executeOpponentTurn/' app/src/main/java/com/example/ui/LudoScreen.kt
sed -i 's/val botName = remember {/val opponentName = remember {/' app/src/main/java/com/example/ui/LudoScreen.kt
sed -i 's/"Opponent"/"Opponent"/' app/src/main/java/com/example/ui/LudoScreen.kt
sed -i 's/botName/opponentName/g' app/src/main/java/com/example/ui/LudoScreen.kt
sed -i 's/executeBotTurn()/executeOpponentTurn()/' app/src/main/java/com/example/ui/LudoScreen.kt
