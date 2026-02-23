package br.com.leo.leomotors.data

import java.time.LocalDate
import java.time.YearMonth

object ReportCalculator {
    fun calculatePeriodReport(
        vehicleId: Long,
        start: LocalDate,
        end: LocalDate,
        fuelRecords: List<FuelRecord>,
        odometerRecords: List<OdometerRecord>
    ): PeriodReport {
        val startEpoch = start.toEpochDay()
        val endEpoch = end.toEpochDay()

        val periodFuels = fuelRecords.filter {
            it.vehicleId == vehicleId && it.dateEpochDay in startEpoch..endEpoch
        }
        val periodOdometers = odometerRecords.filter {
            it.vehicleId == vehicleId && it.dateEpochDay in startEpoch..endEpoch
        }

        val points = mutableListOf<Double>()
        periodFuels.forEach { points.add(it.odometerKm) }
        periodOdometers.forEach { points.add(it.odometerKm) }

        val distance = if (points.size >= 2) {
            points.maxOrNull().orEmptyDouble() - points.minOrNull().orEmptyDouble()
        } else {
            0.0
        }

        val liters = periodFuels.sumOf { it.liters }
        val totalCost = periodFuels.sumOf { it.totalCost }
        val averageKmPerLiter = if (liters > 0.0) distance / liters else 0.0
        val averageMonthlyCost = calculateAverageMonthlyCost(vehicleId, fuelRecords)

        return PeriodReport(
            distanceKm = distance,
            liters = liters,
            averageKmPerLiter = averageKmPerLiter,
            totalCost = totalCost,
            refuelCount = periodFuels.size,
            averageMonthlyCost = averageMonthlyCost
        )
    }

    private fun calculateAverageMonthlyCost(vehicleId: Long, fuelRecords: List<FuelRecord>): Double {
        val monthTotals = fuelRecords
            .asSequence()
            .filter { it.vehicleId == vehicleId }
            .groupBy {
                val date = LocalDate.ofEpochDay(it.dateEpochDay)
                YearMonth.of(date.year, date.month)
            }
            .mapValues { (_, records) -> records.sumOf { it.totalCost } }
            .values

        return if (monthTotals.isNotEmpty()) {
            monthTotals.average()
        } else {
            0.0
        }
    }
}

private fun Double?.orEmptyDouble(): Double = this ?: 0.0
