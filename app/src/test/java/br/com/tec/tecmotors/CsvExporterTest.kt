package br.com.tec.tecmotors

import br.com.tec.tecmotors.data.CsvExporter
import br.com.tec.tecmotors.domain.model.FuelRecord
import br.com.tec.tecmotors.domain.model.LocalStateSnapshot
import br.com.tec.tecmotors.domain.model.MaintenanceRecord
import br.com.tec.tecmotors.domain.model.MaintenanceType
import br.com.tec.tecmotors.domain.model.OdometerRecord
import br.com.tec.tecmotors.domain.model.Vehicle
import br.com.tec.tecmotors.domain.model.VehicleType
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {
    @Test
    fun buildSnapshotCsv_containsAllSections_andEscapesText() {
        val snapshot = LocalStateSnapshot(
            vehicles = listOf(Vehicle(1, "Meu \"Carro\"", VehicleType.CAR)),
            odometerRecords = listOf(OdometerRecord(2, 1, 100L, 1234.5)),
            fuelRecords = listOf(FuelRecord(3, 1, 101L, 1300.0, 20.0, 5.0)),
            maintenanceRecords = listOf(
                MaintenanceRecord(
                    id = 4,
                    vehicleId = 1,
                    type = MaintenanceType.OTHER,
                    title = "Filtro",
                    notes = "texto;com;separador",
                    createdAtEpochDay = 102L,
                    dueDateEpochDay = null,
                    dueOdometerKm = null,
                    estimatedCost = null,
                    done = false
                )
            ),
            updatedAtMillis = 1L
        )

        val csv = CsvExporter.buildSnapshotCsv(snapshot)

        assertTrue(csv.contains("[VEICULOS]"))
        assertTrue(csv.contains("[ODOMETRO]"))
        assertTrue(csv.contains("[ABASTECIMENTOS]"))
        assertTrue(csv.contains("[MANUTENCAO]"))
        assertTrue(csv.contains("\"Meu \"\"Carro\"\"\""))
        assertTrue(csv.contains("\"texto;com;separador\""))
    }
}
