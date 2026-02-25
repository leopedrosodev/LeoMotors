package br.com.leo.leomotors.data.local.migration

import androidx.room.withTransaction
import br.com.leo.leomotors.data.local.LeoMotorsDatabase
import br.com.leo.leomotors.data.local.entity.SettingsEntity
import br.com.leo.leomotors.data.local.mapper.toEntity
import br.com.leo.leomotors.domain.model.Vehicle
import br.com.leo.leomotors.domain.model.VehicleType

class LegacyImportManager(
    private val database: LeoMotorsDatabase,
    private val reader: LegacySnapshotSource
) {
    suspend fun importIfNeeded() {
        val settingsDao = database.settingsDao()
        val current = settingsDao.get()
        if (current?.legacyImportDone == true) return

        val snapshot = reader.readSnapshot()
        val currentVehiclesCount = database.vehicleDao().count()

        database.withTransaction {
            if (currentVehiclesCount == 0) {
                val vehicles = snapshot.vehicles.ifEmpty { defaultVehicles() }
                database.vehicleDao().upsertAll(vehicles.map { it.toEntity() })
                database.odometerDao().upsertAll(snapshot.odometerRecords.map { it.toEntity() })
                database.fuelDao().upsertAll(snapshot.fuelRecords.map { it.toEntity() })
                database.maintenanceDao().upsertAll(snapshot.maintenanceRecords.map { it.toEntity() })
            }

            val baseSettings = settingsDao.get() ?: SettingsEntity(
                id = 1,
                darkThemeEnabled = reader.readDarkThemeEnabled(),
                legacyImportDone = false,
                dataUpdatedAtMillis = snapshot.updatedAtMillis.takeIf { it > 0L } ?: System.currentTimeMillis()
            )

            settingsDao.upsert(
                baseSettings.copy(
                    darkThemeEnabled = baseSettings.darkThemeEnabled,
                    legacyImportDone = true,
                    dataUpdatedAtMillis = maxOf(baseSettings.dataUpdatedAtMillis, snapshot.updatedAtMillis)
                )
            )
        }
    }

    private fun defaultVehicles(): List<Vehicle> {
        return listOf(
            Vehicle(id = 1L, name = "Meu Carro", type = VehicleType.CAR),
            Vehicle(id = 2L, name = "Minha Moto", type = VehicleType.MOTORCYCLE)
        )
    }
}
