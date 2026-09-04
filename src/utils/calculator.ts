import { ActivityLevel, CalculatorResult, GoalType, Sex, UserSettings } from '../types';

export const ACTIVITY_MULTIPLIERS: Record<ActivityLevel, { factor: number; label: string; desc: string }> = {
  'Sedentary': { factor: 1.2, label: 'Sedentary', desc: 'Little or no exercise, desk job' },
  'Light': { factor: 1.375, label: 'Light Exercise', desc: '1–3 days/week light workout or brisk walking' },
  'Moderate': { factor: 1.55, label: 'Moderate Exercise', desc: '3–5 days/week moderate gym or sports' },
  'Active': { factor: 1.725, label: 'Active', desc: '6–7 days/week hard exercise / physical job' },
  'Very Active': { factor: 1.9, label: 'Very Active', desc: 'Intense daily training / athlete / physical labor' },
};

export const PROTEIN_GUIDELINES: Record<GoalType, { min: number; max: number; default: number; desc: string }> = {
  'Lose': { min: 1.1, max: 1.3, default: 1.2, desc: '1.1–1.3 g/kg (Protects muscle during caloric deficit)' },
  'Maintain': { min: 1.0, max: 1.2, default: 1.1, desc: '1.0–1.2 g/kg (General health & maintenance)' },
  'Gain': { min: 1.2, max: 1.6, default: 1.5, desc: '1.2–1.6 g/kg (Muscle hypertrophy & lean mass building)' },
};

/**
 * Calculates BMR, TDEE, Calorie Target, and Protein Target using Mifflin-St Jeor Formula
 */
export function calculateTargets(settings: {
  current_weight_kg: number;
  target_weight_kg: number;
  height_cm: number;
  age_years: number;
  sex: Sex;
  activity_level: ActivityLevel;
  goal_type: GoalType;
  goal_rate_kg_per_week: number;
  protein_basis?: 'current' | 'target';
}): CalculatorResult {
  const {
    current_weight_kg,
    target_weight_kg,
    height_cm,
    age_years,
    sex,
    activity_level,
    goal_type,
    goal_rate_kg_per_week,
    protein_basis = 'current'
  } = settings;

  // Mifflin-St Jeor BMR calculation
  let bmrBase = 10 * current_weight_kg + 6.25 * height_cm - 5 * age_years;
  let sexOffset = 5;
  if (sex === 'F') {
    sexOffset = -161;
  } else if (sex === 'Other') {
    sexOffset = -78;
  }
  const bmr = Math.round(bmrBase + sexOffset);

  // TDEE calculation
  const activityFactor = ACTIVITY_MULTIPLIERS[activity_level]?.factor || 1.2;
  const tdee = Math.round(bmr * activityFactor);

  // Calorie Target adjustment based on Goal Type & Rate
  // 1 kg body fat ≈ 7700 kcal, so 0.5 kg/week ≈ 550 kcal/day deficit/surplus (standard 500 kcal approximation)
  let calorieAdjustment = 0;
  if (goal_type === 'Lose') {
    const rate = Math.abs(goal_rate_kg_per_week) || 0.5;
    calorieAdjustment = -Math.round((rate / 0.5) * 500);
  } else if (goal_type === 'Gain') {
    const rate = Math.abs(goal_rate_kg_per_week) || 0.25;
    calorieAdjustment = Math.round((rate / 0.25) * 250);
  } else {
    calorieAdjustment = 0;
  }

  // Sensible floor check (minimum 1200 kcal for women, 1400 for men unless strictly intentional)
  const minSafeCalories = sex === 'F' ? 1200 : 1400;
  const recommendedCalories = Math.max(minSafeCalories, tdee + calorieAdjustment);

  // Protein Target calculation based on weight (current vs target)
  const effectiveWeight = protein_basis === 'target' && target_weight_kg > 0 ? target_weight_kg : current_weight_kg;
  const proteinMultiplier = PROTEIN_GUIDELINES[goal_type]?.default || 1.2;
  const recommendedProtein = Math.round(effectiveWeight * proteinMultiplier);

  // Water Intake Target calculation: ~35ml per kg of body weight + activity hydration bonus
  const activityWaterBonus =
    activity_level === 'Very Active'
      ? 750
      : activity_level === 'Active'
      ? 500
      : activity_level === 'Moderate'
      ? 350
      : 0;
  const rawWaterMl = current_weight_kg * 35 + activityWaterBonus;
  // Round to nearest 250ml (1 standard glass)
  const recommendedWaterMl = Math.max(2000, Math.round(rawWaterMl / 250) * 250);

  // Formula strings for transparent user education
  const bmrFormula = sex === 'F'
    ? `10 × ${current_weight_kg}kg + 6.25 × ${height_cm}cm - 5 × ${age_years} - 161 = ${bmr} kcal/day`
    : `10 × ${current_weight_kg}kg + 6.25 × ${height_cm}cm - 5 × ${age_years} + 5 = ${bmr} kcal/day`;

  const tdeeFormula = `${bmr} (BMR) × ${activityFactor} (${activity_level}) = ${tdee} kcal/day`;
  
  const targetFormula = goal_type === 'Lose'
    ? `${tdee} (TDEE) - ${Math.abs(calorieAdjustment)} kcal (${Math.abs(goal_rate_kg_per_week)} kg/wk deficit) = ${recommendedCalories} kcal`
    : goal_type === 'Gain'
    ? `${tdee} (TDEE) + ${calorieAdjustment} kcal (${goal_rate_kg_per_week} kg/wk surplus) = ${recommendedCalories} kcal`
    : `${tdee} kcal (Maintenance)`;

  const proteinFormula = `${effectiveWeight}kg (${protein_basis} weight) × ${proteinMultiplier} g/kg (${goal_type} goal) = ${recommendedProtein} g`;

  const waterFormula = `${current_weight_kg}kg × 35ml + ${activityWaterBonus}ml (${activity_level}) = ${recommendedWaterMl} ml/day (~${(recommendedWaterMl / 1000).toFixed(1)}L)`;

  return {
    bmr,
    tdee,
    activityFactor,
    recommendedCalories,
    calorieAdjustment,
    recommendedProtein,
    proteinMultiplier,
    effectiveWeight,
    recommendedWaterMl,
    formulaDetails: {
      bmrFormula,
      tdeeFormula,
      targetFormula,
      proteinFormula,
      waterFormula,
    },
  };
}

export function formatWater(ml: number): { ml: number; liters: string; glasses: number } {
  return {
    ml,
    liters: (ml / 1000).toFixed(1),
    glasses: Math.round(ml / 250),
  };
}

export function kgToLb(kg: number): number {
  return Number((kg * 2.20462).toFixed(1));
}

export function lbToKg(lb: number): number {
  return Number((lb / 2.20462).toFixed(1));
}

export function formatWeight(kg: number, unit: 'kg' | 'lb' = 'kg'): string {
  if (unit === 'lb') {
    return `${kgToLb(kg)} lb`;
  }
  return `${Number(kg.toFixed(1))} kg`;
}

export function calculateBMI(weight_kg: number, height_cm: number): { bmi: number; category: string; color: string } {
  if (!weight_kg || !height_cm) return { bmi: 0, category: 'Unknown', color: 'text-slate-400' };
  const heightM = height_cm / 100;
  const bmi = Number((weight_kg / (heightM * heightM)).toFixed(1));
  if (bmi < 18.5) return { bmi, category: 'Underweight', color: 'text-amber-500' };
  if (bmi < 24.9) return { bmi, category: 'Normal weight', color: 'text-emerald-600' };
  if (bmi < 29.9) return { bmi, category: 'Overweight', color: 'text-amber-600' };
  return { bmi, category: 'Obese', color: 'text-rose-600' };
}
