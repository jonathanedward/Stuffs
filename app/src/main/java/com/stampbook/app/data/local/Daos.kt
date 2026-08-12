package com.stampbook.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY start_date DESC, id DESC")
    fun observeAll(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :id")
    fun observeById(id: Long): Flow<TripEntity?>

    @Query("SELECT COUNT(*) FROM trips")
    fun observeCount(): Flow<Int>

    @Upsert
    suspend fun upsert(trip: TripEntity): Long

    @Delete
    suspend fun delete(trip: TripEntity)

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface StampDao {
    @Query("SELECT * FROM stamps ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<StampEntity>>

    @Query("SELECT * FROM stamps WHERE trip_id = :tripId ORDER BY date ASC, id ASC")
    fun observeForTrip(tripId: Long): Flow<List<StampEntity>>

    @Query("SELECT * FROM stamps WHERE id = :id")
    fun observeById(id: Long): Flow<StampEntity?>

    @Query("SELECT * FROM stamps WHERE country_code = :countryCode ORDER BY date ASC")
    fun observeForCountry(countryCode: String): Flow<List<StampEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(stamp: StampEntity): Long

    @Update
    suspend fun update(stamp: StampEntity)

    @Query("DELETE FROM stamps WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE stamps SET trip_id = :tripId WHERE id = :stampId")
    suspend fun assignToTrip(stampId: Long, tripId: Long?)
}
