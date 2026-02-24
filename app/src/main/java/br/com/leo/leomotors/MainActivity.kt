package br.com.leo.leomotors

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import br.com.leo.leomotors.cloud.CloudSyncService
import br.com.leo.leomotors.cloud.SyncResponse
import br.com.leo.leomotors.data.FuelRecord
import br.com.leo.leomotors.data.LocalStore
import br.com.leo.leomotors.data.OdometerRecord
import br.com.leo.leomotors.data.PeriodReport
import br.com.leo.leomotors.data.ReportCalculator
import br.com.leo.leomotors.data.Vehicle
import br.com.leo.leomotors.reminder.ReminderScheduler
import br.com.leo.leomotors.ui.theme.LeoMotorsTheme
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ReminderScheduler.initialize(this)

        setContent {
            LeoMotorsRoot()
        }
    }
}

private enum class AppTab(val title: String) {
    ACCOUNT("Conta"),
    REFUELS("Abastecimentos"),
    REPORTS("Relatorios"),
    CALCULATOR("Calculadora"),
    VEHICLES("Veiculos")
}

private enum class MissingField(val label: String) {
    DISTANCE("Distancia"),
    CONSUMPTION("Consumo km/l"),
    LITERS("Litros")
}

private const val LOGO_DRAWABLE_NAME = "logo_leo_motors"
private const val LOGO_DRAWABLE_DARK_NAME = "logo_leo_motors_dark"
private const val INTRO_GIF_DRAWABLE_NAME = "intro_presentation"
private val DATE_BR_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.forLanguageTag("pt-BR"))

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LeoMotorsRoot() {
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
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    edgePadding = 8.dp
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTab == index,
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

                when (tabs[selectedTab]) {
                    AppTab.ACCOUNT -> CloudSyncTab(
                        userEmail = cloudUserEmail,
                        statusMessage = cloudMessage,
                        busy = cloudBusy,
                        onLoginGoogle = {
                            val clientId = resolveGoogleClientId(context)
                            if (clientId == null) {
                                cloudMessage = "Configure app/google-services.json (ou google_web_client_id em strings.xml)."
                                return@CloudSyncTab
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

            AppVersionBadge(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 10.dp, bottom = 10.dp)
            )
        }
    }
}

@Composable
private fun AppVersionBadge(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val versionText = remember(context) {
        runCatching {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            "v${packageInfo.versionName ?: "-"} ($versionCode)"
        }.getOrDefault("v-")
    }

    Text(
        text = versionText,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                shape = RectangleShape
            )
            .padding(horizontal = 6.dp, vertical = 3.dp)
    )
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
    onSyncNow: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
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

@Composable
private fun IntroPresentationScreen(isDarkTheme: Boolean) {
    val gifId = drawableIdByName(INTRO_GIF_DRAWABLE_NAME)
    val logoId = drawableIdByName(if (isDarkTheme) LOGO_DRAWABLE_DARK_NAME else LOGO_DRAWABLE_NAME)
    val fallbackLogoId = drawableIdByName(LOGO_DRAWABLE_NAME)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (gifId != 0) {
            GifImage(
                drawableId = gifId,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.72f)
                    .aspectRatio(9f / 16f)
            )
        }

        if (logoId != 0 || fallbackLogoId != 0) {
            Image(
                painter = painterResource(id = if (logoId != 0) logoId else fallbackLogoId),
                contentDescription = "Logo Leo Motors",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(0.42f)
                    .padding(bottom = 36.dp)
                    .alpha(0.9f)
            )
        } else {
            Text(
                text = "LEO MOTORS",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun AppTopBarTitle(isDarkTheme: Boolean) {
    val logoId = drawableIdByName(if (isDarkTheme) LOGO_DRAWABLE_DARK_NAME else LOGO_DRAWABLE_NAME)
    val fallbackLogoId = drawableIdByName(LOGO_DRAWABLE_NAME)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        if (logoId != 0 || fallbackLogoId != 0) {
            Image(
                painter = painterResource(id = if (logoId != 0) logoId else fallbackLogoId),
                contentDescription = "Logo Leo Motors",
                contentScale = ContentScale.Fit,
                modifier = Modifier.height(44.dp)
            )
        } else {
            Text("Leo Motors")
        }
    }
}

@Composable
private fun GifImage(drawableId: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(drawableId)
            .crossfade(false)
            .build(),
        imageLoader = imageLoader,
        contentDescription = "Apresentacao Leo Motors",
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

@Composable
private fun drawableIdByName(name: String): Int {
    val context = LocalContext.current
    return remember(name, context) {
        context.resources.getIdentifier(name, "drawable", context.packageName)
    }
}

private fun resolveGoogleClientId(context: Context): String? {
    val manual = context.getString(R.string.google_web_client_id).trim()
    if (manual.isNotEmpty()) return manual

    val generatedResId = context.resources.getIdentifier(
        "default_web_client_id",
        "string",
        context.packageName
    )
    if (generatedResId != 0) {
        val generated = context.getString(generatedResId).trim()
        if (generated.isNotEmpty()) return generated
    }

    return null
}

@Composable
private fun VehiclesTab(
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
private fun RefuelsTab(
    vehicles: List<Vehicle>,
    fuelRecords: List<FuelRecord>,
    onAddRefuel: (Long, Long, Double, Double, Double) -> Unit
) {
    var selectedVehicleId by remember(vehicles) {
        mutableLongStateOf(vehicles.firstOrNull()?.id ?: -1L)
    }
    var dateText by remember { mutableStateOf(todayText()) }
    var odometerText by remember { mutableStateOf("") }
    var litersText by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Lancamento de abastecimento", style = MaterialTheme.typography.titleLarge)

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
        OutlinedTextField(
            value = litersText,
            onValueChange = { litersText = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Litros abastecidos") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )
        OutlinedTextField(
            value = priceText,
            onValueChange = { priceText = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Preco por litro (R$)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
            modifier = Modifier.fillMaxWidth()
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
private fun ReportsTab(
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
private fun FuelCalculatorTab() {
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
