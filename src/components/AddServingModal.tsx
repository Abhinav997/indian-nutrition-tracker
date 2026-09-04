import React, { useState, useEffect } from 'react';
import { X, Plus, Flame, Dumbbell, Sparkles, Check, Info } from 'lucide-react';
import { FoodMaster, MealType } from '../types';

interface AddServingModalProps {
  food: FoodMaster | null;
  selectedDate: string;
  defaultMealType?: MealType;
  isOpen: boolean;
  onClose: () => void;
  onSaveLog: (logData: {
    food_id: string;
    food_name: string;
    source: any;
    serving_grams: number;
    calories: number;
    protein: number;
    carbs: number;
    fat: number;
    meal_type: MealType;
  }) => void;
}

export const AddServingModal: React.FC<AddServingModalProps> = ({
  food,
  selectedDate,
  defaultMealType = 'Lunch',
  isOpen,
  onClose,
  onSaveLog,
}) => {
  if (!isOpen || !food) return null;

  const initialGrams = food.typical_serving_grams || 100;
  const [servingGrams, setServingGrams] = useState<number>(initialGrams);
  const [mealType, setMealType] = useState<MealType>(defaultMealType);
  const [quantityMultiplier, setQuantityMultiplier] = useState<number>(1);

  useEffect(() => {
    if (food) {
      setServingGrams(food.typical_serving_grams || 100);
      setQuantityMultiplier(1);
      setMealType(defaultMealType);
    }
  }, [food, defaultMealType]);

  const effectiveGrams = Math.max(1, Math.round(servingGrams * quantityMultiplier));

  // Compute live nutritional values
  const calories = Math.round((food.kcal_per_100g * effectiveGrams) / 100);
  const protein = Number(((food.protein_per_100g * effectiveGrams) / 100).toFixed(1));
  const carbs = Number(((food.carbs_per_100g * effectiveGrams) / 100).toFixed(1));
  const fat = Number(((food.fat_per_100g * effectiveGrams) / 100).toFixed(1));

  const mealTypes: MealType[] = ['Breakfast', 'Lunch', 'Snack', 'Dinner'];

  // Preset portion suggestions
  const presetPortions = [
    { label: '50g', grams: 50 },
    { label: '100g (Std)', grams: 100 },
    { label: '150g (1 Bowl)', grams: 150 },
    { label: '200g (1 Glass/Plate)', grams: 200 },
  ];

  if (food.typical_serving_grams && !presetPortions.some((p) => p.grams === food.typical_serving_grams)) {
    presetPortions.unshift({
      label: food.typical_serving_description || `${food.typical_serving_grams}g`,
      grams: food.typical_serving_grams,
    });
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSaveLog({
      food_id: food.id,
      food_name: food.name,
      source: food.source,
      serving_grams: effectiveGrams,
      calories,
      protein,
      carbs,
      fat,
      meal_type: mealType,
    });
    onClose();
  };

  const getSourceBadge = (source: string) => {
    switch (source) {
      case 'NIN':
        return <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-teal-100 text-teal-800 border border-teal-200">NIN / IFCT</span>;
      case 'OFF':
        return <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-indigo-100 text-indigo-800 border border-indigo-200">Open Food Facts</span>;
      case 'CUSTOM':
        return <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-amber-100 text-amber-800 border border-amber-200">Custom Recipe</span>;
      default:
        return null;
    }
  };

  return (
    <div
      id="add-serving-modal-overlay"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/70 backdrop-blur-sm animate-fade-in"
      onClick={onClose}
    >
      <div
        id="add-serving-modal-dialog"
        className="bg-white rounded-2xl w-full max-w-md shadow-2xl overflow-hidden border border-slate-200"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="bg-slate-900 text-white p-4">
          <div className="flex items-start justify-between">
            <div className="pr-4">
              <div className="flex items-center space-x-2 mb-1">
                {getSourceBadge(food.source)}
                {food.brand && (
                  <span className="text-xs text-slate-400 font-medium truncate max-w-[150px]">
                    {food.brand}
                  </span>
                )}
              </div>
              <h2 className="text-base font-bold leading-snug">{food.name}</h2>
              <p className="text-xs text-slate-400 mt-0.5">
                Per 100g: <span className="text-teal-300 font-semibold">{food.kcal_per_100g} kcal</span> • <span className="text-emerald-300 font-semibold">{food.protein_per_100g}g protein</span> • {food.carbs_per_100g}g C • {food.fat_per_100g}g F
              </p>
            </div>
            <button
              id="close-add-serving-modal-btn"
              onClick={onClose}
              className="text-slate-400 hover:text-white p-1 rounded-lg hover:bg-slate-800 transition"
              aria-label="Close dialog"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="p-4 space-y-4">
          {/* Meal Type Selector */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
              Meal Timing
            </label>
            <div className="grid grid-cols-4 gap-1.5">
              {mealTypes.map((type) => (
                <button
                  type="button"
                  key={type}
                  id={`meal-type-btn-${type.toLowerCase()}`}
                  onClick={() => setMealType(type)}
                  className={`py-1.5 px-2 text-xs font-medium rounded-lg text-center transition ${
                    mealType === type
                      ? 'bg-teal-600 text-white shadow-sm font-semibold'
                      : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                  }`}
                >
                  {type}
                </button>
              ))}
            </div>
          </div>

          {/* Quick Serving Presets */}
          <div>
            <div className="flex items-center justify-between mb-1.5">
              <label className="text-xs font-semibold text-slate-700 uppercase tracking-wider">
                Quick Portions
              </label>
              {food.typical_serving_description && (
                <span className="text-[11px] text-teal-700 font-medium">
                  {food.typical_serving_description}
                </span>
              )}
            </div>
            <div className="flex flex-wrap gap-1.5">
              {presetPortions.map((portion, idx) => (
                <button
                  type="button"
                  key={idx}
                  onClick={() => {
                    setServingGrams(portion.grams);
                    setQuantityMultiplier(1);
                  }}
                  className={`text-xs px-2.5 py-1 rounded-lg border transition ${
                    servingGrams === portion.grams && quantityMultiplier === 1
                      ? 'bg-teal-50 border-teal-500 text-teal-800 font-semibold'
                      : 'border-slate-200 text-slate-600 hover:bg-slate-50'
                  }`}
                >
                  {portion.label}
                </button>
              ))}
            </div>
          </div>

          {/* Serving Grams Custom Input */}
          <div className="bg-slate-50 p-3 rounded-xl border border-slate-200">
            <div className="flex items-center justify-between gap-3">
              <div className="flex-1">
                <label className="block text-xs font-medium text-slate-600 mb-1">
                  Serving Weight (grams)
                </label>
                <div className="relative">
                  <input
                    type="number"
                    id="serving-grams-input"
                    min="1"
                    max="3000"
                    step="1"
                    value={servingGrams || ''}
                    onChange={(e) => setServingGrams(Math.max(1, Number(e.target.value) || 0))}
                    className="w-full bg-white border border-slate-300 rounded-lg px-3 py-1.5 text-sm font-semibold text-slate-800 focus:outline-none focus:ring-2 focus:ring-teal-500"
                  />
                  <span className="absolute right-3 top-2 text-xs font-medium text-slate-400">
                    grams
                  </span>
                </div>
              </div>

              {/* Multiplier (e.g. 2 rotis or 1.5 cups) */}
              <div className="w-28">
                <label className="block text-xs font-medium text-slate-600 mb-1">
                  Qty / Multiplier
                </label>
                <div className="flex items-center space-x-1">
                  {[1, 2, 3].map((mult) => (
                    <button
                      type="button"
                      key={mult}
                      onClick={() => setQuantityMultiplier(mult)}
                      className={`flex-1 py-1.5 text-xs rounded font-semibold border transition ${
                        quantityMultiplier === mult
                          ? 'bg-slate-900 text-white border-slate-900'
                          : 'bg-white text-slate-700 border-slate-300 hover:bg-slate-100'
                      }`}
                    >
                      {mult}x
                    </button>
                  ))}
                </div>
              </div>
            </div>
          </div>

          {/* Computed Summary Card */}
          <div className="bg-gradient-to-br from-teal-500/10 via-emerald-500/5 to-cyan-500/10 p-3.5 rounded-xl border border-teal-200">
            <div className="flex items-center justify-between text-xs text-slate-600 font-medium mb-2">
              <span>Total for {effectiveGrams} grams:</span>
              <span className="font-semibold text-slate-800">{mealType}</span>
            </div>

            <div className="grid grid-cols-4 gap-2 text-center">
              <div className="bg-white/90 p-2 rounded-lg shadow-xs border border-teal-100">
                <div className="flex items-center justify-center text-amber-600 mb-0.5">
                  <Flame className="w-3.5 h-3.5" />
                </div>
                <div className="text-base font-black text-slate-900">{calories}</div>
                <div className="text-[10px] uppercase font-bold text-slate-500">kcal</div>
              </div>

              <div className="bg-white/90 p-2 rounded-lg shadow-xs border border-emerald-100">
                <div className="flex items-center justify-center text-emerald-600 mb-0.5">
                  <Dumbbell className="w-3.5 h-3.5" />
                </div>
                <div className="text-base font-black text-emerald-700">{protein}g</div>
                <div className="text-[10px] uppercase font-bold text-slate-500">Protein</div>
              </div>

              <div className="bg-white/90 p-2 rounded-lg shadow-xs border border-slate-100">
                <div className="text-xs font-semibold text-slate-400 mb-0.5">C</div>
                <div className="text-sm font-bold text-slate-700">{carbs}g</div>
                <div className="text-[10px] uppercase font-bold text-slate-400">Carbs</div>
              </div>

              <div className="bg-white/90 p-2 rounded-lg shadow-xs border border-slate-100">
                <div className="text-xs font-semibold text-slate-400 mb-0.5">F</div>
                <div className="text-sm font-bold text-slate-700">{fat}g</div>
                <div className="text-[10px] uppercase font-bold text-slate-400">Fat</div>
              </div>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="flex items-center space-x-2 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 py-2.5 px-4 rounded-xl border border-slate-300 text-sm font-semibold text-slate-700 hover:bg-slate-100 transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              id="confirm-add-serving-btn"
              className="flex-1 py-2.5 px-4 rounded-xl bg-teal-600 hover:bg-teal-700 text-white text-sm font-bold shadow-md hover:shadow-lg transition flex items-center justify-center space-x-1.5"
            >
              <Check className="w-4 h-4" />
              <span>Add to {selectedDate}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
