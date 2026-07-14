cat << 'INNER' > patch.py
import re

with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'r') as f:
    content = f.read()

target = """    init {
        val database = AppDatabase.getDatabase(application)
        repository = AppRepository(database.appDao())
        loadInAppNotifications()
        loggedInUser = repository.loggedInUserFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    }"""

replacement = """    init {
        val database = AppDatabase.getDatabase(application)
        repository = AppRepository(database.appDao())
        loadInAppNotifications()
        loggedInUser = repository.loggedInUserFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
        viewModelScope.launch {
            kotlinx.coroutines.delay(1500)
            if (loggedInUser.value != null) {
                _currentScreen.value = "home"
            } else {
                _currentScreen.value = "auth"
            }
        }
    }"""

content = content.replace(target, replacement)
with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'w') as f:
    f.write(content)
INNER
python3 patch.py
