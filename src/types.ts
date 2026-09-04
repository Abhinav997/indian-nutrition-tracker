export type FoodSource = 'NIN' | 'OFF' | 'CUSTOM';

export type MealType = 'Breakfast' | 'Lunch' | 'Snack' | 'Dinner';

export type Sex = 'M' | 'F' | 'Other';

export type ActivityLevel = 'Sedentary' | 'Light' | 'Moderate' | 'Active' | 'Very Active';

export type GoalType = 'Lose' | 'Maintain' | 'Gain';

export type UnitSystem = 'kg' | 'lb';

export type DateRange = '7d' | '14d' | '30d' | 'All';

export interface FoodMaster {
  id: string;
  name: string;
  source: FoodSource;
  kcal_per_100g: number;
  protein_per_100g: number;
  carbs_per_100g: number;
  fat_per_100g: number;
  fiber_per_100g?: number;
  typical_serving_description?: string;
  typical_serving_grams?: number;
  brand?: string;
  category?: string;
  barcode?: string;
  image_url?: string;
}

export interface DailyLog {
  id: string;
  date: string; // YYYY-MM-DD
  food_id: string;
  food_name: string;
  source: FoodSource;
  serving_grams: number;
  calories: number;
  protein: number;
  carbs: number;
  fat: number;
  meal_type: MealType;
  created_at: number;
}

export interface WeightLog {
  id: string;
  date: string; // YYYY-MM-DD
  weight_kg: number;
  note?: string;
  created_at: number;
}

export interface WaterLog {
  id: string;
  date: string; // YYYY-MM-DD
  amount_ml: number;
  time?: string;
  created_at: number;
}

export interface UserSettings {
  current_weight_kg: number;
  target_weight_kg: number;
  height_cm: number;
  age_years: number;
  sex: Sex;
  activity_level: ActivityLevel;
  goal_type: GoalType;
  goal_rate_kg_per_week: number; // e.g. -0.5, 0, +0.25
  daily_calorie_target: number;
  daily_protein_target: number;
  daily_water_target_ml: number;
  protein_basis: 'current' | 'target';
  unit_system: UnitSystem;
  default_chart_range: DateRange;
}

export interface OffProductCache {
  barcode?: string;
  product_name: string;
  brand?: string;
  kcal_per_100g: number;
  protein_per_100g: number;
  carbs_per_100g: number;
  fat_per_100g: number;
  last_fetched: string;
}

export interface CustomFood {
  id: string;
  name: string;
  kcal_per_100g: number;
  protein_per_100g: number;
  carbs_per_100g: number;
  fat_per_100g: number;
  fiber_per_100g?: number;
  typical_serving_description?: string;
  typical_serving_grams?: number;
  notes?: string;
  created_at: number;
}

export interface CalculatorResult {
  bmr: number;
  tdee: number;
  activityFactor: number;
  recommendedCalories: number;
  calorieAdjustment: number;
  recommendedProtein: number;
  proteinMultiplier: number;
  effectiveWeight: number;
  recommendedWaterMl: number;
  formulaDetails: {
    bmrFormula: string;
    tdeeFormula: string;
    targetFormula: string;
    proteinFormula: string;
    waterFormula: string;
  };
}
