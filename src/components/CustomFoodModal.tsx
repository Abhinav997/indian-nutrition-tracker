import React, { useState, useEffect } from 'react';
import { X, Plus, Check, Utensils, Info, Sparkles } from 'lucide-react';
import { CustomFood } from '../types';

interface CustomFoodModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSaveCustomFood: (food: Omit<CustomFood, 'id' | 'created_at'>, editId?: string) => void;
  foodToEdit?: CustomFood | null;
}

export const CustomFoodModal: React.FC<CustomFoodModalProps> = ({
  isOpen,
  onClose,
  onSaveCustomFood,
  foodToEdit,
}) => {
  if (!isOpen) return null;

  const [name, setName] = useState('');
  const [kcal100g, setKcal100g] = useState<number | ''>('');
  const [protein100g, setProtein100g] = useState<number | ''>('');
  const [carbs100g, setCarbs100g] = useState<number | ''>('');
  const [fat100g, setFat100g] = useState<number | ''>('');
  const [fiber100g, setFiber100g] = useState<number | ''>('');
  const [servingDesc, setServingDesc] = useState('');
  const [servingGrams, setServingGrams] = useState<number | ''>('');
  const [notes, setNotes] = useState('');

  useEffect(() => {
    if (foodToEdit) {
      setName(foodToEdit.name);
      setKcal100g(foodToEdit.kcal_per_100g);
      setProtein100g(foodToEdit.protein_per_100g);
      setCarbs100g(foodToEdit.carbs_per_100g);
      setFat100g(foodToEdit.fat_per_100g);
      setFiber100g(foodToEdit.fiber_per_100g ?? '');
      setServingDesc(foodToEdit.typical_serving_description ?? '');
      setServingGrams(foodToEdit.typical_serving_grams ?? '');
      setNotes(foodToEdit.notes ?? '');
    } else {
      setName('');
      setKcal100g('');
      setProtein100g('');
      setCarbs100g('');
      setFat100g('');
      setFiber100g('');
      setServingDesc('');
      setServingGrams(100);
      setNotes('');
    }
  }, [foodToEdit, isOpen]);

  // Auto-calculate approximate calories if carbs, protein, fat entered but calories left empty
  const handleAutoCalcCalories = () => {
    const p = Number(protein100g) || 0;
    const c = Number(carbs100g) || 0;
    const f = Number(fat100g) || 0;
    const approx = Math.round(p * 4 + c * 4 + f * 9);
    if (approx > 0) {
      setKcal100g(approx);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      alert('Please enter food name.');
      return;
    }
    if (kcal100g === '' || Number(kcal100g) < 0) {
      alert('Please enter valid calories per 100g.');
      return;
    }

    onSaveCustomFood(
      {
        name: name.trim(),
        kcal_per_100g: Number(kcal100g) || 0,
        protein_per_100g: Number(protein100g) || 0,
        carbs_per_100g: Number(carbs100g) || 0,
        fat_per_100g: Number(fat100g) || 0,
        fiber_per_100g: fiber100g !== '' ? Number(fiber100g) : undefined,
        typical_serving_description: servingDesc.trim() || undefined,
        typical_serving_grams: servingGrams !== '' ? Number(servingGrams) : undefined,
        notes: notes.trim() || undefined,
      },
      foodToEdit?.id
    );
    onClose();
  };

  return (
    <div
      id="custom-food-modal-overlay"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/70 backdrop-blur-sm animate-fade-in"
      onClick={onClose}
    >
      <div
        id="custom-food-modal-dialog"
        className="bg-white rounded-2xl w-full max-w-md shadow-2xl overflow-hidden border border-slate-200 max-h-[90vh] flex flex-col"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="bg-slate-900 text-white p-4 flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <div className="p-2 bg-amber-500/20 text-amber-400 rounded-xl">
              <Utensils className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base font-bold">
                {foodToEdit ? 'Edit Custom Food' : 'Create Custom Food / Recipe'}
              </h2>
              <p className="text-xs text-slate-400">Save personal meals with exact macros</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-white p-1 rounded-lg hover:bg-slate-800 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-4 overflow-y-auto space-y-3.5 flex-1">
          {/* Food Name */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Food / Recipe Name *
            </label>
            <input
              type="text"
              id="custom-food-name-input"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. My Paneer Bhurji (Mom's style)"
              required
              className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3 py-2 text-sm font-semibold text-slate-900 focus:ring-2 focus:ring-teal-500 focus:outline-none"
            />
          </div>

          {/* Macro grid per 100g */}
          <div className="bg-slate-50 p-3 rounded-xl border border-slate-200 space-y-2.5">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-slate-700 uppercase tracking-wider">
                Nutritional Values per 100g *
              </span>
              <button
                type="button"
                onClick={handleAutoCalcCalories}
                className="text-[11px] text-teal-700 hover:text-teal-900 font-semibold underline flex items-center space-x-1"
                title="Calculate calories from 4P + 4C + 9F"
              >
                <Sparkles className="w-3 h-3 text-teal-600" />
                <span>Auto-calc kcal</span>
              </button>
            </div>

            <div className="grid grid-cols-2 gap-2">
              <div>
                <label className="block text-[11px] font-medium text-slate-600 mb-0.5">
                  Calories (kcal) *
                </label>
                <input
                  type="number"
                  id="custom-food-kcal-input"
                  min="0"
                  max="1000"
                  step="1"
                  value={kcal100g}
                  onChange={(e) => setKcal100g(e.target.value === '' ? '' : Number(e.target.value))}
                  placeholder="e.g. 210"
                  required
                  className="w-full bg-white border border-slate-300 rounded-lg px-2.5 py-1.5 text-xs font-bold text-slate-900 focus:ring-2 focus:ring-teal-500 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-[11px] font-medium text-emerald-700 mb-0.5">
                  Protein (g) *
                </label>
                <input
                  type="number"
                  id="custom-food-protein-input"
                  min="0"
                  max="100"
                  step="0.1"
                  value={protein100g}
                  onChange={(e) => setProtein100g(e.target.value === '' ? '' : Number(e.target.value))}
                  placeholder="e.g. 14.5"
                  required
                  className="w-full bg-white border border-slate-300 rounded-lg px-2.5 py-1.5 text-xs font-bold text-emerald-700 focus:ring-2 focus:ring-teal-500 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-[11px] font-medium text-slate-600 mb-0.5">
                  Carbs (g)
                </label>
                <input
                  type="number"
                  id="custom-food-carbs-input"
                  min="0"
                  max="100"
                  step="0.1"
                  value={carbs100g}
                  onChange={(e) => setCarbs100g(e.target.value === '' ? '' : Number(e.target.value))}
                  placeholder="e.g. 8.0"
                  className="w-full bg-white border border-slate-300 rounded-lg px-2.5 py-1.5 text-xs font-semibold text-slate-800 focus:ring-2 focus:ring-teal-500 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-[11px] font-medium text-slate-600 mb-0.5">
                  Fat (g)
                </label>
                <input
                  type="number"
                  id="custom-food-fat-input"
                  min="0"
                  max="100"
                  step="0.1"
                  value={fat100g}
                  onChange={(e) => setFat100g(e.target.value === '' ? '' : Number(e.target.value))}
                  placeholder="e.g. 12.0"
                  className="w-full bg-white border border-slate-300 rounded-lg px-2.5 py-1.5 text-xs font-semibold text-slate-800 focus:ring-2 focus:ring-teal-500 focus:outline-none"
                />
              </div>
            </div>
          </div>

          {/* Standard Serving info */}
          <div className="grid grid-cols-2 gap-2">
            <div>
              <label className="block text-[11px] font-semibold text-slate-700 mb-1">
                Typical Serving Desc
              </label>
              <input
                type="text"
                id="custom-food-serving-desc-input"
                value={servingDesc}
                onChange={(e) => setServingDesc(e.target.value)}
                placeholder="e.g. 1 plate, 1 bowl"
                className="w-full bg-slate-50 border border-slate-300 rounded-lg px-2.5 py-1.5 text-xs text-slate-800 focus:ring-2 focus:ring-teal-500 focus:outline-none"
              />
            </div>

            <div>
              <label className="block text-[11px] font-semibold text-slate-700 mb-1">
                Typical Grams
              </label>
              <input
                type="number"
                id="custom-food-serving-grams-input"
                min="1"
                max="2000"
                value={servingGrams}
                onChange={(e) => setServingGrams(e.target.value === '' ? '' : Number(e.target.value))}
                placeholder="e.g. 150"
                className="w-full bg-slate-50 border border-slate-300 rounded-lg px-2.5 py-1.5 text-xs text-slate-800 focus:ring-2 focus:ring-teal-500 focus:outline-none"
              />
            </div>
          </div>

          {/* Recipe Notes */}
          <div>
            <label className="block text-[11px] font-semibold text-slate-700 mb-1">
              Recipe Ingredients / Notes (Optional)
            </label>
            <textarea
              id="custom-food-notes-input"
              rows={2}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="e.g. 100g paneer + 1 tsp oil + 1 chopped onion & tomato"
              className="w-full bg-slate-50 border border-slate-300 rounded-lg px-2.5 py-1.5 text-xs text-slate-800 focus:ring-2 focus:ring-teal-500 focus:outline-none resize-none"
            />
          </div>

          <div className="flex items-center space-x-2 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 py-2.5 px-4 rounded-xl border border-slate-300 text-xs font-semibold text-slate-700 hover:bg-slate-100 transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              id="save-custom-food-submit-btn"
              className="flex-1 py-2.5 px-4 rounded-xl bg-amber-600 hover:bg-amber-700 text-white text-xs font-bold shadow-md hover:shadow-lg transition flex items-center justify-center space-x-1"
            >
              <Check className="w-4 h-4" />
              <span>{foodToEdit ? 'Update Food' : 'Save Custom Food'}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
