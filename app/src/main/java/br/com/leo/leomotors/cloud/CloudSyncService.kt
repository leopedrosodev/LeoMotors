package br.com.leo.leomotors.cloud

import android.content.Context
import br.com.leo.leomotors.data.FuelRecord
import br.com.leo.leomotors.data.LocalStateSnapshot
import br.com.leo.leomotors.data.LocalStore
import br.com.leo.leomotors.data.OdometerRecord
import br.com.leo.leomotors.data.Vehicle
import br.com.leo.leomotors.data.VehicleType
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class CloudSyncService(private val context: Context) {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    companion object {
        private const val COLLECTION = "leo_motors_users"
        private const val FIELD_UPDATED_AT = "updatedAtMillis"
        private const val FIELD_VEHICLES = "vehicles"
        private const val FIELD_ODOMETER = "odometerRecords"
        private const val FIELD_FUEL = "fuelRecords"
        private const val FIELD_SCHEMA = "schemaVersion"
        private const val SCHEMA_VERSION = 1L
    }

    fun currentUserEmail(): String? = auth.currentUser?.email

    fun isSignedIn(): Boolean = auth.currentUser != null

    suspend fun signInWithGoogleIdToken(idToken: String): Result<String> {
        return runCatching {
            ensureFirebaseConfiguredOrThrow()
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await()
            auth.currentUser?.email ?: "Conta Google conectada"
        }
    }

    fun signOut() {
        auth.signOut()
    }

    suspend fun uploadLocalState(localStore: LocalStore): Result<SyncResponse> {
        return runCatching {
            ensureSignedInOrThrow()
            val snapshot = localStore.getLocalStateSnapshot()
            writeRemoteSnapshot(snapshot)
            SyncResponse(
                message = "Dados locais enviados para a nuvem.",
                localUpdated = false
            )
        }
    }

    suspend fun downloadRemoteState(localStore: LocalStore): Result<SyncResponse> {
        return runCatching {
            ensureSignedInOrThrow()
            val remote = readRemoteSnapshot()
                ?: throw IllegalStateException("Nenhum backup remoto encontrado para esta conta.")
            localStore.restoreLocalState(remote)
            SyncResponse(
                message = "Dados baixados da nuvem para o aparelho.",
                localUpdated = true
            )
        }
    }

    suspend fun syncNow(localStore: LocalStore): Result<SyncResponse> {
        return runCatching {
            ensureSignedInOrThrow()

            val local = localStore.getLocalStateSnapshot()
            val remote = readRemoteSnapshot()

            if (remote == null) {
                writeRemoteSnapshot(local)
                return@runCatching SyncResponse(
                    message = "Primeiro backup criado na nuvem.",
                    localUpdated = false
                )
            }

            when {
                remote.updatedAtMillis > local.updatedAtMillis -> {
                    localStore.restoreLocalState(remote)
                    SyncResponse(
                        message = "Conflito resolvido: nuvem estava mais recente e substituiu o local.",
                        localUpdated = true
                    )
                }

                remote.updatedAtMillis < local.updatedAtMillis -> {
                    writeRemoteSnapshot(local)
                    SyncResponse(
                        message = "Conflito resolvido: local estava mais recente e foi enviado para a nuvem.",
                        localUpdated = false
                    )
                }

                else -> {
                    SyncResponse(
                        message = "Dados já estão sincronizados.",
                        localUpdated = false
                    )
                }
            }
        }
    }

    private suspend fun writeRemoteSnapshot(snapshot: LocalStateSnapshot) {
        val uid = requireNotNull(auth.currentUser?.uid)
        firestore.collection(COLLECTION)
            .document(uid)
            .set(snapshot.toRemoteMap())
            .await()
    }

    private suspend fun readRemoteSnapshot(): LocalStateSnapshot? {
        val uid = requireNotNull(auth.currentUser?.uid)
        val doc = firestore.collection(COLLECTION)
            .document(uid)
            .get()
            .await()

        if (!doc.exists()) return null

        val data = doc.data ?: return null

        val updatedAtMillis = asLong(data[FIELD_UPDATED_AT]) ?: 0L
        val vehicles = asMapList(data[FIELD_VEHICLES]).mapNotNull(::mapToVehicle)
        val odometerRecords = asMapList(data[FIELD_ODOMETER]).mapNotNull(::mapToOdometer)
        val fuelRecords = asMapList(data[FIELD_FUEL]).mapNotNull(::mapToFuel)

        return LocalStateSnapshot(
            vehicles = vehicles,
            odometerRecords = odometerRecords,
            fuelRecords = fuelRecords,
            updatedAtMillis = updatedAtMillis
        )
    }

    private fun ensureSignedInOrThrow() {
        ensureFirebaseConfiguredOrThrow()
        if (auth.currentUser == null) {
            throw IllegalStateException("Faça login com Google antes de sincronizar.")
        }
    }

    private fun ensureFirebaseConfiguredOrThrow() {
        if (FirebaseApp.getApps(context).isNotEmpty()) return
        val initialized = FirebaseApp.initializeApp(context)
        if (initialized == null) {
            throw IllegalStateException(
                "Firebase não configurado. Adicione o arquivo app/google-services.json e sincronize o projeto."
            )
        }
    }
}

data class SyncResponse(
    val message: String,
    val localUpdated: Boolean
)

private fun LocalStateSnapshot.toRemoteMap(): Map<String, Any> {
    return mapOf(
        FIELD_SCHEMA to SCHEMA_VERSION,
        FIELD_UPDATED_AT to updatedAtMillis,
        FIELD_VEHICLES to vehicles.map {
            mapOf(
                "id" to it.id,
                "name" to it.name,
                "type" to it.type.name
            )
        },
        FIELD_ODOMETER to odometerRecords.map {
            mapOf(
                "id" to it.id,
                "vehicleId" to it.vehicleId,
                "dateEpochDay" to it.dateEpochDay,
                "odometerKm" to it.odometerKm
            )
        },
        FIELD_FUEL to fuelRecords.map {
            mapOf(
                "id" to it.id,
                "vehicleId" to it.vehicleId,
                "dateEpochDay" to it.dateEpochDay,
                "odometerKm" to it.odometerKm,
                "liters" to it.liters,
                "pricePerLiter" to it.pricePerLiter
            )
        }
    )
}

private fun asMapList(value: Any?): List<Map<String, Any?>> {
    val raw = value as? List<*> ?: return emptyList()
    return raw.mapNotNull { it as? Map<String, Any?> }
}

private fun mapToVehicle(map: Map<String, Any?>): Vehicle? {
    val id = asLong(map["id"]) ?: return null
    val name = map["name"] as? String ?: return null
    val typeRaw = map["type"] as? String ?: VehicleType.CAR.name
    val type = runCatching { VehicleType.valueOf(typeRaw) }.getOrDefault(VehicleType.CAR)
    return Vehicle(id = id, name = name, type = type)
}

private fun mapToOdometer(map: Map<String, Any?>): OdometerRecord? {
    val id = asLong(map["id"]) ?: return null
    val vehicleId = asLong(map["vehicleId"]) ?: return null
    val dateEpochDay = asLong(map["dateEpochDay"]) ?: return null
    val odometerKm = asDouble(map["odometerKm"]) ?: return null
    return OdometerRecord(
        id = id,
        vehicleId = vehicleId,
        dateEpochDay = dateEpochDay,
        odometerKm = odometerKm
    )
}

private fun mapToFuel(map: Map<String, Any?>): FuelRecord? {
    val id = asLong(map["id"]) ?: return null
    val vehicleId = asLong(map["vehicleId"]) ?: return null
    val dateEpochDay = asLong(map["dateEpochDay"]) ?: return null
    val odometerKm = asDouble(map["odometerKm"]) ?: return null
    val liters = asDouble(map["liters"]) ?: return null
    val pricePerLiter = asDouble(map["pricePerLiter"]) ?: return null
    return FuelRecord(
        id = id,
        vehicleId = vehicleId,
        dateEpochDay = dateEpochDay,
        odometerKm = odometerKm,
        liters = liters,
        pricePerLiter = pricePerLiter
    )
}

private fun asLong(value: Any?): Long? {
    return when (value) {
        is Long -> value
        is Int -> value.toLong()
        is Double -> value.toLong()
        is Float -> value.toLong()
        is String -> value.toLongOrNull()
        else -> null
    }
}

private fun asDouble(value: Any?): Double? {
    return when (value) {
        is Double -> value
        is Float -> value.toDouble()
        is Long -> value.toDouble()
        is Int -> value.toDouble()
        is String -> value.toDoubleOrNull()
        else -> null
    }
}

private const val FIELD_UPDATED_AT = "updatedAtMillis"
private const val FIELD_VEHICLES = "vehicles"
private const val FIELD_ODOMETER = "odometerRecords"
private const val FIELD_FUEL = "fuelRecords"
private const val FIELD_SCHEMA = "schemaVersion"
private const val SCHEMA_VERSION = 1L
