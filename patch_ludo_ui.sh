cat << 'INNER' > patch.py
import re

with open('app/src/main/java/com/example/ui/LudoMatchmakingScreen.kt', 'r') as f:
    content = f.read()

target = """            // Check if admin accepted
            if (user != null && activeMatches.containsKey(user!!.whatsappNumber)) {
                foundOpponent = activeMatches[user!!.whatsappNumber]
            }"""

replacement = """            // Poll server every second
            if (user != null && elapsed % 1000 == 0) {
                val opponent = viewModel.checkLudoMatchStatus(user!!.whatsappNumber)
                if (opponent != null) {
                    foundOpponent = opponent
                }
            }"""

content = content.replace(target, replacement)
with open('app/src/main/java/com/example/ui/LudoMatchmakingScreen.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/ui/AppUi.kt', 'r') as f:
    content2 = f.read()

target2 = "val ludoRequests by viewModel.ludoMatchRequests.collectAsStateWithLifecycle()"

replacement2 = """val ludoRequests by viewModel.ludoMatchRequests.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        while(true) {
            viewModel.fetchAdminLudoRequests()
            kotlinx.coroutines.delay(2000)
        }
    }"""
content2 = content2.replace(target2, replacement2)

with open('app/src/main/java/com/example/ui/AppUi.kt', 'w') as f:
    f.write(content2)

INNER
python3 patch.py
