cat << 'INNER' > temp_timer.kt

    LaunchedEffect(currentTurn, hasRolledThisTurn) {
        turnSecondsLeft = 8
        while (turnSecondsLeft > 0) {
            delay(1000)
            turnSecondsLeft--
        }
        if (currentTurn == 1) {
            p1Lives--
            if (p1Lives <= 0) {
                onGameFinished(p1Points, false)
            } else {
                currentTurn = 2
                hasRolledThisTurn = false
                playablePawns.clear()
                executeOpponentTurn()
            }
        }
    }

INNER
sed -i '1123r temp_timer.kt' app/src/main/java/com/example/ui/LudoScreen.kt
