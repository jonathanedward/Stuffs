package com.stampbook.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    @ColumnInfo(name = "start_date") val startDate: Long,
    @ColumnInfo(name = "end_date") val endDate: Long?,
    val notes: String?,
)

@Entity(
    tableName = "stamps",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["trip_id"],
            // Deleting a trip keeps its stamps: the passport is the record of record.
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("trip_id"), Index("country_code"), Index("date")],
)
data class StampEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "trip_id") val tripId: Long?,
    @ColumnInfo(name = "country_code") val countryCode: String,
    val city: String?,
    val date: Long,
    val note: String?,
    val seed: Int,
)
