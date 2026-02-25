package br.com.leo.leomotors.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import br.com.leo.leomotors.data.local.dao.FuelDao
import br.com.leo.leomotors.data.local.dao.MaintenanceDao
import br.com.leo.leomotors.data.local.dao.OdometerDao
import br.com.leo.leomotors.data.local.dao.SettingsDao
import br.com.leo.leomotors.data.local.dao.VehicleDao
import br.com.leo.leomotors.data.local.entity.FuelRecordEntity
import br.com.leo.leomotors.data.local.entity.MaintenanceRecordEntity
import br.com.leo.leomotors.data.local.entity.OdometerRecordEntity
import br.com.leo.leomotors.data.local.entity.SettingsEntity
import br.com.leo.leomotors.data.local.entity.VehicleEntity

@Database(
    entities = [
        VehicleEntity::class,
        OdometerRecordEntity::class,
        FuelRecordEntity::class,
        MaintenanceRecordEntity::class,
        SettingsEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class LeoMotorsDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun odometerDao(): OdometerDao
    abstract fun fuelDao(): FuelDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun settingsDao(): SettingsDao
}
