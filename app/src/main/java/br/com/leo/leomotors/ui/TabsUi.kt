package br.com.leo.leomotors

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.leo.leomotors.data.FuelRecord
import br.com.leo.leomotors.data.OdometerRecord
import br.com.leo.leomotors.data.PeriodReport
import br.com.leo.leomotors.data.ReportCalculator
import br.com.leo.leomotors.data.Vehicle
import br.com.leo.leomotors.data.VehicleType
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class MissingField(val label: String) {
    DISTANCE("Distancia"),
    CONSUMPTION("Consumo km/l"),
    LITERS("Litros")
}

private val DATE_BR_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.forLanguageTag("pt-BR"))

@Composable
internal fun VehiclesTab(
    vehicles: List<Vehicle>,
    odometerRecords: List<OdometerRecord>,
    onRenameVehicle: (Long, String) -> Unit,
    onAddOdometer: (Long, Long, Double) -> Unit
) {
    var selectedVehicleId by remember(vehicles) {
        mutableLongStateOf(vehicles.firstOrNull()?.id ?: -1L)
    }
    var dateText by remember { mutableStateOf(todayText()) }
    var odometerText by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Cadastro de veiculos", style = MaterialTheme.typography.titleLarge)
        Text(
            "Os dois veiculos ja ficam cadastrados localmente no seu celular.",
            style = MaterialTheme.typography.bodyMedium
        )

        vehicles.forEach { vehicle ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                var nameText by remember(vehicle.id, vehicle.name) { mutableStateOf(vehicle.name) }
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${vehicle.type.label}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        label = { Text("Nome do veiculo") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            if (nameText.isNotBlank()) {
                                onRenameVehicle(vehicle.id, nameText)
                                feedback = "Nome atualizado"
                            }
                        }) {
                            Text("Salvar nome")
                        }

                        Text(
                            text = lastOdometerText(vehicle.id, odometerRecords),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))

        Text("Registrar odometro", style = MaterialTheme.typography.titleMedium)
        VehicleSelector(
            vehicles = vehicles,
            selectedVehicleId = selectedVehicleId,
            onSelect = { selectedVehicleId = it }
        )
        OutlinedTextField(
            value = dateText,
            onValueChange = { dateText = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Data (DD/MM/AAAA)") },
            singleLine = true
        )
        OutlinedTextField(
            value = odometerText,
            onValueChange = { odometerText = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Odometro (km)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )
        Button(
            onClick = {
                val date = parseDate(dateText)
                val odometer = parseDecimal(odometerText)
                if (selectedVehicleId <= 0L || date == null || odometer == null) {
                    feedback = "Preencha data e odometro corretamente"
                    return@Button
                }
                onAddOdometer(selectedVehicleId, date.toEpochDay(), odometer)
                odometerText = ""
                feedback = "Odometro registrado"
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Salvar odometro")
        }

        feedback?.let {
            Text(it, color = MaterialTheme.colorScheme.primary)
        }

        Text(
            text = "Dica: registre no inicio e no fim do mes para melhorar a precisao dos relatorios.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
internal fun RefuelsTab(
    vehicles: List<Vehicle>,
    fuelRecords: List<FuelRecord>,
    onAddRefuel: (Long, Long, Double, Double, Double) -> Unit
) {
    var selectedVehicleId by remember(vehicles) {
        mutableLongStateOf(-1L)
    }
    var dateText by remember { mutableStateOf(todayText()) }
    var odometerText by remember { mutableStateOf("") }
    var litersText by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf<String?>(null) }
    val selectedVehicle = vehicles.firstOrNull { it.id == selectedVehicleId }
    val canEditRefuelForm = selectedVehicle != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Lancamento de abastecimento", style = MaterialTheme.typography.titleLarge)
        Text(
            "Escolha primeiro o veiculo para lancar o abastecimento.",
            style = MaterialTheme.typography.bodyMedium
        )

        RefuelVehicleCardSelector(
            vehicles = vehicles,
            selectedVehicleId = selectedVehicleId,
            onSelect = { selectedVehicleId = it }
        )

        if (selectedVehicle == null) {
            Text(
                "Selecione Carro ou Moto para habilitar o formulario.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            Text(
                "Selecionado: ${selectedVehicle.name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        OutlinedTextField(
            value = dateText,
            onValueChange = { dateText = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Data (DD/MM/AAAA)") },
            enabled = canEditRefuelForm,
            singleLine = true
        )
        OutlinedTextField(
            value = odometerText,
            onValueChange = { odometerText = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Odometro (km)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            enabled = canEditRefuelForm,
            singleLine = true
        )
        OutlinedTextField(
            value = litersText,
            onValueChange = { litersText = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Litros abastecidos") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            enabled = canEditRefuelForm,
            singleLine = true
        )
        OutlinedTextField(
            value = priceText,
            onValueChange = { priceText = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Preco por litro (R$)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            enabled = canEditRefuelForm,
            singleLine = true
        )

        Button(
            onClick = {
                val date = parseDate(dateText)
                val odometer = parseDecimal(odometerText)
                val liters = parseDecimal(litersText)
                val price = parseDecimal(priceText)

                if (selectedVehicleId <= 0L || date == null || odometer == null || liters == null || price == null) {
                    feedback = "Preencha todos os campos com valores validos"
                    return@Button
                }

                onAddRefuel(
                    selectedVehicleId,
                    date.toEpochDay(),
                    odometer,
                    liters,
                    price
                )

                odometerText = ""
                litersText = ""
                priceText = ""
                feedback = "Abastecimento salvo"
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = canEditRefuelForm
        ) {
            Text("Salvar abastecimento")
        }

        feedback?.let {
            Text(it, color = MaterialTheme.colorScheme.primary)
        }

        HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
        Text("Ultimos abastecimentos", style = MaterialTheme.typography.titleMedium)

        if (fuelRecords.isEmpty()) {
            Text("Nenhum abastecimento registrado ainda")
        } else {
            fuelRecords.take(10).forEach { record ->
                val vehicleName = vehicles.firstOrNull { it.id == record.vehicleId }?.name ?: "Veiculo"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("$vehicleName - ${formatDate(record.dateEpochDay)}")
                        Text("Odometro: ${formatNumber(record.odometerKm)} km")
                        Text(
                            "Litros: ${formatNumber(record.liters)} | Preco/l: R$ ${formatNumber(record.pricePerLiter)}"
                        )
                        Text("Total: ${formatCurrency(record.totalCost)}")
                    }
                }
            }
        }
    }
}

@Composable
private fun RefuelVehicleCardSelector(
    vehicles: List<Vehicle>,
    selectedVehicleId: Long,
    onSelect: (Long) -> Unit
) {
    val carVehicle = vehicles.firstOrNull { it.type == VehicleType.CAR }
    val motorcycleVehicle = vehicles.firstOrNull { it.type == VehicleType.MOTORCYCLE }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RefuelVehicleCard(
            modifier = Modifier.weight(1f),
            title = "Carro",
            vehicle = carVehicle,
            selected = carVehicle?.id == selectedVehicleId,
            iconType = VehicleType.CAR,
            onClick = { carVehicle?.let { onSelect(it.id) } }
        )

        RefuelVehicleCard(
            modifier = Modifier.weight(1f),
            title = "Moto",
            vehicle = motorcycleVehicle,
            selected = motorcycleVehicle?.id == selectedVehicleId,
            iconType = VehicleType.MOTORCYCLE,
            onClick = { motorcycleVehicle?.let { onSelect(it.id) } }
        )
    }
}

@Composable
private fun RefuelVehicleCard(
    modifier: Modifier,
    title: String,
    vehicle: Vehicle?,
    selected: Boolean,
    iconType: VehicleType,
    onClick: () -> Unit
) {
    val enabled = vehicle != null
    val containerColor = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        selected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
        selected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (iconType == VehicleType.CAR) {
                    Icons.Filled.DirectionsCar
                } else {
                    Icons.Filled.TwoWheeler
                },
                contentDescription = title
            )

            Text(title, fontWeight = FontWeight.Bold)
            Text(vehicle?.name ?: "Nao cadastrado", style = MaterialTheme.typography.bodyMedium)
            Text(
                if (selected) "Selecionado" else "Toque para selecionar",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
internal fun ReportsTab(
    vehicles: List<Vehicle>,
    fuelRecords: List<FuelRecord>,
    odometerRecords: List<OdometerRecord>
) {
    var selectedVehicleId by remember(vehicles) {
        mutableLongStateOf(vehicles.firstOrNull()?.id ?: -1L)
    }

    val today = remember { LocalDate.now() }
    val weekStart = remember(today) {
        today.with(DayOfWeek.MONDAY)
    }
    val monthStart = remember(today) {
        YearMonth.of(today.year, today.month).atDay(1)
    }

    val weeklyReport = remember(selectedVehicleId, fuelRecords, odometerRecords, weekStart, today) {
        if (selectedVehicleId <= 0L) {
            emptyReport()
        } else {
            ReportCalculator.calculatePeriodReport(
                vehicleId = selectedVehicleId,
                start = weekStart,
                end = today,
                fuelRecords = fuelRecords,
                odometerRecords = odometerRecords
            )
        }
    }

    val monthlyReport = remember(selectedVehicleId, fuelRecords, odometerRecords, monthStart, today) {
        if (selectedVehicleId <= 0L) {
            emptyReport()
        } else {
            ReportCalculator.calculatePeriodReport(
                vehicleId = selectedVehicleId,
                start = monthStart,
                end = today,
                fuelRecords = fuelRecords,
                odometerRecords = odometerRecords
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Relatorios", style = MaterialTheme.typography.titleLarge)
        VehicleSelector(
            vehicles = vehicles,
            selectedVehicleId = selectedVehicleId,
            onSelect = { selectedVehicleId = it }
        )

        PeriodReportCard(
            title = "Semanal",
            period = "${weekStart.format(DATE_BR_FORMATTER)} ate ${today.format(DATE_BR_FORMATTER)}",
            report = weeklyReport
        )

        PeriodReportCard(
            title = "Mensal",
            period = "${monthStart.format(DATE_BR_FORMATTER)} ate ${today.format(DATE_BR_FORMATTER)}",
            report = monthlyReport
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Indicadores principais", fontWeight = FontWeight.Bold)
                Text("Media de gasto mensal (historico): ${formatCurrency(monthlyReport.averageMonthlyCost)}")
                Text("Quantidade de abastecimentos no mes: ${monthlyReport.refuelCount}")
                Text("Consumo medio semanal: ${formatNumber(weeklyReport.averageKmPerLiter)} km/l")
                Text("Consumo medio mensal: ${formatNumber(monthlyReport.averageKmPerLiter)} km/l")
            }
        }
    }
}

@Composable
internal fun FuelCalculatorTab() {
    var missingField by remember { mutableStateOf(MissingField.LITERS) }
    var distanceText by remember { mutableStateOf("") }
    var consumptionText by remember { mutableStateOf("") }
    var litersText by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }

    val distance = parseDecimal(distanceText)
    val consumption = parseDecimal(consumptionText)
    val liters = parseDecimal(litersText)
    val price = parseDecimal(priceText)

    val solvedDistance = when (missingField) {
        MissingField.DISTANCE -> if (consumption != null && liters != null) consumption * liters else null
        MissingField.CONSUMPTION -> distance
        MissingField.LITERS -> distance
    }

    val solvedConsumption = when (missingField) {
        MissingField.DISTANCE -> consumption
        MissingField.CONSUMPTION -> if (distance != null && liters != null && liters > 0.0) distance / liters else null
        MissingField.LITERS -> consumption
    }

    val solvedLiters = when (missingField) {
        MissingField.DISTANCE -> liters
        MissingField.CONSUMPTION -> liters
        MissingField.LITERS -> if (distance != null && consumption != null && consumption > 0.0) distance / consumption else null
    }

    val totalCost = if (solvedLiters != null && price != null) solvedLiters * price else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Calculadora de combustivel", style = MaterialTheme.typography.titleLarge)
        Text(
            "Escolha o campo que sera calculado automaticamente, como no modelo do print.",
            style = MaterialTheme.typography.bodyMedium
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MissingField.entries.forEach { option ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        RadioButton(
                            selected = missingField == option,
                            onClick = { missingField = option }
                        )
                        Text(option.label, modifier = Modifier.padding(top = 12.dp))
                    }
                }
            }
        }

        OutlinedTextField(
            value = distanceText,
            onValueChange = { distanceText = it },
            label = { Text("Distancia (km)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            enabled = missingField != MissingField.DISTANCE,
            singleLine = true
        )
        OutlinedTextField(
            value = consumptionText,
            onValueChange = { consumptionText = it },
            label = { Text("Consumo (km/l)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            enabled = missingField != MissingField.CONSUMPTION,
            singleLine = true
        )
        OutlinedTextField(
            value = litersText,
            onValueChange = { litersText = it },
            label = { Text("Quantidade de combustivel (litros)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            enabled = missingField != MissingField.LITERS,
            singleLine = true
        )
        OutlinedTextField(
            value = priceText,
            onValueChange = { priceText = it },
            label = { Text("Preco do combustivel (R$/l)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Resultado", fontWeight = FontWeight.Bold)
                Text("Distancia: ${formatMaybeNumber(solvedDistance)} km")
                Text("Consumo: ${formatMaybeNumber(solvedConsumption)} km/l")
                Text("Litros: ${formatMaybeNumber(solvedLiters)} l")
                Text("Custo do combustivel: ${formatMaybeCurrency(totalCost)}")
            }
        }

        TextButton(
            onClick = {
                distanceText = ""
                consumptionText = ""
                litersText = ""
                priceText = ""
                missingField = MissingField.LITERS
            }
        ) {
            Text("Limpar calculadora")
        }
    }
}

@Composable
private fun VehicleSelector(
    vehicles: List<Vehicle>,
    selectedVehicleId: Long,
    onSelect: (Long) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        vehicles.forEach { vehicle ->
            FilterChip(
                selected = selectedVehicleId == vehicle.id,
                onClick = { onSelect(vehicle.id) },
                label = { Text(vehicle.name) }
            )
        }
    }
}

@Composable
private fun PeriodReportCard(title: String, period: String, report: PeriodReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(period, style = MaterialTheme.typography.bodySmall)
            HorizontalDivider(modifier = Modifier.padding(top = 2.dp, bottom = 2.dp))
            Text("Distancia percorrida: ${formatNumber(report.distanceKm)} km")
            Text("Consumo de combustivel: ${formatNumber(report.averageKmPerLiter)} km/l")
            Text("Gasto de combustivel: ${formatCurrency(report.totalCost)}")
            Text("Quantidade de abastecimentos: ${report.refuelCount}")
            Text("Litros abastecidos: ${formatNumber(report.liters)} l")
        }
    }
}

private fun todayText(): String = LocalDate.now().format(DATE_BR_FORMATTER)

private fun parseDate(input: String): LocalDate? {
    val value = input.trim()
    return runCatching { LocalDate.parse(value, DATE_BR_FORMATTER) }.getOrNull()
        ?: runCatching { LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()
}

private fun parseDecimal(input: String): Double? {
    val normalized = input.trim().replace(',', '.')
    return normalized.toDoubleOrNull()
}

private fun formatDate(epochDay: Long): String {
    return runCatching {
        LocalDate.ofEpochDay(epochDay).format(DATE_BR_FORMATTER)
    }.getOrDefault("-")
}

private fun formatNumber(value: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR"))
    formatter.maximumFractionDigits = 2
    formatter.minimumFractionDigits = 2
    return formatter.format(value)
}

private fun formatMaybeNumber(value: Double?): String = value?.let { formatNumber(it) } ?: "-"

private fun formatCurrency(value: Double): String {
    return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(value)
}

private fun formatMaybeCurrency(value: Double?): String = value?.let { formatCurrency(it) } ?: "-"

private fun emptyReport(): PeriodReport {
    return PeriodReport(
        distanceKm = 0.0,
        liters = 0.0,
        averageKmPerLiter = 0.0,
        totalCost = 0.0,
        refuelCount = 0,
        averageMonthlyCost = 0.0
    )
}

private fun lastOdometerText(vehicleId: Long, records: List<OdometerRecord>): String {
    val latest = records
        .filter { it.vehicleId == vehicleId }
        .maxByOrNull { it.dateEpochDay }

    return if (latest == null) {
        "Sem odometro registrado"
    } else {
        "Ultimo odometro: ${formatNumber(latest.odometerKm)} km"
    }
}
