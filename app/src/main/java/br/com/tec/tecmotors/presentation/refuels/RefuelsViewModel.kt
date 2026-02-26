package br.com.tec.tecmotors.presentation.refuels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.tec.tecmotors.domain.usecase.AddRefuelUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveRefuelsUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveVehiclesUseCase
import br.com.tec.tecmotors.presentation.common.parseDateBrOrIso
import br.com.tec.tecmotors.presentation.common.parseDecimal
import br.com.tec.tecmotors.presentation.common.todayBr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RefuelsViewModel(
    private val observeVehiclesUseCase: ObserveVehiclesUseCase,
    private val observeRefuelsUseCase: ObserveRefuelsUseCase,
    private val addRefuelUseCase: AddRefuelUseCase
) : ViewModel() {
    private val localState = MutableStateFlow(RefuelsUiState(dateText = todayBr()))

    val uiState: StateFlow<RefuelsUiState> = combine(
        observeVehiclesUseCase(),
        observeRefuelsUseCase(),
        localState
    ) { vehicles, refuels, state ->
        val selected = state.selectedVehicleId.takeIf { id -> vehicles.any { it.id == id } }
            ?: vehicles.firstOrNull()?.id
            ?: -1L

        state.copy(
            vehicles = vehicles,
            fuelRecords = refuels,
            selectedVehicleId = selected
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = localState.value
    )

    fun onEvent(event: RefuelsUiEvent) {
        when (event) {
            is RefuelsUiEvent.SelectVehicle -> localState.update { it.copy(selectedVehicleId = event.vehicleId) }
            is RefuelsUiEvent.ChangeDate -> localState.update { it.copy(dateText = event.value) }
            is RefuelsUiEvent.ChangeOdometer -> localState.update { it.copy(odometerText = event.value) }
            is RefuelsUiEvent.ChangeLiters -> localState.update { it.copy(litersText = event.value) }
            is RefuelsUiEvent.ChangePrice -> localState.update { it.copy(priceText = event.value) }

            RefuelsUiEvent.SaveRefuel -> {
                val state = uiState.value
                val date = parseDateBrOrIso(state.dateText)
                val odometer = parseDecimal(state.odometerText)
                val liters = parseDecimal(state.litersText)
                val price = parseDecimal(state.priceText)

                if (state.selectedVehicleId <= 0L || date == null || odometer == null || liters == null || price == null) {
                    localState.update { it.copy(feedback = "Preencha todos os campos com valores validos") }
                    return
                }

                viewModelScope.launch {
                    addRefuelUseCase(
                        vehicleId = state.selectedVehicleId,
                        dateEpochDay = date.toEpochDay(),
                        odometerKm = odometer,
                        liters = liters,
                        pricePerLiter = price
                    )
                    localState.update {
                        it.copy(
                            odometerText = "",
                            litersText = "",
                            priceText = "",
                            feedback = "Abastecimento salvo"
                        )
                    }
                }
            }

            RefuelsUiEvent.ClearFeedback -> localState.update { it.copy(feedback = null) }
        }
    }
}
