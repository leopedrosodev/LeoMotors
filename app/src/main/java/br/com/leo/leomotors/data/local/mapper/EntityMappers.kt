package br.com.leo.leomotors.data.local.mapper

import br.com.leo.leomotors.data.local.entity.FuelRecordEntity
import br.com.leo.leomotors.data.local.entity.MaintenanceRecordEntity
import br.com.leo.leomotors.data.local.entity.OdometerRecordEntity
import br.com.leo.leomotors.data.local.entity.SettingsEntity
import br.com.leo.leomotors.data.local.entity.VehicleEntity
import br.com.leo.leomotors.domain.model.FuelRecord
import br.com.leo.leomotors.domain.model.MaintenanceRecord
import br.com.leo.leomotors.domain.model.MaintenanceType
import br.com.leo.leomotors.domain.model.OdometerRecord
import br.com.leo.leomotors.domain.model.Settings
import br.com.leo.leomotors.domain.model.Vehicle
import br.com.leo.leomotors.domain.model.VehicleType

fun VehicleEntity.toDomain(): Vehicle = Vehicle(
    id = id,
    name = name,
    type = runCatching { VehicleType.valueOf(type) }.getOrDefault(VehicleType.CAR)
)

fun Vehicle.toEntity(): VehicleEntity = VehicleEntity(
    id = id,
    name = name,
    type = type.name
)

fun OdometerRecordEntity.toDomain(): OdometerRecord = OdometerRecord(
    id = id,
    vehicleId = vehicleId,
    dateEpochDay = dateEpochDay,
    odometerKm = odometerKm
)

fun OdometerRecord.toEntity(): OdometerRecordEntity = OdometerRecordEntity(
    id = id,
    vehicleId = vehicleId,
    dateEpochDay = dateEpochDay,
    odometerKm = odometerKm
)

fun FuelRecordEntity.toDomain(): FuelRecord = FuelRecord(
    id = id,
    vehicleId = vehicleId,
    dateEpochDay = dateEpochDay,
    odometerKm = odometerKm,
    liters = liters,
    pricePerLiter = pricePerLiter
)

fun FuelRecord.toEntity(): FuelRecordEntity = FuelRecordEntity(
    id = id,
    vehicleId = vehicleId,
    dateEpochDay = dateEpochDay,
    odometerKm = odometerKm,
    liters = liters,
    pricePerLiter = pricePerLiter
)

fun MaintenanceRecordEntity.toDomain(): MaintenanceRecord = MaintenanceRecord(
    id = id,
    vehicleId = vehicleId,
    type = runCatching { MaintenanceType.valueOf(type) }.getOrDefault(MaintenanceType.OTHER),
    title = title,
    notes = notes,
    createdAtEpochDay = createdAtEpochDay,
    dueDateEpochDay = dueDateEpochDay,
    dueOdometerKm = dueOdometerKm,
    estimatedCost = estimatedCost,
    done = done
)

fun MaintenanceRecord.toEntity(): MaintenanceRecordEntity = MaintenanceRecordEntity(
    id = id,
    vehicleId = vehicleId,
    type = type.name,
    title = title,
    notes = notes,
    createdAtEpochDay = createdAtEpochDay,
    dueDateEpochDay = dueDateEpochDay,
    dueOdometerKm = dueOdometerKm,
    estimatedCost = estimatedCost,
    done = done
)

fun SettingsEntity.toDomain(): Settings = Settings(
    darkThemeEnabled = darkThemeEnabled,
    legacyImportDone = legacyImportDone,
    dataUpdatedAtMillis = dataUpdatedAtMillis
)

fun Settings.toEntity(): SettingsEntity = SettingsEntity(
    id = 1,
    darkThemeEnabled = darkThemeEnabled,
    legacyImportDone = legacyImportDone,
    dataUpdatedAtMillis = dataUpdatedAtMillis
)
