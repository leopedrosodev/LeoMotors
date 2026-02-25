package br.com.leo.leomotors.data

import android.content.Context
import androidx.room.withTransaction
import br.com.leo.leomotors.data.local.LeoMotorsDatabase
import br.com.leo.leomotors.data.local.MetaEntity
import br.com.leo.leomotors.data.local.toDomain
import br.com.leo.leomotors.data.local.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import kotlin.math.max

class LocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("leo_motors_local_store", Context.MODE_PRIVATE)
    private val database = LeoMotorsDatabase.getInstance(context)
    private val dao = database.dao()

    companion object {
        private const val KEY_NEXT_ID = "next_id"
        private const val KEY_VEHICLES = "vehicles"
        private const val KEY_ODOMETER_RECORDS = "odometer_records"
        private const val KEY_FUEL_RECORDS = "fuel_records"
        private const val KEY_DARK_THEME = "dark_theme_enabled"
        private const val KEY_DATA_UPDATED_AT = "data_updated_at"

        private const val META_NEXT_ID = "next_id"
        private const val META_UPDATED_AT = "data_updated_at"
        private const val META_LEGACY_MIGRATED = "legacy_migrated"
    }

    init {
        runIo {
            migrateLegacyDataIfNeeded()
        }
    }

    fun getVehicles(): List<Vehicle> {
        return runIo {
            val vehicles = dao.getVehicles().map { it.toDomain() }
            if (vehicles.isNotEmpty()) return@runIo vehicles

            val defaults = listOf(
                Vehicle(id = nextIdInternal(), name = "Meu Carro", type = VehicleType.CAR),
                Vehicle(id = nextIdInternal(), name = "Minha Moto", type = VehicleType.MOTORCYCLE)
            )
            dao.insertVehicles(defaults.map { it.toEntity() })
            touchDataUpdatedAtInternal()
            defaults
        }
    }

    fun saveVehicles(vehicles: List<Vehicle>) {
        runIo {
            dao.clearVehicles()
            if (vehicles.isNotEmpty()) {
                dao.insertVehicles(vehicles.map { it.toEntity() })
            }
            ensureNextIdAtLeast(maxVehicleId(vehicles) + 1L)
            touchDataUpdatedAtInternal()
        }
    }

    fun updateVehicleName(vehicleId: Long, name: String) {
        runIo {
            dao.updateVehicleName(vehicleId, name.trim())
            touchDataUpdatedAtInternal()
        }
    }

    fun getOdometerRecords(): List<OdometerRecord> {
        return runIo {
            dao.getOdometerRecords().map { it.toDomain() }
        }
    }

    fun addOdometerRecord(vehicleId: Long, dateEpochDay: Long, odometerKm: Double) {
        runIo {
            val record = OdometerRecord(
                id = nextIdInternal(),
                vehicleId = vehicleId,
                dateEpochDay = dateEpochDay,
                odometerKm = odometerKm
            )
            dao.insertOdometerRecord(record.toEntity())
            touchDataUpdatedAtInternal()
        }
    }

    fun getFuelRecords(): List<FuelRecord> {
        return runIo {
            dao.getFuelRecords().map { it.toDomain() }
        }
    }

    fun addFuelRecord(
        vehicleId: Long,
        dateEpochDay: Long,
        odometerKm: Double,
        liters: Double,
        pricePerLiter: Double
    ) {
        runIo {
            val record = FuelRecord(
                id = nextIdInternal(),
                vehicleId = vehicleId,
                dateEpochDay = dateEpochDay,
                odometerKm = odometerKm,
                liters = liters,
                pricePerLiter = pricePerLiter
            )
            dao.insertFuelRecord(record.toEntity())
            touchDataUpdatedAtInternal()
        }
    }

    fun getLocalStateSnapshot(): LocalStateSnapshot {
        return runIo {
            LocalStateSnapshot(
                vehicles = dao.getVehicles().map { it.toDomain() },
                odometerRecords = dao.getOdometerRecords().map { it.toDomain() },
                fuelRecords = dao.getFuelRecords().map { it.toDomain() },
                updatedAtMillis = getDataUpdatedAtInternal()
            )
        }
    }

    fun restoreLocalState(snapshot: LocalStateSnapshot) {
        runIo {
            database.withTransaction {
                dao.replaceAll(
                    vehicles = snapshot.vehicles.map { it.toEntity() },
                    odometerRecords = snapshot.odometerRecords.map { it.toEntity() },
                    fuelRecords = snapshot.fuelRecords.map { it.toEntity() }
                )

                val maxId = maxOf(
                    maxVehicleId(snapshot.vehicles),
                    maxOdometerId(snapshot.odometerRecords),
                    maxFuelId(snapshot.fuelRecords)
                )

                setMetaLong(META_NEXT_ID, maxId + 1L)
                val restoredUpdatedAt = snapshot.updatedAtMillis.takeIf { it > 0L }
                    ?: System.currentTimeMillis()
                setMetaLong(META_UPDATED_AT, restoredUpdatedAt)
            }
        }
    }

    fun getDataUpdatedAt(): Long {
        return runIo {
            getDataUpdatedAtInternal()
        }
    }

    fun isDarkThemeEnabled(): Boolean {
        return prefs.getBoolean(KEY_DARK_THEME, true)
    }

    fun setDarkThemeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_THEME, enabled).apply()
    }

    private suspend fun migrateLegacyDataIfNeeded() {
        val alreadyMigrated = dao.getMetaLong(META_LEGACY_MIGRATED) == 1L
        if (alreadyMigrated) return

        val legacyVehicles = readArray(KEY_VEHICLES).toVehicleList()
        val legacyOdometerRecords = readArray(KEY_ODOMETER_RECORDS).toOdometerList()
        val legacyFuelRecords = readArray(KEY_FUEL_RECORDS).toFuelList()

        val vehiclesToPersist = if (legacyVehicles.isNotEmpty()) {
            legacyVehicles
        } else {
            listOf(
                Vehicle(id = 1L, name = "Meu Carro", type = VehicleType.CAR),
                Vehicle(id = 2L, name = "Minha Moto", type = VehicleType.MOTORCYCLE)
            )
        }

        val maxId = maxOf(
            maxVehicleId(vehiclesToPersist),
            maxOdometerId(legacyOdometerRecords),
            maxFuelId(legacyFuelRecords)
        )

        val legacyNextId = prefs.getLong(KEY_NEXT_ID, 0L)
        val computedNextId = max(maxId + 1L, if (legacyNextId > 0L) legacyNextId else 1L)

        val updatedAtFromPrefs = prefs.getLong(KEY_DATA_UPDATED_AT, 0L)
        val updatedAt = if (updatedAtFromPrefs > 0L) updatedAtFromPrefs else System.currentTimeMillis()

        database.withTransaction {
            dao.replaceAll(
                vehicles = vehiclesToPersist.map { it.toEntity() },
                odometerRecords = legacyOdometerRecords.map { it.toEntity() },
                fuelRecords = legacyFuelRecords.map { it.toEntity() }
            )
            setMetaLong(META_NEXT_ID, computedNextId)
            setMetaLong(META_UPDATED_AT, updatedAt)
            setMetaLong(META_LEGACY_MIGRATED, 1L)
        }
    }

    private suspend fun nextIdInternal(): Long {
        val value = dao.getMetaLong(META_NEXT_ID) ?: 1L
        setMetaLong(META_NEXT_ID, value + 1L)
        return value
    }

    private suspend fun ensureNextIdAtLeast(minimum: Long) {
        val current = dao.getMetaLong(META_NEXT_ID) ?: 1L
        if (minimum > current) {
            setMetaLong(META_NEXT_ID, minimum)
        }
    }

    private suspend fun touchDataUpdatedAtInternal() {
        setMetaLong(META_UPDATED_AT, System.currentTimeMillis())
    }

    private suspend fun getDataUpdatedAtInternal(): Long {
        val stored = dao.getMetaLong(META_UPDATED_AT)
        if (stored != null && stored > 0L) return stored

        val fallback = System.currentTimeMillis()
        setMetaLong(META_UPDATED_AT, fallback)
        return fallback
    }

    private suspend fun setMetaLong(key: String, value: Long) {
        dao.upsertMeta(MetaEntity(key = key, longValue = value))
    }

    private fun readArray(key: String): JSONArray {
        val raw = prefs.getString(key, null) ?: return JSONArray()
        return runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
    }

    private fun <T> runIo(block: suspend () -> T): T {
        return runBlocking(Dispatchers.IO) {
            block()
        }
    }
}

private fun maxVehicleId(vehicles: List<Vehicle>): Long {
    return vehicles.maxOfOrNull { it.id } ?: 0L
}

private fun maxOdometerId(records: List<OdometerRecord>): Long {
    return records.maxOfOrNull { it.id } ?: 0L
}

private fun maxFuelId(records: List<FuelRecord>): Long {
    return records.maxOfOrNull { it.id } ?: 0L
}

private fun JSONArray.toVehicleList(): List<Vehicle> {
    return List(length()) { index -> Vehicle.fromJson(getJSONObject(index)) }
}

private fun JSONArray.toOdometerList(): List<OdometerRecord> {
    return List(length()) { index -> OdometerRecord.fromJson(getJSONObject(index)) }
}

private fun JSONArray.toFuelList(): List<FuelRecord> {
    return List(length()) { index -> FuelRecord.fromJson(getJSONObject(index)) }
}
