package br.com.leo.leomotors.data

import android.content.Context
import org.json.JSONArray

class LocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("leo_motors_local_store", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_NEXT_ID = "next_id"
        private const val KEY_VEHICLES = "vehicles"
        private const val KEY_ODOMETER_RECORDS = "odometer_records"
        private const val KEY_FUEL_RECORDS = "fuel_records"
        private const val KEY_DARK_THEME = "dark_theme_enabled"
        private const val KEY_DATA_UPDATED_AT = "data_updated_at"
    }

    fun getVehicles(): List<Vehicle> {
        val list = readArray(KEY_VEHICLES).toVehicleList()
        if (list.isNotEmpty()) return list

        val defaults = listOf(
            Vehicle(id = nextId(), name = "Meu Carro", type = VehicleType.CAR),
            Vehicle(id = nextId(), name = "Minha Moto", type = VehicleType.MOTORCYCLE)
        )
        saveVehicles(defaults)
        return defaults
    }

    fun saveVehicles(vehicles: List<Vehicle>) {
        val array = JSONArray()
        vehicles.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_VEHICLES, array.toString()).apply()
        touchDataUpdatedAt()
    }

    fun updateVehicleName(vehicleId: Long, name: String) {
        val updated = getVehicles().map {
            if (it.id == vehicleId) it.copy(name = name.trim()) else it
        }
        saveVehicles(updated)
    }

    fun getOdometerRecords(): List<OdometerRecord> {
        return readArray(KEY_ODOMETER_RECORDS).toOdometerList().sortedByDescending { it.dateEpochDay }
    }

    fun addOdometerRecord(vehicleId: Long, dateEpochDay: Long, odometerKm: Double) {
        val current = getOdometerRecords().toMutableList()
        current.add(
            OdometerRecord(
                id = nextId(),
                vehicleId = vehicleId,
                dateEpochDay = dateEpochDay,
                odometerKm = odometerKm
            )
        )
        saveOdometerRecords(current)
    }

    fun getFuelRecords(): List<FuelRecord> {
        return readArray(KEY_FUEL_RECORDS).toFuelList().sortedByDescending { it.dateEpochDay }
    }

    fun addFuelRecord(
        vehicleId: Long,
        dateEpochDay: Long,
        odometerKm: Double,
        liters: Double,
        pricePerLiter: Double
    ) {
        val current = getFuelRecords().toMutableList()
        current.add(
            FuelRecord(
                id = nextId(),
                vehicleId = vehicleId,
                dateEpochDay = dateEpochDay,
                odometerKm = odometerKm,
                liters = liters,
                pricePerLiter = pricePerLiter
            )
        )
        saveFuelRecords(current)
    }

    fun getLocalStateSnapshot(): LocalStateSnapshot {
        return LocalStateSnapshot(
            vehicles = getVehicles(),
            odometerRecords = getOdometerRecords(),
            fuelRecords = getFuelRecords(),
            updatedAtMillis = getDataUpdatedAt()
        )
    }

    fun restoreLocalState(snapshot: LocalStateSnapshot) {
        val vehiclesArray = JSONArray()
        snapshot.vehicles.forEach { vehiclesArray.put(it.toJson()) }

        val odometerArray = JSONArray()
        snapshot.odometerRecords.forEach { odometerArray.put(it.toJson()) }

        val fuelArray = JSONArray()
        snapshot.fuelRecords.forEach { fuelArray.put(it.toJson()) }

        val maxId = listOf(
            snapshot.vehicles.maxOfOrNull { it.id } ?: 0L,
            snapshot.odometerRecords.maxOfOrNull { it.id } ?: 0L,
            snapshot.fuelRecords.maxOfOrNull { it.id } ?: 0L
        ).maxOrNull() ?: 0L

        val restoredUpdatedAt = snapshot.updatedAtMillis.takeIf { it > 0L } ?: System.currentTimeMillis()

        prefs.edit()
            .putString(KEY_VEHICLES, vehiclesArray.toString())
            .putString(KEY_ODOMETER_RECORDS, odometerArray.toString())
            .putString(KEY_FUEL_RECORDS, fuelArray.toString())
            .putLong(KEY_NEXT_ID, maxId + 1L)
            .putLong(KEY_DATA_UPDATED_AT, restoredUpdatedAt)
            .apply()
    }

    fun getDataUpdatedAt(): Long {
        val stored = prefs.getLong(KEY_DATA_UPDATED_AT, 0L)
        if (stored > 0L) return stored
        val fallback = System.currentTimeMillis()
        prefs.edit().putLong(KEY_DATA_UPDATED_AT, fallback).apply()
        return fallback
    }

    fun isDarkThemeEnabled(): Boolean {
        return prefs.getBoolean(KEY_DARK_THEME, true)
    }

    fun setDarkThemeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_THEME, enabled).apply()
    }

    private fun saveOdometerRecords(records: List<OdometerRecord>) {
        val array = JSONArray()
        records.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_ODOMETER_RECORDS, array.toString()).apply()
        touchDataUpdatedAt()
    }

    private fun saveFuelRecords(records: List<FuelRecord>) {
        val array = JSONArray()
        records.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_FUEL_RECORDS, array.toString()).apply()
        touchDataUpdatedAt()
    }

    private fun nextId(): Long {
        val value = prefs.getLong(KEY_NEXT_ID, 1L)
        prefs.edit().putLong(KEY_NEXT_ID, value + 1L).apply()
        return value
    }

    private fun readArray(key: String): JSONArray {
        val raw = prefs.getString(key, null) ?: return JSONArray()
        return runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
    }

    private fun touchDataUpdatedAt() {
        prefs.edit().putLong(KEY_DATA_UPDATED_AT, System.currentTimeMillis()).apply()
    }
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
