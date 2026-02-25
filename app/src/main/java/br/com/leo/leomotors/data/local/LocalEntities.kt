package br.com.leo.leomotors.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val type: String
)

@Entity(
    tableName = "odometer_records",
    indices = [Index("vehicleId"), Index(value = ["vehicleId", "dateEpochDay"])]
)
data class OdometerRecordEntity(
    @PrimaryKey val id: Long,
    val vehicleId: Long,
    val dateEpochDay: Long,
    val odometerKm: Double
)

@Entity(
    tableName = "fuel_records",
    indices = [Index("vehicleId"), Index(value = ["vehicleId", "dateEpochDay"])]
)
data class FuelRecordEntity(
    @PrimaryKey val id: Long,
    val vehicleId: Long,
    val dateEpochDay: Long,
    val odometerKm: Double,
    val liters: Double,
    val pricePerLiter: Double
)

@Entity(tableName = "meta")
data class MetaEntity(
    @PrimaryKey val key: String,
    val longValue: Long? = null
)
