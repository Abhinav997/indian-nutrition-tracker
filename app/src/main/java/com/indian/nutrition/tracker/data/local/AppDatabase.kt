package com.indian.nutrition.tracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.RoomDatabase

@Database(
    entities = [
        DailyLogEntity::class,
        WeightLogEntity::class,
        WaterLogEntity::class,
        CustomFoodEntity::class,
        OffCacheEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dailyLogDao(): DailyLogDao
    abstract fun weightLogDao(): WeightLogDao
    abstract fun waterLogDao(): WaterLogDao
    abstract fun customFoodDao(): CustomFoodDao
    abstract fun offCacheDao(): OffCacheDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "inw.db")
                .addMigrations(MIGRATION_1_2)
                .build()

        /** Adds custom-food serving units without changing existing logs. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE custom_foods ADD COLUMN servingUnit TEXT NOT NULL DEFAULT 'GRAMS'",
                )
            }
        }
    }
}
