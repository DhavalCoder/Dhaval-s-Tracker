package com.lifeos.app

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "study_topics", indices = [Index("category")])
data class TopicEntity(
    @PrimaryKey val id: Long,
    val category: String,
    val title: String,
    val completed: Boolean = false,
    val description: String = "",
    val notes: String = ""
)

@Entity(tableName = "workout_logs", indices = [Index("day"), Index("exercise")])
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exercise: String,
    val sets: Int,
    val reps: Int,
    val weight: Double,
    val day: String
)

@Dao
interface LifeOSDao {
    @Query("SELECT * FROM study_topics ORDER BY id")
    fun getTopics(): List<TopicEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun putTopics(items: List<TopicEntity>)

    @Query("DELETE FROM study_topics")
    fun clearTopics()

    @Query("SELECT COUNT(*) FROM study_topics")
    fun topicCount(): Int

    @Query("SELECT * FROM workout_logs ORDER BY id")
    fun getWorkoutLogs(): List<WorkoutEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWorkout(log: WorkoutEntity)

    @Query("DELETE FROM workout_logs WHERE id = :id")
    fun deleteWorkout(id: Long)
}

@Database(entities = [TopicEntity::class, WorkoutEntity::class], version = 1, exportSchema = false)
abstract class LifeOSDatabase : RoomDatabase() {
    abstract fun lifeOSDao(): LifeOSDao

    companion object {
        @Volatile private var INSTANCE: LifeOSDatabase? = null

        fun get(context: Context): LifeOSDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LifeOSDatabase::class.java,
                    "lifeos_private.db"
                ).build().also { INSTANCE = it }
            }
    }
}
