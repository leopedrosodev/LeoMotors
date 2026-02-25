package br.com.leo.leomotors.presentation.refuels

import br.com.leo.leomotors.domain.model.FuelRecord
import br.com.leo.leomotors.domain.model.Vehicle

sealed interface RefuelsUiEvent {
    data class SelectVehicle(val vehicleId: Long) : RefuelsUiEvent
    data class ChangeDate(val value: String) : RefuelsUiEvent
    data class ChangeOdometer(val value: String) : RefuelsUiEvent
    data class ChangeLiters(val value: String) : RefuelsUiEvent
    data class ChangePrice(val value: String) : RefuelsUiEvent
    data object SaveRefuel : RefuelsUiEvent
    data object ClearFeedback : RefuelsUiEvent
}

data class RefuelsUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val fuelRecords: List<FuelRecord> = emptyList(),
    val selectedVehicleId: Long = -1L,
    val dateText: String = "",
    val odometerText: String = "",
    val litersText: String = "",
    val priceText: String = "",
    val feedback: String? = null
)
