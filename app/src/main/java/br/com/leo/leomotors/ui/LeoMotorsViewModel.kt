package br.com.leo.leomotors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.leo.leomotors.data.FuelRecord
import br.com.leo.leomotors.data.LocalStore
import br.com.leo.leomotors.data.OdometerRecord
import br.com.leo.leomotors.data.Vehicle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LeoMotorsUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val fuelRecords: List<FuelRecord> = emptyList(),
    val odometerRecords: List<OdometerRecord> = emptyList(),
    val loaded: Boolean = false
)

class LeoMotorsViewModel(
    private val store: LocalStore
) : ViewModel() {
    private val _uiState = MutableStateFlow(LeoMotorsUiState())
    val uiState: StateFlow<LeoMotorsUiState> = _uiState.asStateFlow()

    init {
        refreshAll()
    }

    fun refreshAll() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = LeoMotorsUiState(
                vehicles = store.getVehicles(),
                fuelRecords = store.getFuelRecords(),
                odometerRecords = store.getOdometerRecords(),
                loaded = true
            )
        }
    }

    fun renameVehicle(vehicleId: Long, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            store.updateVehicleName(vehicleId, name)
            refreshAll()
        }
    }

    fun addOdometer(vehicleId: Long, dateEpochDay: Long, odometerKm: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            store.addOdometerRecord(vehicleId, dateEpochDay, odometerKm)
            refreshAll()
        }
    }

    fun addRefuel(
        vehicleId: Long,
        dateEpochDay: Long,
        odometerKm: Double,
        liters: Double,
        pricePerLiter: Double
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            store.addFuelRecord(vehicleId, dateEpochDay, odometerKm, liters, pricePerLiter)
            refreshAll()
        }
    }
}

class LeoMotorsViewModelFactory(
    private val store: LocalStore
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LeoMotorsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LeoMotorsViewModel(store) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
