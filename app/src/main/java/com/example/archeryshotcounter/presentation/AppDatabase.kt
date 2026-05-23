package com.example.archeryshotcounter.presentation

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun getAll(): List<Session>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(session: Session)

    @Delete
    fun delete(session: Session)
}

@Dao
interface ShotDao {
    @Insert
    fun insert(shot: Shot)
}

@Database(entities = [Session::class, Shot::class], version = 1, exportSchema = false)
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
                ).build().also { instance = it }
            }
    }
}
