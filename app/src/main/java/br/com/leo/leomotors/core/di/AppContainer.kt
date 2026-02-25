package br.com.leo.leomotors.core.di

import android.content.Context
import androidx.room.Room
import br.com.leo.leomotors.data.local.LeoMotorsDatabase
import br.com.leo.leomotors.data.local.RoomSnapshotDataSource
import br.com.leo.leomotors.data.local.migration.LegacyImportManager
import br.com.leo.leomotors.data.local.migration.LegacyPreferencesReader
import br.com.leo.leomotors.data.local.migration.RoomMigrations
import br.com.leo.leomotors.data.repository.MaintenanceRepositoryImpl
import br.com.leo.leomotors.data.repository.OdometerRepositoryImpl
import br.com.leo.leomotors.data.repository.RefuelRepositoryImpl
import br.com.leo.leomotors.data.repository.SettingsRepositoryImpl
import br.com.leo.leomotors.data.repository.SnapshotRepositoryImpl
import br.com.leo.leomotors.data.repository.SyncRepositoryImpl
import br.com.leo.leomotors.data.repository.VehicleRepositoryImpl
import br.com.leo.leomotors.domain.repository.MaintenanceRepository
import br.com.leo.leomotors.domain.repository.OdometerRepository
import br.com.leo.leomotors.domain.repository.RefuelRepository
import br.com.leo.leomotors.domain.repository.SettingsRepository
import br.com.leo.leomotors.domain.repository.SnapshotRepository
import br.com.leo.leomotors.domain.repository.SyncRepository
import br.com.leo.leomotors.domain.repository.VehicleRepository
import br.com.leo.leomotors.domain.usecase.AddMaintenanceUseCase
import br.com.leo.leomotors.domain.usecase.AddOdometerUseCase
import br.com.leo.leomotors.domain.usecase.AddRefuelUseCase
import br.com.leo.leomotors.domain.usecase.CalculateMaintenanceStatusUseCase
import br.com.leo.leomotors.domain.usecase.CalculateMonthlyMetricsUseCase
import br.com.leo.leomotors.domain.usecase.CalculatePeriodReportUseCase
import br.com.leo.leomotors.domain.usecase.CalculateVehicleSummaryUseCase
import br.com.leo.leomotors.domain.usecase.CurrentSyncUserUseCase
import br.com.leo.leomotors.domain.usecase.DownloadRemoteStateUseCase
import br.com.leo.leomotors.domain.usecase.EnsureDefaultVehiclesUseCase
import br.com.leo.leomotors.domain.usecase.ObserveDarkThemeUseCase
import br.com.leo.leomotors.domain.usecase.ObserveMaintenanceUseCase
import br.com.leo.leomotors.domain.usecase.ObserveOdometersUseCase
import br.com.leo.leomotors.domain.usecase.ObserveRefuelsUseCase
import br.com.leo.leomotors.domain.usecase.ObserveReportsDataUseCase
import br.com.leo.leomotors.domain.usecase.ObserveSettingsUseCase
import br.com.leo.leomotors.domain.usecase.ObserveVehiclesUseCase
import br.com.leo.leomotors.domain.usecase.RenameVehicleUseCase
import br.com.leo.leomotors.domain.usecase.SetDarkThemeUseCase
import br.com.leo.leomotors.domain.usecase.SetMaintenanceDoneUseCase
import br.com.leo.leomotors.domain.usecase.SignInWithGoogleUseCase
import br.com.leo.leomotors.domain.usecase.SignOutUseCase
import br.com.leo.leomotors.domain.usecase.SyncNowUseCase
import br.com.leo.leomotors.domain.usecase.UploadLocalStateUseCase

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: LeoMotorsDatabase = Room.databaseBuilder(
        appContext,
        LeoMotorsDatabase::class.java,
        "leo_motors.db"
    )
        .addMigrations(RoomMigrations.MIGRATION_1_2)
        .build()

    private val settingsRepository: SettingsRepository = SettingsRepositoryImpl(database)

    private val vehicleRepository: VehicleRepository = VehicleRepositoryImpl(
        database = database,
        settingsRepository = settingsRepository
    )

    private val odometerRepository: OdometerRepository = OdometerRepositoryImpl(
        database = database,
        settingsRepository = settingsRepository
    )

    private val refuelRepository: RefuelRepository = RefuelRepositoryImpl(
        database = database,
        settingsRepository = settingsRepository
    )

    private val maintenanceRepository: MaintenanceRepository = MaintenanceRepositoryImpl(
        database = database,
        settingsRepository = settingsRepository
    )

    private val snapshotDataSource = RoomSnapshotDataSource(database)

    private val snapshotRepository: SnapshotRepository = SnapshotRepositoryImpl(
        database = database,
        dataSource = snapshotDataSource
    )

    private val syncRepository: SyncRepository = SyncRepositoryImpl(
        context = appContext,
        snapshotRepository = snapshotRepository
    )

    val legacyImportManager = LegacyImportManager(
        database = database,
        reader = LegacyPreferencesReader(appContext)
    )

    val observeDarkThemeUseCase = ObserveDarkThemeUseCase(settingsRepository)
    val setDarkThemeUseCase = SetDarkThemeUseCase(settingsRepository)
    val observeSettingsUseCase = ObserveSettingsUseCase(settingsRepository)

    val observeVehiclesUseCase = ObserveVehiclesUseCase(vehicleRepository)
    val ensureDefaultVehiclesUseCase = EnsureDefaultVehiclesUseCase(vehicleRepository)
    val renameVehicleUseCase = RenameVehicleUseCase(vehicleRepository)
    val observeOdometersUseCase = ObserveOdometersUseCase(odometerRepository)
    val addOdometerUseCase = AddOdometerUseCase(odometerRepository)

    val observeRefuelsUseCase = ObserveRefuelsUseCase(refuelRepository)
    val addRefuelUseCase = AddRefuelUseCase(refuelRepository)

    val observeMaintenanceUseCase = ObserveMaintenanceUseCase(maintenanceRepository)
    val addMaintenanceUseCase = AddMaintenanceUseCase(maintenanceRepository)
    val setMaintenanceDoneUseCase = SetMaintenanceDoneUseCase(maintenanceRepository)
    val calculateMaintenanceStatusUseCase = CalculateMaintenanceStatusUseCase()

    val observeReportsDataUseCase = ObserveReportsDataUseCase(refuelRepository, odometerRepository)
    val calculatePeriodReportUseCase = CalculatePeriodReportUseCase()
    val calculateMonthlyMetricsUseCase = CalculateMonthlyMetricsUseCase(calculatePeriodReportUseCase)
    val calculateVehicleSummaryUseCase = CalculateVehicleSummaryUseCase()

    val signInWithGoogleUseCase = SignInWithGoogleUseCase(syncRepository)
    val signOutUseCase = SignOutUseCase(syncRepository)
    val uploadLocalStateUseCase = UploadLocalStateUseCase(syncRepository)
    val downloadRemoteStateUseCase = DownloadRemoteStateUseCase(syncRepository)
    val syncNowUseCase = SyncNowUseCase(syncRepository)
    val currentSyncUserUseCase = CurrentSyncUserUseCase(syncRepository)
}
