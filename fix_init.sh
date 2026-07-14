sed -i '141,147c\
    init {\
        val database = AppDatabase.getDatabase(application)\
        repository = AppRepository(database.appDao())\
        loadInAppNotifications()\
\
        loggedInUser = repository.loggedInUserFlow.stateIn(\
            scope = viewModelScope,\
            started = SharingStarted.WhileSubscribed(5000),\
            initialValue = null\
        )\
    }\
' app/src/main/java/com/example/ui/MainViewModel.kt
