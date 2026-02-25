package br.com.leo.leomotors

import br.com.leo.leomotors.domain.model.MaintenanceRecord
import br.com.leo.leomotors.domain.model.MaintenanceType
import br.com.leo.leomotors.domain.usecase.CalculateMaintenanceStatusUseCase
import br.com.leo.leomotors.domain.usecase.MaintenanceDueStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class MaintenanceStatusUseCaseTest {
    private val useCase = CalculateMaintenanceStatusUseCase()
    private val today = LocalDate.of(2026, 2, 25)

    @Test
    fun doneRecord_isDone() {
        val status = useCase(baseRecord(done = true), currentOdometerKm = 1000.0, today = today)
        assertEquals(MaintenanceDueStatus.DONE, status)
    }

    @Test
    fun overdueByDate_isOverdue() {
        val status = useCase(
            baseRecord(dueDateEpochDay = today.minusDays(1).toEpochDay()),
            currentOdometerKm = 500.0,
            today = today
        )
        assertEquals(MaintenanceDueStatus.OVERDUE, status)
    }

    @Test
    fun dueSoonByKm_isDueSoon() {
        val status = useCase(
            baseRecord(dueDateEpochDay = null, dueOdometerKm = 2000.0),
            currentOdometerKm = 1600.0,
            today = today
        )
        assertEquals(MaintenanceDueStatus.DUE_SOON, status)
    }

    @Test
    fun farFuture_isOnTrack() {
        val status = useCase(
            baseRecord(dueDateEpochDay = today.plusDays(60).toEpochDay(), dueOdometerKm = null),
            currentOdometerKm = 1000.0,
            today = today
        )
        assertEquals(MaintenanceDueStatus.ON_TRACK, status)
    }

    private fun baseRecord(
        done: Boolean = false,
        dueDateEpochDay: Long? = today.plusDays(20).toEpochDay(),
        dueOdometerKm: Double? = 2000.0
    ): MaintenanceRecord {
        return MaintenanceRecord(
            id = 1,
            vehicleId = 1,
            type = MaintenanceType.OIL_CHANGE,
            title = "Troca",
            notes = "",
            createdAtEpochDay = today.minusDays(10).toEpochDay(),
            dueDateEpochDay = dueDateEpochDay,
            dueOdometerKm = dueOdometerKm,
            estimatedCost = 120.0,
            done = done
        )
    }
}
