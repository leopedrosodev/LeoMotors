package br.com.leo.leomotors.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface LeoMotorsDao {
    @Query("SELECT * FROM vehicles ORDER BY id ASC")
    suspend fun getVehicles(): List<VehicleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicles(vehicles: List<VehicleEntity>)

    @Query("DELETE FROM vehicles")
    suspend fun clearVehicles()

    @Query("UPDATE vehicles SET name = :name WHERE id = :vehicleId")
    suspend fun updateVehicleName(vehicleId: Long, name: String)

    @Query("SELECT * FROM odometer_records ORDER BY dateEpochDay DESC")
    suspend fun getOdometerRecords(): List<OdometerRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOdometerRecords(records: List<OdometerRecordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOdometerRecord(record: OdometerRecordEntity)

    @Query("DELETE FROM odometer_records")
    suspend fun clearOdometerRecords()

    @Query("SELECT * FROM fuel_records ORDER BY dateEpochDay DESC")
    suspend fun getFuelRecords(): List<FuelRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelRecords(records: List<FuelRecordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelRecord(record: FuelRecordEntity)

    @Query("DELETE FROM fuel_records")
    suspend fun clearFuelRecords()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeta(meta: MetaEntity)

    @Query("SELECT longValue FROM meta WHERE `key` = :key LIMIT 1")
    suspend fun getMetaLong(key: String): Long?

    @Transaction
    suspend fun replaceAll(
        vehicles: List<VehicleEntity>,
        odometerRecords: List<OdometerRecordEntity>,
        fuelRecords: List<FuelRecordEntity>
    ) {
        clearVehicles()
        clearOdometerRecords()
        clearFuelRecords()
        if (vehicles.isNotEmpty()) insertVehicles(vehicles)
        if (odometerRecords.isNotEmpty()) insertOdometerRecords(odometerRecords)
        if (fuelRecords.isNotEmpty()) insertFuelRecords(fuelRecords)
    }
}
