package br.com.leo.leomotors.domain.usecase

import br.com.leo.leomotors.domain.model.MaintenanceRecord
import br.com.leo.leomotors.domain.model.MaintenanceType
import br.com.leo.leomotors.domain.repository.MaintenanceRepository
import java.time.LocalDate

enum class MaintenanceDueStatus {
    DONE,
    OVERDUE,
    DUE_SOON,
    ON_TRACK
}

class ObserveMaintenanceUseCase(private val repository: MaintenanceRepository) {
    operator fun invoke() = repository.observeMaintenance()
}

class ObserveMaintenanceByVehicleUseCase(private val repository: MaintenanceRepository) {
    operator fun invoke(vehicleId: Long) = repository.observeMaintenance(vehicleId)
}

class AddMaintenanceUseCase(private val repository: MaintenanceRepository) {
    suspend operator fun invoke(
        vehicleId: Long,
        type: MaintenanceType,
        title: String,
        notes: String,
        createdAtEpochDay: Long,
        dueDateEpochDay: Long?,
        dueOdometerKm: Double?,
        estimatedCost: Double?
    ) {
        repository.addMaintenance(
            vehicleId = vehicleId,
            type = type,
            title = title,
            notes = notes,
            createdAtEpochDay = createdAtEpochDay,
            dueDateEpochDay = dueDateEpochDay,
            dueOdometerKm = dueOdometerKm,
            estimatedCost = estimatedCost
        )
    }
}

class SetMaintenanceDoneUseCase(private val repository: MaintenanceRepository) {
    suspend operator fun invoke(recordId: Long, done: Boolean) {
        repository.setDone(recordId, done)
    }
}

class CalculateMaintenanceStatusUseCase {
    operator fun invoke(
        record: MaintenanceRecord,
        currentOdometerKm: Double?,
        today: LocalDate
    ): MaintenanceDueStatus {
        if (record.done) return MaintenanceDueStatus.DONE

        val todayEpoch = today.toEpochDay()
        val dateOverdue = record.dueDateEpochDay?.let { it < todayEpoch } == true
        val kmOverdue = record.dueOdometerKm?.let { due -> currentOdometerKm?.let { it >= due } } == true
        if (dateOverdue || kmOverdue) return MaintenanceDueStatus.OVERDUE

        val dueSoonDate = record.dueDateEpochDay?.let { due ->
            val daysToDue = due - todayEpoch
            daysToDue in 0..15
        } == true

        val dueSoonKm = record.dueOdometerKm?.let { due ->
            val current = currentOdometerKm ?: return@let false
            val remaining = due - current
            remaining in 0.0..500.0
        } == true

        return if (dueSoonDate || dueSoonKm) {
            MaintenanceDueStatus.DUE_SOON
        } else {
            MaintenanceDueStatus.ON_TRACK
        }
    }
}
