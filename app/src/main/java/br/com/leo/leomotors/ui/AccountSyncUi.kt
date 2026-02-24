package br.com.leo.leomotors

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
internal fun AccountSyncDialog(
    userEmail: String?,
    statusMessage: String?,
    busy: Boolean,
    onDismiss: () -> Unit,
    onLoginGoogle: () -> Unit,
    onSignOut: () -> Unit,
    onUpload: () -> Unit,
    onDownload: () -> Unit,
    onSyncNow: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                CloudSyncTab(
                    userEmail = userEmail,
                    statusMessage = statusMessage,
                    busy = busy,
                    onLoginGoogle = onLoginGoogle,
                    onSignOut = onSignOut,
                    onUpload = onUpload,
                    onDownload = onDownload,
                    onSyncNow = onSyncNow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 560.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Fechar")
                    }
                }
            }
        }
    }
}

@Composable
private fun CloudSyncTab(
    userEmail: String?,
    statusMessage: String?,
    busy: Boolean,
    onLoginGoogle: () -> Unit,
    onSignOut: () -> Unit,
    onUpload: () -> Unit,
    onDownload: () -> Unit,
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Conta e sincronizacao", style = MaterialTheme.typography.titleLarge)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (userEmail == null) {
                    Text("Você ainda não está logado.")
                    Button(
                        onClick = onLoginGoogle,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Entrar com Google")
                    }
                } else {
                    Text("Logado como:")
                    Text(userEmail, fontWeight = FontWeight.Bold)

                    OutlinedButton(
                        onClick = onSignOut,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sair da conta")
                    }

                    Button(
                        onClick = onSyncNow,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sincronizar agora")
                    }

                    OutlinedButton(
                        onClick = onUpload,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enviar para nuvem")
                    }

                    OutlinedButton(
                        onClick = onDownload,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Baixar da nuvem")
                    }
                }

                if (busy) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.height(18.dp))
                        Text("Processando...")
                    }
                }

                statusMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Text(
            "Conflito simples: o snapshot com timestamp mais recente vence (local ou nuvem).",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
