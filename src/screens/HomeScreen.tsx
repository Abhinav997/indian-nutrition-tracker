import React from 'react';
import {
  Flame,
  Dumbbell,
  Scale,
  Plus,
  Trash2,
  ChevronRight,
  TrendingDown,
  CheckCircle2,
  AlertTriangle,
  Utensils,
  Clock,
  Sparkles,
} from 'lucide-react';
import { DailyLog, MealType, UserSettings, WaterLog, WeightLog } from '../types';
import { formatWeight, calculateBMI } from '../utils/calculator';
import { WaterTracker } from '../components/WaterTracker';

interface HomeScreenProps {
  selectedDate: string;
  dailyLogs: DailyLog[];
  weightLogs: WeightLog[];
  waterLogs: WaterLog[];
  settings: UserSettings;
  onOpenFoodSearch: (mealType?: MealType) => void;
  onOpenLogWeight: () => void;
  onAddWater: (amountMl: number, time?: string) => void;
  onDeleteWaterLog: (id: string) => void;
  onDeleteLog: (id: string) => void;
  onNavigateToTab: (tab: any) => void;
}

export const HomeScreen: React.FC<HomeScreenProps> = ({
  selectedDate,
  dailyLogs,
  weightLogs,
  waterLogs,
  settings,
  onOpenFoodSearch,
  onOpenLogWeight,
  onAddWater,
  onDeleteWaterLog,
  onDeleteLog,
  onNavigateToTab,
}) => {
  // Aggregate today's macros
  const totalCalories = dailyLogs.reduce((acc, curr) => acc + curr.calories, 0);
  const totalProtein = Number(
    dailyLogs.reduce((acc, curr) => acc + curr.protein, 0).toFixed(1)
  );
  const totalCarbs = Number(
    dailyLogs.reduce((acc, curr) => acc + curr.carbs, 0).toFixed(1)
  );
  const totalFat = Number(
    dailyLogs.reduce((acc, curr) => acc + curr.fat, 0).toFixed(1)
  );

  const calorieTarget = settings.daily_calorie_target;
  const proteinTarget = settings.daily_protein_target;

  const calProgress = Math.min(100, Math.round((totalCalories / calorieTarget) * 100));
  const proteinProgress = Math.min(100, Math.round((totalProtein / proteinTarget) * 100));
  const remainingCalories = calorieTarget - totalCalories;
  const remainingProtein = Number((proteinTarget - totalProtein).toFixed(1));

  // Current weight and BMI
  const latestWeight = weightLogs.length > 0
    ? weightLogs[weightLogs.length - 1].weight_kg
    : settings.current_weight_kg;
  const startingWeight = weightLogs.length > 0 ? weightLogs[0].weight_kg : settings.current_weight_kg;
  const totalWeightLost = Number((startingWeight - latestWeight).toFixed(1));
  const bmiInfo = calculateBMI(latestWeight, settings.height_cm);

  // Group logs by meal type
  const mealSections: { type: MealType; label: string; time: string }[] = [
    { type: 'Breakfast', label: 'Breakfast', time: 'Morning' },
    { type: 'Lunch', label: 'Lunch', time: 'Afternoon' },
    { type: 'Snack', label: 'Snacks & Tea', time: 'Evening' },
    { type: 'Dinner', label: 'Dinner', time: 'Night' },
  ];

  return (
    <div className="space-y-4 pb-20">
      {/* Top Main Card: Today's Intake */}
      <div
        id="today-intake-card"
        className="bg-white rounded-2xl p-4 shadow-sm border border-slate-200 relative overflow-hidden"
      >
        <div className="flex items-center justify-between mb-3">
          <div>
            <h2 className="text-sm font-extrabold text-slate-900 uppercase tracking-wider">
              Today's Intake
            </h2>
            <p className="text-xs text-slate-500">Target vs Logged Nutrition</p>
          </div>
          <button
            id="home-quick-add-food-btn"
            onClick={() => onOpenFoodSearch()}
            className="flex items-center space-x-1 bg-teal-600 hover:bg-teal-700 text-white px-3 py-1.5 rounded-xl text-xs font-bold shadow-xs hover:shadow-md transition"
          >
            <Plus className="w-4 h-4" />
            <span>Add Food</span>
          </button>
        </div>

        {/* Calorie Progress Bar */}
        <div className="mb-4 bg-slate-50 p-3 rounded-xl border border-slate-100">
          <div className="flex items-center justify-between mb-1.5">
            <div className="flex items-center space-x-1.5">
              <Flame className="w-4 h-4 text-amber-500" />
              <span className="text-xs font-bold text-slate-800">Calories</span>
            </div>
            <div className="text-xs font-bold text-slate-900">
              <span className="text-teal-700 text-sm font-black">{totalCalories}</span> / {calorieTarget} kcal
            </div>
          </div>
          <div className="w-full bg-slate-200 rounded-full h-3 overflow-hidden">
            <div
              className={`h-full rounded-full transition-all duration-500 ${
                totalCalories > calorieTarget
                  ? 'bg-amber-500'
                  : 'bg-gradient-to-r from-teal-500 to-emerald-500'
              }`}
              style={{ width: `${Math.min(100, calProgress)}%` }}
            />
          </div>
          <div className="flex items-center justify-between text-[11px] font-medium text-slate-500 mt-1.5">
            <span>{calProgress}% consumed</span>
            <span>
              {remainingCalories >= 0 ? (
                <span className="text-emerald-700 font-semibold">{remainingCalories} kcal left</span>
              ) : (
                <span className="text-amber-600 font-semibold">+{Math.abs(remainingCalories)} kcal over target</span>
              )}
            </span>
          </div>
        </div>

        {/* Protein Progress Bar */}
        <div className="mb-4 bg-slate-50 p-3 rounded-xl border border-slate-100">
          <div className="flex items-center justify-between mb-1.5">
            <div className="flex items-center space-x-1.5">
              <Dumbbell className="w-4 h-4 text-emerald-600" />
              <span className="text-xs font-bold text-slate-800">Protein Target</span>
            </div>
            <div className="text-xs font-bold text-slate-900">
              <span className="text-emerald-700 text-sm font-black">{totalProtein}</span> / {proteinTarget} g
            </div>
          </div>
          <div className="w-full bg-slate-200 rounded-full h-3 overflow-hidden">
            <div
              className="h-full bg-gradient-to-r from-emerald-500 to-teal-500 rounded-full transition-all duration-500"
              style={{ width: `${Math.min(100, proteinProgress)}%` }}
            />
          </div>
          <div className="flex items-center justify-between text-[11px] font-medium text-slate-500 mt-1.5">
            <span>{proteinProgress}% reached</span>
            <span>
              {remainingProtein > 0 ? (
                <span className="text-slate-600 font-medium">{remainingProtein}g needed</span>
              ) : (
                <span className="text-emerald-600 font-bold flex items-center space-x-1">
                  <CheckCircle2 className="w-3 h-3 inline" /> <span>Goal achieved!</span>
                </span>
              )}
            </span>
          </div>
        </div>

        {/* Secondary Macro Breakdown (Carbs & Fat) */}
        <div className="grid grid-cols-2 gap-2 pt-1 border-t border-slate-100">
          <div className="bg-slate-50 px-3 py-2 rounded-lg flex items-center justify-between text-xs">
            <span className="text-slate-600 font-medium">Carbohydrates</span>
            <span className="font-bold text-slate-900">{totalCarbs}g</span>
          </div>
          <div className="bg-slate-50 px-3 py-2 rounded-lg flex items-center justify-between text-xs">
            <span className="text-slate-600 font-medium">Fats</span>
            <span className="font-bold text-slate-900">{totalFat}g</span>
          </div>
        </div>
      </div>

      {/* Small Weight Summary Card */}
      <div
        id="home-weight-summary-card"
        className="bg-white rounded-2xl p-4 shadow-sm border border-slate-200 flex items-center justify-between"
      >
        <div className="flex items-center space-x-3">
          <div className="p-2.5 bg-teal-50 text-teal-700 rounded-xl border border-teal-100">
            <Scale className="w-5 h-5" />
          </div>
          <div>
            <div className="text-xs text-slate-500 font-medium">Current Body Weight</div>
            <div className="text-lg font-black text-slate-900">
              {formatWeight(latestWeight, settings.unit_system)}
            </div>
            <div className="text-[11px] text-slate-500 flex items-center space-x-2">
              <span>Target: {formatWeight(settings.target_weight_kg, settings.unit_system)}</span>
              <span>•</span>
              <span className={`font-semibold ${bmiInfo.color}`}>BMI {bmiInfo.bmi}</span>
            </div>
          </div>
        </div>

        <button
          id="home-log-weight-btn"
          onClick={onOpenLogWeight}
          className="bg-slate-900 hover:bg-slate-800 text-white text-xs font-bold px-3.5 py-2 rounded-xl transition shadow-xs hover:shadow"
        >
          Log Weight
        </button>
      </div>

      {/* Water & Hydration Tracker */}
      <WaterTracker
        selectedDate={selectedDate}
        waterLogs={waterLogs}
        settings={settings}
        onAddWater={onAddWater}
        onDeleteWaterLog={onDeleteWaterLog}
      />

      {/* Meals Breakdown for the Selected Date */}
      <div className="space-y-3">
        <div className="flex items-center justify-between px-1">
          <h3 className="text-xs font-extrabold text-slate-800 uppercase tracking-wider">
            Meals Logged for {selectedDate}
          </h3>
          <span className="text-xs font-bold text-teal-700">
            {dailyLogs.length} items logged
          </span>
        </div>

        {mealSections.map((section) => {
          const items = dailyLogs.filter((log) => log.meal_type === section.type);
          const mealKcal = items.reduce((sum, item) => sum + item.calories, 0);
          const mealProtein = Number(items.reduce((sum, item) => sum + item.protein, 0).toFixed(1));

          return (
            <div
              key={section.type}
              className="bg-white rounded-2xl p-3.5 shadow-sm border border-slate-200 space-y-2.5"
            >
              <div className="flex items-center justify-between border-b border-slate-100 pb-2">
                <div className="flex items-center space-x-2">
                  <div className="w-2 h-2 rounded-full bg-teal-500" />
                  <span className="text-xs font-bold text-slate-900">{section.label}</span>
                  <span className="text-[11px] text-slate-400">({section.time})</span>
                </div>
                <div className="flex items-center space-x-2">
                  <span className="text-xs font-bold text-slate-700">
                    {mealKcal} kcal • {mealProtein}g P
                  </span>
                  <button
                    onClick={() => onOpenFoodSearch(section.type)}
                    className="text-teal-600 hover:text-teal-800 p-1 hover:bg-teal-50 rounded-lg transition"
                    title={`Add food to ${section.label}`}
                  >
                    <Plus className="w-4 h-4" />
                  </button>
                </div>
              </div>

              {items.length === 0 ? (
                <div className="text-center py-2 text-slate-400 text-xs flex items-center justify-center space-x-1.5">
                  <span>No items logged yet.</span>
                  <button
                    onClick={() => onOpenFoodSearch(section.type)}
                    className="text-teal-600 font-semibold hover:underline"
                  >
                    + Add to {section.label}
                  </button>
                </div>
              ) : (
                <div className="space-y-1.5">
                  {items.map((item) => (
                    <div
                      key={item.id}
                      className="flex items-center justify-between bg-slate-50 hover:bg-slate-100/80 p-2 rounded-xl transition border border-slate-100"
                    >
                      <div className="pr-2 flex-1 min-w-0">
                        <div className="flex items-center space-x-1.5">
                          <span className="text-xs font-bold text-slate-900 truncate">
                            {item.food_name}
                          </span>
                          <span
                            className={`text-[9px] font-bold px-1.5 py-0.2 rounded ${
                              item.source === 'NIN'
                                ? 'bg-teal-100 text-teal-800'
                                : item.source === 'OFF'
                                ? 'bg-indigo-100 text-indigo-800'
                                : 'bg-amber-100 text-amber-800'
                            }`}
                          >
                            {item.source}
                          </span>
                        </div>
                        <div className="text-[11px] text-slate-500">
                          {item.serving_grams}g • <span className="font-semibold text-slate-700">{item.calories} kcal</span> • <span className="font-semibold text-emerald-700">{item.protein}g protein</span>
                        </div>
                      </div>

                      <button
                        onClick={() => onDeleteLog(item.id)}
                        className="text-slate-400 hover:text-rose-600 p-1 rounded-lg hover:bg-rose-50 transition"
                        title="Delete log"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
};
