package br.com.leo.leomotors.presentation.refuels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.leo.leomotors.R
import br.com.leo.leomotors.presentation.common.VehicleCardSelector
import br.com.leo.leomotors.presentation.common.formatCurrency
import br.com.leo.leomotors.presentation.common.formatDate
import br.com.leo.leomotors.presentation.common.formatNumber

@Composable
fun RefuelsScreen(
    state: RefuelsUiState,
    onEvent: (RefuelsUiEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.title_refuels), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.desc_refuels_intro),
            style = MaterialTheme.typography.bodyMedium
        )

        VehicleCardSelector(
            vehicles = state.vehicles,
            selectedVehicleId = state.selectedVehicleId,
            onSelect = { onEvent(RefuelsUiEvent.SelectVehicle(it)) }
        )

        OutlinedTextField(
            value = state.dateText,
            onValueChange = { onEvent(RefuelsUiEvent.ChangeDate(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.label_date_br)) },
            singleLine = true
        )
        OutlinedTextField(
            value = state.odometerText,
            onValueChange = { onEvent(RefuelsUiEvent.ChangeOdometer(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.label_odometer_km)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )
        OutlinedTextField(
            value = state.litersText,
            onValueChange = { onEvent(RefuelsUiEvent.ChangeLiters(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.label_liters)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )
        OutlinedTextField(
            value = state.priceText,
            onValueChange = { onEvent(RefuelsUiEvent.ChangePrice(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.label_price_per_liter)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )

        Button(
            onClick = { onEvent(RefuelsUiEvent.SaveRefuel) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.action_save_refuel))
        }

        state.feedback?.let {
            Text(it, color = MaterialTheme.colorScheme.primary)
        }

        HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
        Text(stringResource(R.string.title_latest_refuels), style = MaterialTheme.typography.titleMedium)

        if (state.fuelRecords.isEmpty()) {
            Text(stringResource(R.string.text_no_refuels))
        } else {
            state.fuelRecords.take(10).forEach { record ->
                val vehicleName = state.vehicles.firstOrNull { it.id == record.vehicleId }?.name ?: "Veiculo"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("$vehicleName - ${formatDate(record.dateEpochDay)}")
                        Text(stringResource(R.string.text_refuel_odometer, formatNumber(record.odometerKm)))
                        Text(
                            stringResource(
                                R.string.text_refuel_liters_price,
                                formatNumber(record.liters),
                                formatNumber(record.pricePerLiter)
                            )
                        )
                        Text(stringResource(R.string.text_refuel_total, formatCurrency(record.totalCost)))
                    }
                }
            }
        }
    }
}
