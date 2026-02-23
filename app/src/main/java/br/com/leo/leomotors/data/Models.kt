package br.com.leo.leomotors.data

import org.json.JSONObject

enum class VehicleType(val label: String) {
    CAR("Carro"),
    MOTORCYCLE("Moto")
}

data class Vehicle(
    val id: Long,
    val name: String,
    val type: VehicleType
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("type", type.name)

    companion object {
        fun fromJson(json: JSONObject): Vehicle {
            return Vehicle(
                id = json.optLong("id"),
                name = json.optString("name"),
                type = VehicleType.valueOf(json.optString("type", VehicleType.CAR.name))
            )
        }
    }
}

data class OdometerRecord(
    val id: Long,
    val vehicleId: Long,
    val dateEpochDay: Long,
    val odometerKm: Double
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("vehicleId", vehicleId)
        .put("dateEpochDay", dateEpochDay)
        .put("odometerKm", odometerKm)

    companion object {
        fun fromJson(json: JSONObject): OdometerRecord {
            return OdometerRecord(
                id = json.optLong("id"),
                vehicleId = json.optLong("vehicleId"),
                dateEpochDay = json.optLong("dateEpochDay"),
                odometerKm = json.optDouble("odometerKm")
            )
        }
    }
}

data class FuelRecord(
    val id: Long,
    val vehicleId: Long,
    val dateEpochDay: Long,
    val odometerKm: Double,
    val liters: Double,
    val pricePerLiter: Double
) {
    val totalCost: Double = liters * pricePerLiter

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("vehicleId", vehicleId)
        .put("dateEpochDay", dateEpochDay)
        .put("odometerKm", odometerKm)
        .put("liters", liters)
        .put("pricePerLiter", pricePerLiter)

    companion object {
        fun fromJson(json: JSONObject): FuelRecord {
            return FuelRecord(
                id = json.optLong("id"),
                vehicleId = json.optLong("vehicleId"),
                dateEpochDay = json.optLong("dateEpochDay"),
                odometerKm = json.optDouble("odometerKm"),
                liters = json.optDouble("liters"),
                pricePerLiter = json.optDouble("pricePerLiter")
            )
        }
    }
}

data class PeriodReport(
    val distanceKm: Double,
    val liters: Double,
    val averageKmPerLiter: Double,
    val totalCost: Double,
    val refuelCount: Int,
    val averageMonthlyCost: Double
)
