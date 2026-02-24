package br.com.leo.leomotors

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import br.com.leo.leomotors.cloud.CloudSyncService
import br.com.leo.leomotors.cloud.SyncResponse
import br.com.leo.leomotors.data.LocalStore
import br.com.leo.leomotors.ui.theme.LeoMotorsTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class AppTab(val title: String) {
    REFUELS("Abastecimentos"),
    REPORTS("Relatorios"),
    CALCULATOR("Calculadora"),
    VEHICLES("Veiculos")
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun LeoMotorsRoot() {
    val context = LocalContext.current
    val store = remember { LocalStore(context) }
    var isDarkTheme by remember { mutableStateOf(store.isDarkThemeEnabled()) }

    LeoMotorsTheme(darkTheme = isDarkTheme) {
        LeoMotorsApp(
            store = store,
            isDarkTheme = isDarkTheme,
            onToggleTheme = {
                val nextValue = !isDarkTheme
                isDarkTheme = nextValue
                store.setDarkThemeEnabled(nextValue)
            }
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LeoMotorsApp(
    store: LocalStore,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val cloudSyncService = remember(context) { CloudSyncService(context.applicationContext) }

    var vehicles by remember { mutableStateOf(store.getVehicles()) }
    var fuelRecords by remember { mutableStateOf(store.getFuelRecords()) }
    var odometerRecords by remember { mutableStateOf(store.getOdometerRecords()) }
    var cloudMessage by remember { mutableStateOf<String?>(null) }
    var cloudBusy by remember { mutableStateOf(false) }
    var cloudUserEmail by remember { mutableStateOf(cloudSyncService.currentUserEmail()) }

    fun refreshAll() {
        vehicles = store.getVehicles()
        fuelRecords = store.getFuelRecords()
        odometerRecords = store.getOdometerRecords()
    }

    fun refreshCloudUser() {
        cloudUserEmail = cloudSyncService.currentUserEmail()
    }

    fun handleCloudResult(result: Result<SyncResponse>) {
        result.onSuccess {
            cloudMessage = it.message
            if (it.localUpdated) {
                refreshAll()
            }
        }.onFailure {
            cloudMessage = it.message ?: "Falha na sincronizacao."
        }
        refreshCloudUser()
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        if (activityResult.resultCode != Activity.RESULT_OK || activityResult.data == null) {
            cloudMessage = "Login Google cancelado."
            return@rememberLauncherForActivityResult
        }

        val account = runCatching {
            GoogleSignIn.getSignedInAccountFromIntent(activityResult.data)
                .getResult(ApiException::class.java)
        }.getOrElse {
            cloudMessage = "Falha ao obter conta Google: ${it.message}"
            return@rememberLauncherForActivityResult
        }

        val idToken = account.idToken
        if (idToken.isNullOrBlank()) {
            cloudMessage = "ID token ausente. Verifique o client ID do Google."
            return@rememberLauncherForActivityResult
        }

        coroutineScope.launch {
            cloudBusy = true
            val signInResult = cloudSyncService.signInWithGoogleIdToken(idToken)
            signInResult.onSuccess {
                cloudMessage = "Logado como $it"
            }.onFailure {
                cloudMessage = it.message ?: "Falha no login Google."
            }
            refreshCloudUser()
            cloudBusy = false
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    var showIntro by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(2200)
        showIntro = false
    }

    if (showIntro) {
        IntroPresentationScreen(isDarkTheme = isDarkTheme)
        return
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showAccountDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = {
                    AppTopBarTitle(isDarkTheme = isDarkTheme)
                },
                actions = {
                    IconButton(onClick = { showAccountDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "Conta e sincronizacao",
                            tint = if (cloudUserEmail != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = if (isDarkTheme) "Mudar para tema claro" else "Mudar para tema escuro",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                val tabs = AppTab.entries
                val safeSelectedTab = selectedTab.coerceIn(0, tabs.lastIndex)
                ScrollableTabRow(
                    selectedTabIndex = safeSelectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    edgePadding = 8.dp
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = safeSelectedTab == index,
                            onClick = { selectedTab = index },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            text = {
                                Text(
                                    text = tab.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }

                when (tabs[safeSelectedTab]) {
                    AppTab.VEHICLES -> VehiclesTab(
                        vehicles = vehicles,
                        odometerRecords = odometerRecords,
                        onRenameVehicle = { vehicleId, name ->
                            store.updateVehicleName(vehicleId, name)
                            refreshAll()
                        },
                        onAddOdometer = { vehicleId, dateEpochDay, odometerKm ->
                            store.addOdometerRecord(vehicleId, dateEpochDay, odometerKm)
                            refreshAll()
                        }
                    )

                    AppTab.REFUELS -> RefuelsTab(
                        vehicles = vehicles,
                        fuelRecords = fuelRecords,
                        onAddRefuel = { vehicleId, dateEpochDay, odometerKm, liters, pricePerLiter ->
                            store.addFuelRecord(vehicleId, dateEpochDay, odometerKm, liters, pricePerLiter)
                            refreshAll()
                        }
                    )

                    AppTab.REPORTS -> ReportsTab(
                        vehicles = vehicles,
                        fuelRecords = fuelRecords,
                        odometerRecords = odometerRecords
                    )

                    AppTab.CALCULATOR -> FuelCalculatorTab()
                }
            }

            if (showAccountDialog) {
                AccountSyncDialog(
                    userEmail = cloudUserEmail,
                    statusMessage = cloudMessage,
                    busy = cloudBusy,
                    onDismiss = { showAccountDialog = false },
                    onLoginGoogle = {
                        val clientId = resolveGoogleClientId(context)
                        if (clientId == null) {
                            cloudMessage = "Configure app/google-services.json (ou google_web_client_id em strings.xml)."
                            return@AccountSyncDialog
                        }
                        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestEmail()
                            .requestIdToken(clientId)
                            .build()
                        val signInClient = GoogleSignIn.getClient(context, options)
                        googleSignInLauncher.launch(signInClient.signInIntent)
                    },
                    onSignOut = {
                        cloudSyncService.signOut()
                        refreshCloudUser()
                        cloudMessage = "Sessao encerrada."
                    },
                    onUpload = {
                        coroutineScope.launch {
                            cloudBusy = true
                            handleCloudResult(cloudSyncService.uploadLocalState(store))
                            cloudBusy = false
                        }
                    },
                    onDownload = {
                        coroutineScope.launch {
                            cloudBusy = true
                            handleCloudResult(cloudSyncService.downloadRemoteState(store))
                            cloudBusy = false
                        }
                    },
                    onSyncNow = {
                        coroutineScope.launch {
                            cloudBusy = true
                            handleCloudResult(cloudSyncService.syncNow(store))
                            cloudBusy = false
                        }
                    }
                )
            }

            AppVersionBadge(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 10.dp, bottom = 10.dp)
            )
        }
    }
}
