package br.com.tec.tecmotors

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.tec.tecmotors.data.local.TecMotorsDatabase
import br.com.tec.tecmotors.data.local.migration.LegacyImportManager
import br.com.tec.tecmotors.data.local.migration.LegacySnapshotSource
import br.com.tec.tecmotors.domain.model.FuelRecord
import br.com.tec.tecmotors.domain.model.LocalStateSnapshot
import br.com.tec.tecmotors.domain.model.MaintenanceRecord
import br.com.tec.tecmotors.domain.model.MaintenanceType
import br.com.tec.tecmotors.domain.model.OdometerRecord
import br.com.tec.tecmotors.domain.model.Vehicle
import br.com.tec.tecmotors.domain.model.VehicleType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LegacyImportManagerInstrumentedTest {
    private lateinit var db: TecMotorsDatabase

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, TecMotorsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun importIfNeeded_isIdempotent_andPreservesIds() = runBlocking {
        val snapshot = LocalStateSnapshot(
            vehicles = listOf(Vehicle(10L, "Carro", VehicleType.CAR)),
            odometerRecords = listOf(OdometerRecord(20L, 10L, 100L, 1234.0)),
            fuelRecords = listOf(FuelRecord(30L, 10L, 100L, 1240.0, 20.0, 5.0)),
            maintenanceRecords = listOf(
                MaintenanceRecord(40L, 10L, MaintenanceType.OTHER, "Filtro", "", 100L, null, null, null, false)
            ),
            updatedAtMillis = 999L
        )

        val source = object : LegacySnapshotSource {
            override fun readSnapshot(): LocalStateSnapshot = snapshot
            override fun readDarkThemeEnabled(defaultValue: Boolean): Boolean = false
        }

        val manager = LegacyImportManager(db, source)

        manager.importIfNeeded()
        manager.importIfNeeded()

        val vehicles = db.vehicleDao().getAll()
        val odometers = db.odometerDao().getAll()
        val fuels = db.fuelDao().getAll()
        val maintenance = db.maintenanceDao().getAll()
        val settings = db.settingsDao().get()

        assertEquals(1, vehicles.size)
        assertEquals(10L, vehicles.first().id)
        assertEquals(1, odometers.size)
        assertEquals(20L, odometers.first().id)
        assertEquals(1, fuels.size)
        assertEquals(30L, fuels.first().id)
        assertEquals(1, maintenance.size)
        assertEquals(40L, maintenance.first().id)
        assertTrue(settings?.legacyImportDone == true)
    }
}
