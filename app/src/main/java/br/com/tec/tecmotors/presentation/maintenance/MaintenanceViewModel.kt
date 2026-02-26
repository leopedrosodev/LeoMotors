package br.com.tec.tecmotors.presentation.maintenance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.tec.tecmotors.domain.model.MaintenanceType
import br.com.tec.tecmotors.domain.usecase.AddMaintenanceUseCase
import br.com.tec.tecmotors.domain.usecase.MaintenanceDueStatus
import br.com.tec.tecmotors.domain.usecase.ObserveMaintenanceUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveOdometersUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveVehiclesUseCase
import br.com.tec.tecmotors.domain.usecase.SetMaintenanceDoneUseCase
import br.com.tec.tecmotors.domain.usecase.CalculateMaintenanceStatusUseCase
import br.com.tec.tecmotors.presentation.common.parseDateBrOrIso
import br.com.tec.tecmotors.presentation.common.parseDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class MaintenanceViewModel(
    private val observeVehiclesUseCase: ObserveVehiclesUseCase,
    private val observeOdometersUseCase: ObserveOdometersUseCase,
    private val observeMaintenanceUseCase: ObserveMaintenanceUseCase,
    private val addMaintenanceUseCase: AddMaintenanceUseCase,
    private val setMaintenanceDoneUseCase: SetMaintenanceDoneUseCase,
    private val calculateMaintenanceStatusUseCase: CalculateMaintenanceStatusUseCase
) : ViewModel() {
    private val localState = MutableStateFlow(MaintenanceUiState())

    val uiState: StateFlow<MaintenanceUiState> = combine(
        observeVehiclesUseCase(),
        observeOdometersUseCase(),
        observeMaintenanceUseCase(),
        localState
    ) { vehicles, odometers, maintenance, state ->
        val selected = state.selectedVehicleId.takeIf { id -> vehicles.any { it.id == id } }
            ?: vehicles.firstOrNull()?.id
            ?: -1L

        state.copy(
            vehicles = vehicles,
            odometerRecords = odometers,
            maintenanceRecords = maintenance,
            selectedVehicleId = selected
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = localState.value
    )

    fun onEvent(event: MaintenanceUiEvent) {
        when (event) {
            is MaintenanceUiEvent.SelectVehicle -> {
                localState.update { it.copy(selectedVehicleId = event.vehicleId) }
            }

            is MaintenanceUiEvent.SelectType -> {
                localState.update {
                    it.copy(
                        selectedType = event.type,
                        titleText = if (it.titleText.isBlank()) event.type.label else it.titleText
                    )
                }
            }

            is MaintenanceUiEvent.ChangeTitle -> localState.update { it.copy(titleText = event.value) }
            is MaintenanceUiEvent.ChangeDueDate -> localState.update { it.copy(dueDateText = event.value) }
            is MaintenanceUiEvent.ChangeDueKm -> localState.update { it.copy(dueKmText = event.value) }
            is MaintenanceUiEvent.ChangeEstimatedCost -> localState.update { it.copy(estimatedCostText = event.value) }
            is MaintenanceUiEvent.ChangeNotes -> localState.update { it.copy(notesText = event.value) }

            MaintenanceUiEvent.SaveMaintenance -> saveMaintenance()

            is MaintenanceUiEvent.ToggleDone -> {
                viewModelScope.launch {
                    setMaintenanceDoneUseCase(event.recordId, event.done)
                }
            }

            MaintenanceUiEvent.ClearFeedback -> localState.update { it.copy(feedback = null) }
        }
    }

    private fun saveMaintenance() {
        val state = uiState.value

        val dueDate = state.dueDateText.takeIf { it.isNotBlank() }?.let(::parseDateBrOrIso)
        val dueKm = state.dueKmText.takeIf { it.isNotBlank() }?.let(::parseDecimal)
        val estimatedCost = state.estimatedCostText.takeIf { it.isNotBlank() }?.let(::parseDecimal)

        if (state.selectedVehicleId <= 0L) {
            localState.update { it.copy(feedback = "Selecione um veiculo") }
            return
        }
        if (state.titleText.isBlank()) {
            localState.update { it.copy(feedback = "Informe um titulo para a manutencao") }
            return
        }
        if (state.dueDateText.isNotBlank() && dueDate == null) {
            localState.update { it.copy(feedback = "Data invalida") }
            return
        }
        if (state.dueKmText.isNotBlank() && dueKm == null) {
            localState.update { it.copy(feedback = "Odometro invalido") }
            return
        }
        if (dueDate == null && dueKm == null) {
            localState.update { it.copy(feedback = "Informe data ou odometro para vencimento") }
            return
        }

        viewModelScope.launch {
            addMaintenanceUseCase(
                vehicleId = state.selectedVehicleId,
                type = state.selectedType,
                title = state.titleText,
                notes = state.notesText,
                createdAtEpochDay = LocalDate.now().toEpochDay(),
                dueDateEpochDay = dueDate?.toEpochDay(),
                dueOdometerKm = dueKm,
                estimatedCost = estimatedCost
            )
            localState.update {
                it.copy(
                    dueDateText = "",
                    dueKmText = "",
                    estimatedCostText = "",
                    notesText = "",
                    feedback = "Manutencao cadastrada"
                )
            }
        }
    }

    fun latestOdometer(vehicleId: Long): Double? {
        return uiState.value.odometerRecords
            .filter { it.vehicleId == vehicleId }
            .maxByOrNull { it.dateEpochDay }
            ?.odometerKm
    }

    fun maintenanceTypeLabel(type: MaintenanceType): String = type.label

    fun statusOf(record: br.com.tec.tecmotors.domain.model.MaintenanceRecord): MaintenanceDueStatus {
        val odometer = latestOdometer(record.vehicleId)
        return calculateMaintenanceStatusUseCase(record, odometer, LocalDate.now())
    }
}
