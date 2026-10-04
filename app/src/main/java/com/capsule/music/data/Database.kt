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

data class TrackStat(
    @ColumnInfo(name = "track_title") val trackTitle: String,
    @ColumnInfo(name = "artist_name") val artistName: String,
    @ColumnInfo(name = "play_count") val playCount: Int,
    @ColumnInfo(name = "total_ms") val totalMs: Long
)

data class ArtistStat(
    @ColumnInfo(name = "artist_name") val artistName: String,
    @ColumnInfo(name = "play_count") val playCount: Int,
    @ColumnInfo(name = "total_ms") val totalMs: Long
)

data class DayStat(
    @ColumnInfo(name = "day") val day: String,
    @ColumnInfo(name = "total_ms") val totalMs: Long
)

@Dao
interface PlaybackDao {
    @Insert
    suspend fun insertEvent(event: PlaybackEvent)

    @Insert
    suspend fun insertAll(events: List<PlaybackEvent>)

    @Query("SELECT * FROM playback_events ORDER BY timestamp DESC LIMIT 50")
    fun getAllEvents(): Flow<List<PlaybackEvent>>

    @Query("SELECT IFNULL(SUM(duration_ms), 0) FROM playback_events WHERE timestamp >= :sinceTimestamp")
    suspend fun getMinutesSince(sinceTimestamp: Long): Long

    // Топ 5 треков за период
    @Query("""
        SELECT track_title, artist_name, COUNT(*) as play_count, SUM(duration_ms) as total_ms
        FROM playback_events
        WHERE timestamp >= :sinceTimestamp
        GROUP BY track_title, artist_name
        ORDER BY total_ms DESC
        LIMIT 5
    """)
    suspend fun getTopTracks(sinceTimestamp: Long): List<TrackStat>

    // Топ 5 артистов за период
    @Query("""
        SELECT artist_name, COUNT(*) as play_count, SUM(duration_ms) as total_ms
        FROM playback_events
        WHERE timestamp >= :sinceTimestamp
        GROUP BY artist_name
        ORDER BY total_ms DESC
        LIMIT 5
    """)
    suspend fun getTopArtists(sinceTimestamp: Long): List<ArtistStat>

    // Статистика по дням за последний месяц
    @Query("""
        SELECT date(timestamp / 1000, 'unixepoch', 'localtime') as day, SUM(duration_ms) as total_ms
        FROM playback_events
        WHERE timestamp >= :sinceTimestamp
        GROUP BY day
        ORDER BY day ASC
    """)
    suspend fun getDailyStats(sinceTimestamp: Long): List<DayStat>

    // Проверка стрика артиста в предыдущем месяце
    @Query("""
        SELECT COUNT(*) FROM (
            SELECT artist_name
            FROM playback_events
            WHERE timestamp BETWEEN :startPrevMonth AND :endPrevMonth
            GROUP BY artist_name
            ORDER BY SUM(duration_ms) DESC
            LIMIT 5
        ) WHERE artist_name = :artist
    """)
    suspend fun isArtistInPrevMonthTop(artist: String, startPrevMonth: Long, endPrevMonth: Long): Int

    @Query("DELETE FROM playback_events")
    suspend fun clearAll()
}

@Database(entities = [PlaybackEvent::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playbackDao(): PlaybackDao
}