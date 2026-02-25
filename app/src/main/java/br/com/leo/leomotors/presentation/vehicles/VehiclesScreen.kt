package br.com.leo.leomotors.presentation.vehicles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import br.com.leo.leomotors.R
import br.com.leo.leomotors.domain.model.OdometerRecord
import br.com.leo.leomotors.presentation.common.VehicleChipSelector
import br.com.leo.leomotors.presentation.common.formatNumber

@Composable
fun VehiclesScreen(
    state: VehiclesUiState,
    onEvent: (VehiclesUiEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.title_vehicles), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.desc_vehicles_intro),
            style = MaterialTheme.typography.bodyMedium
        )

        state.vehicles.forEach { vehicle ->
            val draftName = state.nameDrafts[vehicle.id] ?: vehicle.name
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = vehicle.type.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = draftName,
                        onValueChange = { onEvent(VehiclesUiEvent.ChangeVehicleName(vehicle.id, it)) },
                        label = { Text(stringResource(R.string.label_vehicle_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onEvent(VehiclesUiEvent.SaveVehicleName(vehicle.id)) }) {
                            Text(stringResource(R.string.action_save_name))
                        }

                        Text(
                            text = lastOdometerText(vehicle.id, state.odometerRecords),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))

        Text(stringResource(R.string.label_register_odometer), style = MaterialTheme.typography.titleMedium)
        VehicleChipSelector(
            vehicles = state.vehicles,
            selectedVehicleId = state.selectedVehicleId,
            onSelect = { onEvent(VehiclesUiEvent.SelectVehicle(it)) }
        )

        OutlinedTextField(
            value = state.dateText,
            onValueChange = { onEvent(VehiclesUiEvent.ChangeDate(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.label_date_br)) },
            singleLine = true
        )

        OutlinedTextField(
            value = state.odometerText,
            onValueChange = { onEvent(VehiclesUiEvent.ChangeOdometer(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.label_odometer_km)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )

        Button(
            onClick = { onEvent(VehiclesUiEvent.SaveOdometer) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.action_save_odometer))
        }

        state.feedback?.let {
            Text(it, color = MaterialTheme.colorScheme.primary)
        }

        Text(
            text = stringResource(R.string.hint_odometer_month),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun lastOdometerText(vehicleId: Long, records: List<OdometerRecord>): String {
    val latest = records
        .filter { it.vehicleId == vehicleId }
        .maxByOrNull { it.dateEpochDay }

    return if (latest == null) {
        stringResource(R.string.text_no_odometer)
    } else {
        stringResource(R.string.text_last_odometer, formatNumber(latest.odometerKm))
    }
}
