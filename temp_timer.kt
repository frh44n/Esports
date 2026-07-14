
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

