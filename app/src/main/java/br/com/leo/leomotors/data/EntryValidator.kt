package br.com.leo.leomotors.data

import java.time.LocalDate

object EntryValidator {
    fun validateOdometerEntry(
        vehicleId: Long,
        date: LocalDate?,
        odometerKm: Double?,
        odometerRecords: List<OdometerRecord>,
        fuelRecords: List<FuelRecord>
    ): String? {
        if (vehicleId <= 0L) return "Selecione um veiculo."
        if (date == null) return "Data invalida."
        if (date.isAfter(LocalDate.now())) return "A data nao pode ser no futuro."
        if (odometerKm == null || odometerKm <= 0.0) return "Odometro deve ser maior que zero."

        val latest = latestKnownOdometer(vehicleId, odometerRecords, fuelRecords)
        if (latest != null && odometerKm < latest) {
            return "Odometro menor que o ultimo registrado (${formatSimple(latest)} km)."
        }

        return null
    }

    fun validateRefuelEntry(
        vehicleId: Long,
        date: LocalDate?,
        odometerKm: Double?,
        liters: Double?,
        pricePerLiter: Double?,
        odometerRecords: List<OdometerRecord>,
        fuelRecords: List<FuelRecord>
    ): String? {
        if (vehicleId <= 0L) return "Selecione Carro ou Moto para abastecer."
        if (date == null) return "Data invalida."
        if (date.isAfter(LocalDate.now())) return "A data nao pode ser no futuro."
        if (odometerKm == null || odometerKm <= 0.0) return "Odometro deve ser maior que zero."
        if (liters == null || liters <= 0.0) return "Litros abastecidos devem ser maiores que zero."
        if (pricePerLiter == null || pricePerLiter <= 0.0) return "Preco por litro deve ser maior que zero."

        val latest = latestKnownOdometer(vehicleId, odometerRecords, fuelRecords)
        if (latest != null && odometerKm < latest) {
            return "Odometro menor que o ultimo registrado (${formatSimple(latest)} km)."
        }

        return null
    }

    fun latestKnownOdometer(
        vehicleId: Long,
        odometerRecords: List<OdometerRecord>,
        fuelRecords: List<FuelRecord>
    ): Double? {
        val odometerMax = odometerRecords
            .asSequence()
            .filter { it.vehicleId == vehicleId }
            .maxOfOrNull { it.odometerKm }

        val fuelMax = fuelRecords
            .asSequence()
            .filter { it.vehicleId == vehicleId }
            .maxOfOrNull { it.odometerKm }

        return listOfNotNull(odometerMax, fuelMax).maxOrNull()
    }
}

private fun formatSimple(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toLong().toString()
    } else {
        "%.1f".format(value)
    }
}
