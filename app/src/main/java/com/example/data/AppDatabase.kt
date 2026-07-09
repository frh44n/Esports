package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // User Queries
    @Query("SELECT * FROM users WHERE isLoggedIn = 1 LIMIT 1")
    fun getLoggedInUserFlow(): Flow<User?>

    @Query("SELECT * FROM users WHERE isLoggedIn = 1 LIMIT 1")
    suspend fun getLoggedInUser(): User?

    @Query("SELECT * FROM users WHERE whatsappNumber = :whatsappNumber LIMIT 1")
    suspend fun getUserByWhatsapp(whatsappNumber: String): User?

    @Query("SELECT * FROM users WHERE ownReferralCode = :referralCode LIMIT 1")
    suspend fun getUserByReferralCode(referralCode: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Query("UPDATE users SET isLoggedIn = 0")
    suspend fun logoutAllUsers()

    // Tournament Queries
    @Query("SELECT * FROM tournaments ORDER BY id DESC")
    fun getAllTournamentsFlow(): Flow<List<Tournament>>

    @Query("SELECT * FROM tournaments WHERE id = :id LIMIT 1")
    suspend fun getTournamentById(id: Int): Tournament?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTournament(tournament: Tournament)

    @Update
    suspend fun updateTournament(tournament: Tournament)

    @Query("DELETE FROM tournaments WHERE id = :id")
    suspend fun deleteTournamentById(id: Int)

    // Registration Queries
    @Query("SELECT * FROM registrations WHERE whatsappNumber = :whatsappNumber AND tournamentId = :tournamentId LIMIT 1")
    suspend fun getRegistration(whatsappNumber: String, tournamentId: Int): Registration?

    @Query("SELECT * FROM registrations WHERE tournamentId = :tournamentId")
    suspend fun getRegistrationsForTournament(tournamentId: Int): List<Registration>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegistration(registration: Registration)

    // Transaction Queries
    @Query("SELECT * FROM transactions WHERE whatsappNumber = :whatsappNumber ORDER BY timestamp DESC LIMIT :limit")
    fun getTransactionsFlow(whatsappNumber: String, limit: Int): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlowAdmin(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Int): Transaction?

    // Game History Queries
    @Query("SELECT * FROM game_histories WHERE whatsappNumber = :whatsappNumber ORDER BY timestamp DESC LIMIT 20")
    fun getGameHistoriesFlow(whatsappNumber: String): Flow<List<GameHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGameHistory(gameHistory: GameHistory)
}

@Database(
    entities = [
        User::class,
        Tournament::class,
        Registration::class,
        Transaction::class,
        GameHistory::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "arena_esports_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
