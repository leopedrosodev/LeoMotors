package br.com.tec.tecmotors.domain.usecase

import br.com.tec.tecmotors.domain.repository.RefuelRepository

class ObserveRefuelsUseCase(private val repository: RefuelRepository) {
    operator fun invoke() = repository.observeRefuels()
}

class AddRefuelUseCase(private val repository: RefuelRepository) {
    suspend operator fun invoke(
        vehicleId: Long,
        dateEpochDay: Long,
        odometerKm: Double,
        liters: Double,
        pricePerLiter: Double
    ) {
        repository.addRefuel(vehicleId, dateEpochDay, odometerKm, liters, pricePerLiter)
    }
}
