package br.com.leo.leomotors.data.local

import androidx.room.withTransaction
import br.com.leo.leomotors.data.local.entity.SettingsEntity
import br.com.leo.leomotors.data.local.mapper.toDomain
import br.com.leo.leomotors.data.local.mapper.toEntity
import br.com.leo.leomotors.domain.model.LocalStateSnapshot

class RoomSnapshotDataSource(
    private val database: LeoMotorsDatabase
) {
    suspend fun getSnapshot(): LocalStateSnapshot {
        val vehicles = database.vehicleDao().getAll().map { it.toDomain() }
        val odometers = database.odometerDao().getAll().map { it.toDomain() }
        val fuels = database.fuelDao().getAll().map { it.toDomain() }
        val maintenance = database.maintenanceDao().getAll().map { it.toDomain() }
        val settings = database.settingsDao().get()

        return LocalStateSnapshot(
            vehicles = vehicles,
            odometerRecords = odometers,
            fuelRecords = fuels,
            maintenanceRecords = maintenance,
            updatedAtMillis = settings?.dataUpdatedAtMillis ?: System.currentTimeMillis()
        )
    }

    suspend fun restoreSnapshot(snapshot: LocalStateSnapshot) {
        database.withTransaction {
            database.vehicleDao().upsertAll(snapshot.vehicles.map { it.toEntity() })
            database.odometerDao().upsertAll(snapshot.odometerRecords.map { it.toEntity() })
            database.fuelDao().upsertAll(snapshot.fuelRecords.map { it.toEntity() })
            database.maintenanceDao().upsertAll(snapshot.maintenanceRecords.map { it.toEntity() })

            val currentSettings = database.settingsDao().get() ?: SettingsEntity(
                id = 1,
                darkThemeEnabled = true,
                legacyImportDone = true,
                dataUpdatedAtMillis = snapshot.updatedAtMillis
            )

            database.settingsDao().upsert(
                currentSettings.copy(
                    dataUpdatedAtMillis = snapshot.updatedAtMillis.takeIf { it > 0L }
                        ?: System.currentTimeMillis(),
                    legacyImportDone = true
                )
            )
        }
    }
}
