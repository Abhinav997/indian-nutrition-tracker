package com.indian.nutrition.tracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DailyLogEntity)

    @Query("DELETE FROM daily_logs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM daily_logs ORDER BY date DESC, created_at ASC")
    fun observeAll(): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM daily_logs WHERE date = :date ORDER BY created_at ASC")
    fun observeForDate(date: String): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM daily_logs")
    suspend fun getAll(): List<DailyLogEntity>

    @Query("DELETE FROM daily_logs")
    suspend fun deleteAll()
}

@Dao
interface WeightLogDao {
    @Query("SELECT * FROM weight_logs ORDER BY date ASC")
    fun observeAll(): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM weight_logs WHERE date = :date LIMIT 1")
    suspend fun findByDate(date: String): WeightLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WeightLogEntity)

    @Query("UPDATE weight_logs SET weight_kg = :weightKg, note = :note, created_at = :createdAt WHERE id = :id")
    suspend fun update(id: String, weightKg: Double, note: String?, createdAt: Long)

    @Query("DELETE FROM weight_logs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM weight_logs")
    suspend fun deleteAll()
}

@Dao
interface WaterLogDao {
    @Insert
    suspend fun insert(entity: WaterLogEntity)

    @Query("DELETE FROM water_logs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM water_logs ORDER BY created_at ASC")
    fun observeAll(): Flow<List<WaterLogEntity>>

    @Query("SELECT * FROM water_logs WHERE date = :date ORDER BY created_at ASC")
    fun observeForDate(date: String): Flow<List<WaterLogEntity>>

    @Query("DELETE FROM water_logs")
    suspend fun deleteAll()
}

@Dao
interface CustomFoodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CustomFoodEntity)

    @Query("UPDATE custom_foods SET name = :name, kcal_per_100g = :kcal, protein_per_100g = :protein, " +
        "carbs_per_100g = :carbs, fat_per_100g = :fat, fiber_per_100g = :fiber, " +
        "typical_serving_description = :servingDesc, typical_serving_grams = :servingGrams, notes = :notes " +
        "WHERE id = :id")
    suspend fun update(
        id: String,
        name: String,
        kcal: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        fiber: Double?,
        servingDesc: String?,
        servingGrams: Int?,
        notes: String?,
    )

    @Query("DELETE FROM custom_foods WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM custom_foods ORDER BY created_at DESC")
    fun observeAll(): Flow<List<CustomFoodEntity>>
}

@Dao
interface OffCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: OffCacheEntity)

    @Query("SELECT * FROM off_cache")
    fun observeAll(): Flow<List<OffCacheEntity>>

    @Query("SELECT * FROM off_cache")
    suspend fun getAll(): List<OffCacheEntity>

    @Query("DELETE FROM off_cache WHERE last_fetched < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("DELETE FROM off_cache WHERE key IN (SELECT key FROM off_cache ORDER BY last_fetched ASC LIMIT :count)")
    suspend fun deleteOldest(count: Int)

    @Query("SELECT COUNT(*) FROM off_cache")
    suspend fun count(): Int

    @Query("DELETE FROM off_cache")
    suspend fun deleteAll()
}
