package br.com.leo.leomotors

import br.com.leo.leomotors.domain.model.OdometerRecord
import br.com.leo.leomotors.domain.model.Vehicle
import br.com.leo.leomotors.domain.model.VehicleType
import br.com.leo.leomotors.domain.repository.OdometerRepository
import br.com.leo.leomotors.domain.repository.VehicleRepository
import br.com.leo.leomotors.domain.usecase.AddOdometerUseCase
import br.com.leo.leomotors.domain.usecase.ObserveOdometersUseCase
import br.com.leo.leomotors.domain.usecase.ObserveVehiclesUseCase
import br.com.leo.leomotors.domain.usecase.RenameVehicleUseCase
import br.com.leo.leomotors.presentation.vehicles.VehiclesUiEvent
import br.com.leo.leomotors.presentation.vehicles.VehiclesViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VehiclesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun saveVehicleName_updatesStateAndRepository() = runTest {
        val vehicleRepository = FakeVehicleRepository()
        val odometerRepository = FakeOdometerRepository()

        val viewModel = VehiclesViewModel(
            observeVehiclesUseCase = ObserveVehiclesUseCase(vehicleRepository),
            observeOdometersUseCase = ObserveOdometersUseCase(odometerRepository),
            renameVehicleUseCase = RenameVehicleUseCase(vehicleRepository),
            addOdometerUseCase = AddOdometerUseCase(odometerRepository)
        )
        val collectJob: Job = launch { viewModel.uiState.collect { } }

        advanceUntilIdle()
        viewModel.onEvent(VehiclesUiEvent.ChangeVehicleName(1L, "Carro Novo"))
        advanceUntilIdle()
        viewModel.onEvent(VehiclesUiEvent.SaveVehicleName(1L))

        advanceUntilIdle()

        assertEquals("Carro Novo", vehicleRepository.getVehicles().first().name)
        assertEquals("Nome atualizado", viewModel.uiState.value.feedback)
        collectJob.cancel()
    }

    private class FakeVehicleRepository : VehicleRepository {
        private val vehicles = MutableStateFlow(
            listOf(Vehicle(1, "Meu Carro", VehicleType.CAR))
        )

        override fun observeVehicles(): Flow<List<Vehicle>> = vehicles.asStateFlow()

        override suspend fun getVehicles(): List<Vehicle> = vehicles.value

        override suspend fun ensureDefaultVehiclesIfEmpty() = Unit

        override suspend fun renameVehicle(vehicleId: Long, name: String) {
            vehicles.value = vehicles.value.map { if (it.id == vehicleId) it.copy(name = name) else it }
        }
    }

    private class FakeOdometerRepository : OdometerRepository {
        val records = MutableStateFlow<List<OdometerRecord>>(emptyList())

        override fun observeOdometerRecords(): Flow<List<OdometerRecord>> = records.asStateFlow()

        override suspend fun getOdometerRecords(): List<OdometerRecord> = records.value

        override suspend fun addOdometer(vehicleId: Long, dateEpochDay: Long, odometerKm: Double) {
            val id = (records.value.maxOfOrNull { it.id } ?: 0L) + 1L
            records.value = records.value + OdometerRecord(
                id = id,
                vehicleId = vehicleId,
                dateEpochDay = dateEpochDay,
                odometerKm = odometerKm
            )
        }
    }
}
