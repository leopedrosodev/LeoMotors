package br.com.leo.leomotors.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        VehicleEntity::class,
        OdometerRecordEntity::class,
        FuelRecordEntity::class,
        MetaEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class LeoMotorsDatabase : RoomDatabase() {
    abstract fun dao(): LeoMotorsDao

    companion object {
        @Volatile
        private var instance: LeoMotorsDatabase? = null

        fun getInstance(context: Context): LeoMotorsDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LeoMotorsDatabase::class.java,
                    "leo_motors.db"
                ).build().also { db ->
                    instance = db
                }
            }
        }
    }
}
