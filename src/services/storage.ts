import { NIN_IFCT_FOODS } from '../data/nin_ifct_data';
import { INDIAN_PACKAGED_FOODS } from '../data/packagedFoods';
import { CustomFood, DailyLog, FoodMaster, OffProductCache, UserSettings, WaterLog, WeightLog } from '../types';

const STORAGE_KEYS = {
  SETTINGS: 'inw_user_settings_v1',
  DAILY_LOGS: 'inw_daily_logs_v1',
  WEIGHT_LOGS: 'inw_weight_logs_v1',
  WATER_LOGS: 'inw_water_logs_v1',
  CUSTOM_FOODS: 'inw_custom_foods_v1',
  OFF_CACHE: 'inw_off_cache_v1',
};

// Default User Settings
export const DEFAULT_SETTINGS: UserSettings = {
  current_weight_kg: 82.0,
  target_weight_kg: 74.0,
  height_cm: 176,
  age_years: 28,
  sex: 'M',
  activity_level: 'Moderate',
  goal_type: 'Lose',
  goal_rate_kg_per_week: -0.5,
  daily_calorie_target: 1950,
  daily_protein_target: 115,
  daily_water_target_ml: 2750,
  protein_basis: 'current',
  unit_system: 'kg',
  default_chart_range: '14d',
};

/**
 * Format helper for YYYY-MM-DD
 */
export function formatDateKey(d: Date = new Date()): string {
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export class LocalStorageDatabase {
  constructor() {
    // Database initialized
  }

  // User Settings
  getSettings(): UserSettings {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.SETTINGS);
      if (!data) return DEFAULT_SETTINGS;
      return { ...DEFAULT_SETTINGS, ...JSON.parse(data) };
    } catch {
      return DEFAULT_SETTINGS;
    }
  }

  saveSettings(settings: UserSettings): void {
    localStorage.setItem(STORAGE_KEYS.SETTINGS, JSON.stringify(settings));
  }

  // Daily Food Logs
  getAllDailyLogs(): DailyLog[] {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.DAILY_LOGS);
      return data ? JSON.parse(data) : [];
    } catch {
      return [];
    }
  }

  getLogsForDate(dateStr: string): DailyLog[] {
    const all = this.getAllDailyLogs();
    return all.filter((item) => item.date === dateStr);
  }

  addDailyLog(log: Omit<DailyLog, 'id' | 'created_at'>): DailyLog {
    const all = this.getAllDailyLogs();
    const newLog: DailyLog = {
      ...log,
      id: `log_${Date.now()}_${Math.random().toString(36).substring(7)}`,
      created_at: Date.now(),
    };
    all.push(newLog);
    localStorage.setItem(STORAGE_KEYS.DAILY_LOGS, JSON.stringify(all));
    return newLog;
  }

  deleteDailyLog(id: string): void {
    const all = this.getAllDailyLogs().filter((item) => item.id !== id);
    localStorage.setItem(STORAGE_KEYS.DAILY_LOGS, JSON.stringify(all));
  }

  updateDailyLog(id: string, updates: Partial<DailyLog>): void {
    const all = this.getAllDailyLogs().map((item) => (item.id === id ? { ...item, ...updates } : item));
    localStorage.setItem(STORAGE_KEYS.DAILY_LOGS, JSON.stringify(all));
  }

  // Weight Logs
  getAllWeightLogs(): WeightLog[] {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.WEIGHT_LOGS);
      const list: WeightLog[] = data ? JSON.parse(data) : [];
      return list.sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime());
    } catch {
      return [];
    }
  }

  addWeightLog(date: string, weight_kg: number, note?: string): WeightLog {
    const all = this.getAllWeightLogs();
    // Check if an entry for this exact date already exists; update it or add new
    const existingIndex = all.findIndex((w) => w.date === date);
    let updated: WeightLog;
    if (existingIndex >= 0) {
      all[existingIndex] = {
        ...all[existingIndex],
        weight_kg,
        note: note ?? all[existingIndex].note,
        created_at: Date.now(),
      };
      updated = all[existingIndex];
    } else {
      updated = {
        id: `w_${Date.now()}_${Math.random().toString(36).substring(7)}`,
        date,
        weight_kg,
        note,
        created_at: Date.now(),
      };
      all.push(updated);
    }
    all.sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime());
    localStorage.setItem(STORAGE_KEYS.WEIGHT_LOGS, JSON.stringify(all));

    // Also optionally sync user current weight if this is today or most recent
    const latest = all[all.length - 1];
    if (latest) {
      const settings = this.getSettings();
      this.saveSettings({ ...settings, current_weight_kg: latest.weight_kg });
    }

    return updated;
  }

  deleteWeightLog(id: string): void {
    const all = this.getAllWeightLogs().filter((item) => item.id !== id);
    localStorage.setItem(STORAGE_KEYS.WEIGHT_LOGS, JSON.stringify(all));
  }

  // Water Logs
  getAllWaterLogs(): WaterLog[] {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.WATER_LOGS);
      return data ? JSON.parse(data) : [];
    } catch {
      return [];
    }
  }

  getWaterLogsForDate(dateStr: string): WaterLog[] {
    const all = this.getAllWaterLogs();
    return all.filter((item) => item.date === dateStr);
  }

  getWaterTotalForDate(dateStr: string): number {
    const logs = this.getWaterLogsForDate(dateStr);
    return logs.reduce((sum, item) => sum + item.amount_ml, 0);
  }

  addWaterLog(date: string, amount_ml: number, time?: string): WaterLog {
    const all = this.getAllWaterLogs();
    const now = new Date();
    const timeStr =
      time ||
      now.toLocaleTimeString('en-US', {
        hour: '2-digit',
        minute: '2-digit',
      });

    const newLog: WaterLog = {
      id: `water_${Date.now()}_${Math.random().toString(36).substring(7)}`,
      date,
      amount_ml,
      time: timeStr,
      created_at: Date.now(),
    };
    all.push(newLog);
    localStorage.setItem(STORAGE_KEYS.WATER_LOGS, JSON.stringify(all));
    return newLog;
  }

  deleteWaterLog(id: string): void {
    const all = this.getAllWaterLogs().filter((item) => item.id !== id);
    localStorage.setItem(STORAGE_KEYS.WATER_LOGS, JSON.stringify(all));
  }

  // Custom Foods
  getAllCustomFoods(): CustomFood[] {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.CUSTOM_FOODS);
      return data ? JSON.parse(data) : [];
    } catch {
      return [];
    }
  }

  addCustomFood(food: Omit<CustomFood, 'id' | 'created_at'>): CustomFood {
    const all = this.getAllCustomFoods();
    const newFood: CustomFood = {
      ...food,
      id: `cust_${Date.now()}_${Math.random().toString(36).substring(7)}`,
      created_at: Date.now(),
    };
    all.unshift(newFood);
    localStorage.setItem(STORAGE_KEYS.CUSTOM_FOODS, JSON.stringify(all));
    return newFood;
  }

  updateCustomFood(id: string, updates: Partial<CustomFood>): void {
    const all = this.getAllCustomFoods().map((item) => (item.id === id ? { ...item, ...updates } : item));
    localStorage.setItem(STORAGE_KEYS.CUSTOM_FOODS, JSON.stringify(all));
  }

  deleteCustomFood(id: string): void {
    const all = this.getAllCustomFoods().filter((item) => item.id !== id);
    localStorage.setItem(STORAGE_KEYS.CUSTOM_FOODS, JSON.stringify(all));
  }

  // Open Food Facts Local Cache
  getOffCache(): Record<string, OffProductCache> {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.OFF_CACHE);
      return data ? JSON.parse(data) : {};
    } catch {
      return {};
    }
  }

  cacheOffProduct(product: OffProductCache): void {
    const cache = this.getOffCache();
    const key = product.barcode || product.product_name.toLowerCase();
    cache[key] = product;
    localStorage.setItem(STORAGE_KEYS.OFF_CACHE, JSON.stringify(cache));
  }

  // Unified Food Master List (NIN + Custom Foods + Cached OFF)
  getUnifiedFoodMaster(): FoodMaster[] {
    const customFoods = this.getAllCustomFoods().map(
      (c): FoodMaster => ({
        id: c.id,
        name: c.name,
        source: 'CUSTOM',
        kcal_per_100g: c.kcal_per_100g,
        protein_per_100g: c.protein_per_100g,
        carbs_per_100g: c.carbs_per_100g,
        fat_per_100g: c.fat_per_100g,
        fiber_per_100g: c.fiber_per_100g,
        typical_serving_description: c.typical_serving_description || '1 serving',
        typical_serving_grams: c.typical_serving_grams || 100,
        category: 'Custom Foods',
      })
    );

    const offCached = Object.values(this.getOffCache()).map(
      (o): FoodMaster => ({
        id: `off_${o.barcode || o.product_name}`,
        name: o.product_name,
        brand: o.brand,
        source: 'OFF',
        kcal_per_100g: o.kcal_per_100g,
        protein_per_100g: o.protein_per_100g,
        carbs_per_100g: o.carbs_per_100g,
        fat_per_100g: o.fat_per_100g,
        typical_serving_description: '100g serving',
        typical_serving_grams: 100,
        category: 'Branded / Packaged',
      })
    );

    return [...customFoods, ...NIN_IFCT_FOODS, ...INDIAN_PACKAGED_FOODS, ...offCached];
  }

  // Frequently used foods ranking based on actual logs
  getFrequentlyUsedFoods(limit = 8): FoodMaster[] {
    const allLogs = this.getAllDailyLogs();
    const frequencyMap: Record<string, { count: number; name: string; source: any }> = {};

    allLogs.forEach((log) => {
      if (!frequencyMap[log.food_id]) {
        frequencyMap[log.food_id] = { count: 0, name: log.food_name, source: log.source };
      }
      frequencyMap[log.food_id].count += 1;
    });

    const master = this.getUnifiedFoodMaster();
    const masterMap = new Map(master.map((f) => [f.id, f]));

    const sortedIds = Object.keys(frequencyMap).sort(
      (a, b) => frequencyMap[b].count - frequencyMap[a].count
    );

    const result: FoodMaster[] = [];
    for (const id of sortedIds) {
      const food = masterMap.get(id);
      if (food) {
        result.push(food);
      }
      if (result.length >= limit) break;
    }

    // Fallback to top Indian staples if logs are few
    if (result.length < 4) {
      const defaults = NIN_IFCT_FOODS.slice(0, 8);
      defaults.forEach((d) => {
        if (!result.some((r) => r.id === d.id)) {
          result.push(d);
        }
      });
    }

    return result.slice(0, limit);
  }

  // Export & Reset
  exportDataJSON(): string {
    const data = {
      settings: this.getSettings(),
      dailyLogs: this.getAllDailyLogs(),
      weightLogs: this.getAllWeightLogs(),
      waterLogs: this.getAllWaterLogs(),
      customFoods: this.getAllCustomFoods(),
      exportedAt: new Date().toISOString(),
      version: '1.0',
    };
    return JSON.stringify(data, null, 2);
  }

  exportDataCSV(days?: number): string {
    let logs = this.getAllDailyLogs();
    let waters = this.getAllWaterLogs();
    let weights = this.getAllWeightLogs();

    if (days && days > 0) {
      const cutoffDate = new Date();
      cutoffDate.setDate(cutoffDate.getDate() - days);
      const cutoffStr = formatDateKey(cutoffDate);

      logs = logs.filter(l => l.date >= cutoffStr);
      waters = waters.filter(w => w.date >= cutoffStr);
      weights = weights.filter(w => w.date >= cutoffStr);
    }

    let csv = 'Type,Date,Detail1,Detail2,Detail3,Detail4,Detail5,Detail6,Detail7\n';

    csv += '# Food Logs: Type,Date,Meal,Food Name,Source,Serving (g),Calories (kcal),Protein (g),Carbs (g),Fat (g)\n';
    logs.forEach((l) => {
      const cleanName = `"${l.food_name.replace(/"/g, '""')}"`;
      csv += `FOOD,${l.date},${l.meal_type},${cleanName},${l.source},${l.serving_grams},${l.calories},${l.protein},${l.carbs},${l.fat}\n`;
    });

    csv += '# Water Logs: Type,Date,Time,Amount (ml)\n';
    waters.forEach((w) => {
      csv += `WATER,${w.date},"${w.time || ''}",${w.amount_ml}\n`;
    });

    csv += '# Weight Logs: Type,Date,Weight (kg),Note\n';
    weights.forEach((w) => {
      const cleanNote = w.note ? `"${w.note.replace(/"/g, '""')}"` : '';
      csv += `WEIGHT,${w.date},${w.weight_kg},${cleanNote}\n`;
    });

    return csv;
  }

  clearAllLogsToZero(): void {
    localStorage.setItem(STORAGE_KEYS.DAILY_LOGS, JSON.stringify([]));
    localStorage.setItem(STORAGE_KEYS.WEIGHT_LOGS, JSON.stringify([]));
    localStorage.setItem(STORAGE_KEYS.WATER_LOGS, JSON.stringify([]));
  }
}

export const db = new LocalStorageDatabase();
