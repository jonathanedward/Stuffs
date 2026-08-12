package com.stampbook.app.data

import com.stampbook.app.core.StampStyles
import com.stampbook.app.data.local.StampDao
import com.stampbook.app.data.local.StampEntity
import com.stampbook.app.data.local.TripDao
import com.stampbook.app.data.local.TripEntity
import com.stampbook.app.data.model.PassportStats
import com.stampbook.app.data.model.Stamp
import com.stampbook.app.data.model.Trip
import com.stampbook.app.data.model.TripWithStamps
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class StampbookRepository(
    private val tripDao: TripDao,
    private val stampDao: StampDao,
) {

    /** Newest first: the passport opens on the most recent page. */
    val stamps: Flow<List<Stamp>> = stampDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    val trips: Flow<List<Trip>> = tripDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    val stats: Flow<PassportStats> =
        combine(stamps, tripDao.observeCount()) { stamps, tripCount ->
            PassportStats.from(stamps, tripCount)
        }

    fun tripWithStamps(tripId: Long): Flow<TripWithStamps?> =
        combine(tripDao.observeById(tripId), stampDao.observeForTrip(tripId)) { trip, stamps ->
            trip?.let { TripWithStamps(it.toDomain(), stamps.map { row -> row.toDomain() }) }
        }

    fun stamp(stampId: Long): Flow<Stamp?> =
        stampDao.observeById(stampId).map { it?.toDomain() }

    suspend fun addStamp(
        countryCode: String,
        city: String?,
        date: LocalDate,
        note: String?,
        tripId: Long?,
        seed: Int = StampStyles.newSeed(),
    ): Long = stampDao.insert(
        StampEntity(
            tripId = tripId,
            countryCode = countryCode,
            city = city?.trim()?.takeIf { it.isNotEmpty() },
            date = date.toEpochDay(),
            note = note?.trim()?.takeIf { it.isNotEmpty() },
            seed = seed,
        ),
    )

    /** Keeps the existing seed so editing a stamp never changes how it looks. */
    suspend fun updateStamp(stamp: Stamp) = stampDao.update(stamp.toEntity())

    suspend fun deleteStamp(stampId: Long) = stampDao.deleteById(stampId)

    suspend fun assignStampToTrip(stampId: Long, tripId: Long?) =
        stampDao.assignToTrip(stampId, tripId)

    suspend fun saveTrip(trip: Trip): Long = tripDao.upsert(trip.toEntity())

    suspend fun deleteTrip(tripId: Long) = tripDao.deleteById(tripId)
}

private fun StampEntity.toDomain() = Stamp(
    id = id,
    tripId = tripId,
    countryCode = countryCode,
    city = city,
    date = LocalDate.ofEpochDay(date),
    note = note,
    seed = seed,
)

private fun Stamp.toEntity() = StampEntity(
    id = id,
    tripId = tripId,
    countryCode = countryCode,
    city = city?.trim()?.takeIf { it.isNotEmpty() },
    date = date.toEpochDay(),
    note = note?.trim()?.takeIf { it.isNotEmpty() },
    seed = seed,
)

private fun TripEntity.toDomain() = Trip(
    id = id,
    title = title,
    startDate = LocalDate.ofEpochDay(startDate),
    endDate = endDate?.let { LocalDate.ofEpochDay(it) },
    notes = notes,
)

private fun Trip.toEntity() = TripEntity(
    id = id,
    title = title.trim(),
    startDate = startDate.toEpochDay(),
    endDate = endDate?.toEpochDay(),
    notes = notes?.trim()?.takeIf { it.isNotEmpty() },
)
