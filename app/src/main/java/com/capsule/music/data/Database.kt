package com.capsule.music.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "playback_events")
data class PlaybackEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "track_title") val trackTitle: String,
    @ColumnInfo(name = "artist_name") val artistName: String,
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
    @ColumnInfo(name = "timestamp") val timestamp: Long
)

@Dao
interface PlaybackDao {
    @Insert
    suspend fun insertEvent(event: PlaybackEvent)

    @Query("SELECT * FROM playback_events ORDER BY timestamp DESC LIMIT 100")
    fun getAllEvents(): Flow<List<PlaybackEvent>>

    @Query("SELECT COUNT(*) FROM playback_events")
    fun getTotalTracksCount(): Flow<Int>

    @Query("SELECT IFNULL(SUM(duration_ms), 0) FROM playback_events")
    fun getTotalListeningTimeMs(): Flow<Long>
}

@Database(entities = [PlaybackEvent::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playbackDao(): PlaybackDao
}
