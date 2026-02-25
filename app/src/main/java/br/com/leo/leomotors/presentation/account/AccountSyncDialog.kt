package br.com.leo.leomotors.presentation.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import br.com.leo.leomotors.R

@Composable
fun AccountSyncDialog(
    state: AccountSyncUiState,
    onDismiss: () -> Unit,
    onLoginGoogle: () -> Unit,
    onEvent: (AccountSyncUiEvent) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 560.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(stringResource(R.string.title_account_sync), style = MaterialTheme.typography.titleLarge)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (state.userEmail == null) {
                                Text(stringResource(R.string.text_not_logged_in))
                                Button(
                                    onClick = onLoginGoogle,
                                    enabled = !state.busy,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.action_login_google))
                                }
                            } else {
                                Text(stringResource(R.string.text_logged_as))
                                Text(state.userEmail, fontWeight = FontWeight.Bold)

                                OutlinedButton(
                                    onClick = { onEvent(AccountSyncUiEvent.SignOut) },
                                    enabled = !state.busy,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.action_logout))
                                }

                                Button(
                                    onClick = { onEvent(AccountSyncUiEvent.SyncNow) },
                                    enabled = !state.busy,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.action_sync_now))
                                }

                                OutlinedButton(
                                    onClick = { onEvent(AccountSyncUiEvent.Upload) },
                                    enabled = !state.busy,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.action_upload_cloud))
                                }

                                OutlinedButton(
                                    onClick = { onEvent(AccountSyncUiEvent.Download) },
                                    enabled = !state.busy,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.action_download_cloud))
                                }
                            }

                            if (state.busy) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CircularProgressIndicator(modifier = Modifier.height(18.dp))
                                    Text(stringResource(R.string.text_processing))
                                }
                            }

                            state.statusMessage?.let {
                                Text(it, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Text(
                        stringResource(R.string.text_sync_conflict_hint),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_close))
                    }
                }
            }
        }
    }
}
