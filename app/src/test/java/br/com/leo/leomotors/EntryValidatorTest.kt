package br.com.leo.leomotors

import br.com.leo.leomotors.data.EntryValidator
import br.com.leo.leomotors.data.FuelRecord
import br.com.leo.leomotors.data.OdometerRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class EntryValidatorTest {
    @Test
    fun `latestKnownOdometer returns max from odometer and fuel records`() {
        val odometerRecords = listOf(
            OdometerRecord(id = 1, vehicleId = 10, dateEpochDay = 1, odometerKm = 1000.0),
            OdometerRecord(id = 2, vehicleId = 10, dateEpochDay = 2, odometerKm = 1150.0)
        )
        val fuelRecords = listOf(
            FuelRecord(id = 3, vehicleId = 10, dateEpochDay = 3, odometerKm = 1200.0, liters = 10.0, pricePerLiter = 6.0)
        )

        val latest = EntryValidator.latestKnownOdometer(
            vehicleId = 10,
            odometerRecords = odometerRecords,
            fuelRecords = fuelRecords
        )

        assertEquals(1200.0, latest!!, 0.0)
    }

    @Test
    fun `validateRefuelEntry rejects odometer less than latest`() {
        val odometerRecords = listOf(
            OdometerRecord(id = 1, vehicleId = 1, dateEpochDay = 1, odometerKm = 1500.0)
        )

        val error = EntryValidator.validateRefuelEntry(
            vehicleId = 1,
            date = LocalDate.now(),
            odometerKm = 1400.0,
            liters = 12.0,
            pricePerLiter = 5.0,
            odometerRecords = odometerRecords,
            fuelRecords = emptyList()
        )

        assertEquals("Odometro menor que o ultimo registrado (1500 km).", error)
    }

    @Test
    fun `validateOdometerEntry accepts valid current value`() {
        val now = LocalDate.now()
        val error = EntryValidator.validateOdometerEntry(
            vehicleId = 1,
            date = now,
            odometerKm = 2200.0,
            odometerRecords = listOf(
                OdometerRecord(id = 1, vehicleId = 1, dateEpochDay = now.minusDays(1).toEpochDay(), odometerKm = 2100.0)
            ),
            fuelRecords = emptyList()
        )

        assertNull(error)
    }
}
