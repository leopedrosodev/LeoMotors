package br.com.leo.leomotors.data.local.entity

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
    indices = [Index("vehicleId"), Index("dateEpochDay")]
)
data class OdometerRecordEntity(
    @PrimaryKey val id: Long,
    val vehicleId: Long,
    val dateEpochDay: Long,
    val odometerKm: Double
)

@Entity(
    tableName = "fuel_records",
    indices = [Index("vehicleId"), Index("dateEpochDay")]
)
data class FuelRecordEntity(
    @PrimaryKey val id: Long,
    val vehicleId: Long,
    val dateEpochDay: Long,
    val odometerKm: Double,
    val liters: Double,
    val pricePerLiter: Double
)

@Entity(
    tableName = "maintenance_records",
    indices = [Index("vehicleId"), Index("dueDateEpochDay")]
)
data class MaintenanceRecordEntity(
    @PrimaryKey val id: Long,
    val vehicleId: Long,
    val type: String,
    val title: String,
    val notes: String,
    val createdAtEpochDay: Long,
    val dueDateEpochDay: Long?,
    val dueOdometerKm: Double?,
    val estimatedCost: Double?,
    val done: Boolean
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val darkThemeEnabled: Boolean,
    val legacyImportDone: Boolean,
    val dataUpdatedAtMillis: Long
)
