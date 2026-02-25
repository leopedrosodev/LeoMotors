package br.com.leo.leomotors.data.local

import br.com.leo.leomotors.data.FuelRecord
import br.com.leo.leomotors.data.OdometerRecord
import br.com.leo.leomotors.data.Vehicle
import br.com.leo.leomotors.data.VehicleType

fun VehicleEntity.toDomain(): Vehicle {
    val safeType = runCatching { VehicleType.valueOf(type) }.getOrDefault(VehicleType.CAR)
    return Vehicle(id = id, name = name, type = safeType)
}

fun Vehicle.toEntity(): VehicleEntity {
    return VehicleEntity(id = id, name = name, type = type.name)
}

fun OdometerRecordEntity.toDomain(): OdometerRecord {
    return OdometerRecord(
        id = id,
        vehicleId = vehicleId,
        dateEpochDay = dateEpochDay,
        odometerKm = odometerKm
    )
}

fun OdometerRecord.toEntity(): OdometerRecordEntity {
    return OdometerRecordEntity(
        id = id,
        vehicleId = vehicleId,
        dateEpochDay = dateEpochDay,
        odometerKm = odometerKm
    )
}

fun FuelRecordEntity.toDomain(): FuelRecord {
    return FuelRecord(
        id = id,
        vehicleId = vehicleId,
        dateEpochDay = dateEpochDay,
        odometerKm = odometerKm,
        liters = liters,
        pricePerLiter = pricePerLiter
    )
}

fun FuelRecord.toEntity(): FuelRecordEntity {
    return FuelRecordEntity(
        id = id,
        vehicleId = vehicleId,
        dateEpochDay = dateEpochDay,
        odometerKm = odometerKm,
        liters = liters,
        pricePerLiter = pricePerLiter
    )
}
