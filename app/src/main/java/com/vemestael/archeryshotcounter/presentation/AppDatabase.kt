package com.vemestael.archeryshotcounter.presentation

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.RoomDatabase.JournalMode

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun getAll(): List<Session>

    @Upsert
    fun insertOrUpdate(session: Session)

    @Delete
    fun delete(session: Session)
}

@Dao
interface ShotDao {
    @Insert
    fun insert(shot: Shot)

    @Query("SELECT * FROM shots WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    fun getBySession(sessionId: Long): List<Shot>

    @Query("DELETE FROM shots WHERE id IN (SELECT id FROM shots WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT :count)")
    fun deleteLatest(sessionId: Long, count: Int)
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `shots_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionId` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `magnitude` REAL, FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON DELETE CASCADE)")
        database.execSQL("INSERT INTO `shots_new` SELECT `id`, `sessionId`, `timestamp`, `magnitude` FROM `shots`")
        database.execSQL("DROP TABLE `shots`")
        database.execSQL("ALTER TABLE `shots_new` RENAME TO `shots`")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_shots_sessionId` ON `shots` (`sessionId`)")
    }
}

@Database(entities = [Session::class, Shot::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun shotDao(): ShotDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "archery.db"
                ).addMigrations(MIGRATION_1_2)
                    .setJournalMode(JournalMode.TRUNCATE)
                    .build().also { instance = it }
            }
    }
}
