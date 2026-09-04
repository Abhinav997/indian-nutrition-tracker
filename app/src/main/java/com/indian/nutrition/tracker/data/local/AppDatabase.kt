package com.indian.nutrition.tracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DailyLogEntity::class,
        WeightLogEntity::class,
        WaterLogEntity::class,
        CustomFoodEntity::class,
        OffCacheEntity::class,
    ],
    version = 1,
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
                .build()
    }
}
