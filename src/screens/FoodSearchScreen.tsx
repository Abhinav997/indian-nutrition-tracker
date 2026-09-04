import React, { useState, useEffect, useMemo } from 'react';
import {
  Search,
  Plus,
  Flame,
  Dumbbell,
  Sparkles,
  Loader2,
  Globe,
  Utensils,
  History,
  Trash2,
  Edit2,
  Info,
} from 'lucide-react';
import { CustomFood, FoodMaster, FoodSource, MealType } from '../types';
import { searchOpenFoodFacts } from '../services/offApi';
import { db } from '../services/storage';

interface FoodSearchScreenProps {
  selectedDate: string;
  defaultMealType?: MealType;
  onSelectFood: (food: FoodMaster) => void;
  onOpenCustomFoodModal: (foodToEdit?: CustomFood | null) => void;
  onDeleteCustomFood: (id: string) => void;
}

export const FoodSearchScreen: React.FC<FoodSearchScreenProps> = ({
  selectedDate,
  defaultMealType = 'Lunch',
  onSelectFood,
  onOpenCustomFoodModal,
  onDeleteCustomFood,
}) => {
  const [activeTab, setActiveTab] = useState<'search' | 'custom' | 'frequent'>('search');
  const [searchQuery, setSearchQuery] = useState('');
  const [sourceFilter, setSourceFilter] = useState<'ALL' | FoodSource>('ALL');
  const [offLoading, setOffLoading] = useState(false);
  const [offApiResults, setOffApiResults] = useState<FoodMaster[]>([]);
  const [offError, setOffError] = useState<string | null>(null);

  // Local database foods (NIN + Custom + Cached OFF)
  const [unifiedMaster, setUnifiedMaster] = useState<FoodMaster[]>([]);
  const [frequentFoods, setFrequentFoods] = useState<FoodMaster[]>([]);
  const [customFoods, setCustomFoods] = useState<CustomFood[]>([]);

  useEffect(() => {
    refreshData();
  }, [activeTab]);

  const refreshData = () => {
    setUnifiedMaster(db.getUnifiedFoodMaster());
    setFrequentFoods(db.getFrequentlyUsedFoods(12));
    setCustomFoods(db.getAllCustomFoods());
  };

  // Debounced Open Food Facts API query when search term is entered
  useEffect(() => {
    const trimmed = searchQuery.trim();
    if (trimmed.length < 3 || activeTab !== 'search') {
      setOffApiResults([]);
      setOffLoading(false);
      setOffError(null);
      return;
    }

    const timer = setTimeout(async () => {
      setOffLoading(true);
      setOffError(null);
      try {
        const offCache = db.getOffCache();
        const res = await searchOpenFoodFacts(trimmed, offCache);
        if (res.error) {
          setOffError(res.error);
        }
        // Cache returned products
        res.foods.forEach((f) => {
          db.cacheOffProduct({
            barcode: f.barcode,
            product_name: f.name,
            brand: f.brand,
            kcal_per_100g: f.kcal_per_100g,
            protein_per_100g: f.protein_per_100g,
            carbs_per_100g: f.carbs_per_100g,
            fat_per_100g: f.fat_per_100g,
            last_fetched: new Date().toISOString(),
          });
        });
        setOffApiResults(res.foods);
      } catch (e: any) {
        setOffError('Could not connect to Open Food Facts API');
      } finally {
        setOffLoading(false);
      }
    }, 450);

    return () => clearTimeout(timer);
  }, [searchQuery, activeTab]);

  // Filter Local Unified Master results
  const filteredLocalResults = useMemo(() => {
    const q = searchQuery.toLowerCase().trim();
    return unifiedMaster.filter((item) => {
      const matchesQuery =
        !q ||
        item.name.toLowerCase().includes(q) ||
        (item.brand && item.brand.toLowerCase().includes(q)) ||
        (item.category && item.category.toLowerCase().includes(q));

      const matchesSource = sourceFilter === 'ALL' || item.source === sourceFilter;
      return matchesQuery && matchesSource;
    });
  }, [unifiedMaster, searchQuery, sourceFilter]);

  // Combined Results (Local Master + Live OFF search results)
  const combinedSearchResults = useMemo(() => {
    const map = new Map<string, FoodMaster>();
    filteredLocalResults.forEach((f) => map.set(f.id, f));
    offApiResults.forEach((f) => {
      if (!map.has(f.id)) {
        map.set(f.id, f);
      }
    });
    return Array.from(map.values());
  }, [filteredLocalResults, offApiResults]);

  const getSourceBadge = (source: FoodSource) => {
    switch (source) {
      case 'NIN':
        return (
          <span className="text-[10px] font-bold px-1.5 py-0.5 rounded bg-teal-100 text-teal-800 border border-teal-200">
            NIN / IFCT
          </span>
        );
      case 'OFF':
        return (
          <span className="text-[10px] font-bold px-1.5 py-0.5 rounded bg-indigo-100 text-indigo-800 border border-indigo-200">
            Open Food Facts
          </span>
        );
      case 'CUSTOM':
        return (
          <span className="text-[10px] font-bold px-1.5 py-0.5 rounded bg-amber-100 text-amber-800 border border-amber-200">
            Custom
          </span>
        );
    }
  };

  return (
    <div className="space-y-3.5 pb-20">
      {/* Search Header & Tabs */}
      <div className="bg-white p-3.5 rounded-2xl shadow-sm border border-slate-200 space-y-3">
        {/* Navigation Tabs */}
        <div className="flex bg-slate-100 p-1 rounded-xl">
          <button
            id="tab-search-database"
            onClick={() => setActiveTab('search')}
            className={`flex-1 py-1.5 px-2 rounded-lg text-xs font-bold transition flex items-center justify-center space-x-1.5 ${
              activeTab === 'search'
                ? 'bg-white text-slate-900 shadow-sm'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            <Search className="w-3.5 h-3.5 text-teal-600" />
            <span>Search Database</span>
          </button>

          <button
            id="tab-frequent-foods"
            onClick={() => setActiveTab('frequent')}
            className={`flex-1 py-1.5 px-2 rounded-lg text-xs font-bold transition flex items-center justify-center space-x-1.5 ${
              activeTab === 'frequent'
                ? 'bg-white text-slate-900 shadow-sm'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            <History className="w-3.5 h-3.5 text-indigo-600" />
            <span>Frequently Used</span>
          </button>

          <button
            id="tab-custom-foods"
            onClick={() => setActiveTab('custom')}
            className={`flex-1 py-1.5 px-2 rounded-lg text-xs font-bold transition flex items-center justify-center space-x-1.5 ${
              activeTab === 'custom'
                ? 'bg-white text-slate-900 shadow-sm'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            <Utensils className="w-3.5 h-3.5 text-amber-600" />
            <span>Custom ({customFoods.length})</span>
          </button>
        </div>

        {/* Search Bar */}
        {activeTab === 'search' && (
          <div className="space-y-2">
            <div className="relative">
              <Search className="w-4 h-4 text-slate-400 absolute left-3 top-3 pointer-events-none" />
              <input
                type="text"
                id="food-search-input"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search Indian foods, dal, roti, protein powder..."
                className="w-full bg-slate-50 border border-slate-300 rounded-xl pl-9 pr-8 py-2 text-sm text-slate-900 font-semibold focus:outline-none focus:ring-2 focus:ring-teal-500"
              />
              {searchQuery && (
                <button
                  onClick={() => setSearchQuery('')}
                  className="absolute right-2.5 top-2.5 text-xs text-slate-400 hover:text-slate-600 px-1.5 py-0.5 rounded-full"
                >
                  ✕
                </button>
              )}
            </div>

            {/* Source Filter Pills */}
            <div className="flex items-center space-x-1.5 overflow-x-auto py-0.5">
              {(['ALL', 'NIN', 'OFF', 'CUSTOM'] as ('ALL' | FoodSource)[]).map((src) => (
                <button
                  key={src}
                  onClick={() => setSourceFilter(src)}
                  className={`text-[11px] px-2.5 py-0.8 rounded-lg font-semibold whitespace-nowrap transition border ${
                    sourceFilter === src
                      ? 'bg-slate-900 text-white border-slate-900'
                      : 'bg-slate-50 text-slate-600 border-slate-200 hover:bg-slate-100'
                  }`}
                >
                  {src === 'ALL'
                    ? 'All Sources'
                    : src === 'NIN'
                    ? 'NIN / IFCT'
                    : src === 'OFF'
                    ? 'Open Food Facts'
                    : 'Custom Foods'}
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Create Custom Food Button Banner */}
        <div className="flex items-center justify-between pt-1">
          <span className="text-[11px] text-slate-500 font-medium">
            Logging for: <span className="font-bold text-slate-800">{selectedDate}</span>
          </span>
          <button
            id="open-create-custom-food-btn"
            onClick={() => onOpenCustomFoodModal()}
            className="flex items-center space-x-1 text-xs font-bold text-amber-700 bg-amber-50 hover:bg-amber-100 border border-amber-200 px-2.5 py-1 rounded-lg transition"
          >
            <Plus className="w-3.5 h-3.5" />
            <span>New Custom Recipe</span>
          </button>
        </div>
      </div>

      {/* Live OFF Query Loading / Status Indicator */}
      {offLoading && (
        <div className="flex items-center justify-center space-x-2 py-2 text-xs font-semibold text-teal-700 bg-teal-50 rounded-xl border border-teal-100 animate-pulse">
          <Loader2 className="w-3.5 h-3.5 animate-spin" />
          <span>Searching Open Food Facts &amp; packaged database...</span>
        </div>
      )}

      {offError && combinedSearchResults.length === 0 && (
        <div className="text-xs text-amber-800 bg-amber-50 p-2.5 rounded-xl border border-amber-200 flex items-center space-x-2">
          <Info className="w-4 h-4 flex-shrink-0" />
          <span>{offError}</span>
        </div>
      )}

      {/* Content based on Active Tab */}
      {activeTab === 'search' && (
        <div className="space-y-2">
          <div className="flex items-center justify-between px-1">
            <span className="text-xs font-extrabold text-slate-700 uppercase tracking-wider">
              Search Results ({combinedSearchResults.length})
            </span>
            <span className="text-[11px] text-slate-500">Tap to select portion</span>
          </div>

          {combinedSearchResults.length === 0 ? (
            <div className="bg-white rounded-2xl p-8 text-center border border-slate-200 space-y-3">
              <div className="w-12 h-12 rounded-full bg-slate-100 text-slate-400 flex items-center justify-center mx-auto">
                <Search className="w-6 h-6" />
              </div>
              <p className="text-sm font-bold text-slate-800">No matching foods found</p>
              <p className="text-xs text-slate-500 max-w-xs mx-auto">
                Try searching for Indian dishes like "Roti", "Dal", "Paneer", "Rice" or create a custom recipe.
              </p>
              <button
                onClick={() => onOpenCustomFoodModal()}
                className="bg-teal-600 text-white text-xs font-bold px-4 py-2 rounded-xl hover:bg-teal-700 transition"
              >
                + Create Custom Food
              </button>
            </div>
          ) : (
            <div className="space-y-2">
              {combinedSearchResults.map((food) => (
                <div
                  key={food.id}
                  id={`food-item-${food.id}`}
                  onClick={() => onSelectFood(food)}
                  className="bg-white hover:bg-teal-50/40 p-3 rounded-2xl border border-slate-200 shadow-xs hover:shadow-sm hover:border-teal-300 transition cursor-pointer flex items-center justify-between group"
                >
                  <div className="pr-3 flex-1 min-w-0">
                    <div className="flex items-center space-x-1.5 mb-1">
                      {getSourceBadge(food.source)}
                      {food.brand && (
                        <span className="text-[11px] text-slate-500 font-medium truncate max-w-[120px]">
                          {food.brand}
                        </span>
                      )}
                      {food.category && (
                        <span className="text-[10px] text-slate-400 font-medium hidden sm:inline">
                          • {food.category}
                        </span>
                      )}
                    </div>
                    <div className="text-sm font-bold text-slate-900 group-hover:text-teal-900 truncate">
                      {food.name}
                    </div>
                    <div className="text-xs text-slate-500 mt-0.5 flex items-center space-x-2">
                      <span className="font-semibold text-slate-700">
                        {food.kcal_per_100g} kcal
                      </span>
                      <span>•</span>
                      <span className="font-bold text-emerald-700">
                        {food.protein_per_100g}g protein
                      </span>
                      <span>•</span>
                      <span>{food.carbs_per_100g}g C</span>
                      <span>•</span>
                      <span>{food.fat_per_100g}g F</span>
                    </div>
                  </div>

                  <div className="flex flex-col items-end justify-center pl-2 border-l border-slate-100">
                    <span className="text-[10px] font-semibold text-slate-400">per 100g</span>
                    <span className="text-xs font-bold text-teal-700 bg-teal-50 px-2 py-1 rounded-lg group-hover:bg-teal-600 group-hover:text-white transition mt-1">
                      Log +
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Frequently Used Tab */}
      {activeTab === 'frequent' && (
        <div className="space-y-2">
          <div className="flex items-center justify-between px-1">
            <span className="text-xs font-extrabold text-slate-700 uppercase tracking-wider">
              Frequently Logged Foods
            </span>
            <span className="text-[11px] text-slate-500">Based on your log history</span>
          </div>

          <div className="space-y-2">
            {frequentFoods.map((food) => (
              <div
                key={food.id}
                onClick={() => onSelectFood(food)}
                className="bg-white hover:bg-indigo-50/40 p-3 rounded-2xl border border-slate-200 shadow-xs hover:border-indigo-300 transition cursor-pointer flex items-center justify-between"
              >
                <div className="pr-3 flex-1 min-w-0">
                  <div className="flex items-center space-x-1.5 mb-1">
                    {getSourceBadge(food.source)}
                    {food.typical_serving_description && (
                      <span className="text-[11px] text-teal-700 font-semibold">
                        {food.typical_serving_description}
                      </span>
                    )}
                  </div>
                  <div className="text-sm font-bold text-slate-900 truncate">{food.name}</div>
                  <div className="text-xs text-slate-500 mt-0.5">
                    {food.kcal_per_100g} kcal • <span className="font-bold text-emerald-700">{food.protein_per_100g}g protein</span> / 100g
                  </div>
                </div>

                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    onSelectFood(food);
                  }}
                  className="bg-indigo-50 text-indigo-700 hover:bg-indigo-600 hover:text-white px-3 py-1.5 rounded-xl text-xs font-bold transition"
                >
                  Log
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Custom Foods Tab */}
      {activeTab === 'custom' && (
        <div className="space-y-3">
          <div className="flex items-center justify-between px-1">
            <span className="text-xs font-extrabold text-slate-700 uppercase tracking-wider">
              Your Custom Recipes &amp; Foods ({customFoods.length})
            </span>
            <button
              onClick={() => onOpenCustomFoodModal()}
              className="text-xs font-bold text-teal-700 bg-teal-50 px-2.5 py-1 rounded-lg border border-teal-200 hover:bg-teal-100"
            >
              + Add New
            </button>
          </div>

          {customFoods.length === 0 ? (
            <div className="bg-white rounded-2xl p-8 text-center border border-slate-200 space-y-3">
              <div className="w-12 h-12 rounded-full bg-amber-50 text-amber-500 flex items-center justify-center mx-auto">
                <Utensils className="w-6 h-6" />
              </div>
              <p className="text-sm font-bold text-slate-800">No Custom Foods Created</p>
              <p className="text-xs text-slate-500 max-w-xs mx-auto">
                Save your home recipes (e.g. your specific paneer bhurji or protein shakes) with customized macros.
              </p>
              <button
                onClick={() => onOpenCustomFoodModal()}
                className="bg-amber-600 text-white text-xs font-bold px-4 py-2 rounded-xl hover:bg-amber-700 transition"
              >
                + Create First Custom Food
              </button>
            </div>
          ) : (
            <div className="space-y-2">
              {customFoods.map((custom) => (
                <div
                  key={custom.id}
                  className="bg-white p-3 rounded-2xl border border-slate-200 shadow-xs flex items-center justify-between"
                >
                  <div
                    className="pr-2 flex-1 cursor-pointer"
                    onClick={() => {
                      onSelectFood({
                        id: custom.id,
                        name: custom.name,
                        source: 'CUSTOM',
                        kcal_per_100g: custom.kcal_per_100g,
                        protein_per_100g: custom.protein_per_100g,
                        carbs_per_100g: custom.carbs_per_100g,
                        fat_per_100g: custom.fat_per_100g,
                        typical_serving_description: custom.typical_serving_description,
                        typical_serving_grams: custom.typical_serving_grams,
                      });
                    }}
                  >
                    <div className="flex items-center space-x-1.5 mb-0.5">
                      <span className="text-[10px] font-bold px-1.5 py-0.5 rounded bg-amber-100 text-amber-800">
                        Custom
                      </span>
                      {custom.typical_serving_description && (
                        <span className="text-[11px] text-slate-500 font-medium">
                          {custom.typical_serving_description}
                        </span>
                      )}
                    </div>
                    <div className="text-sm font-bold text-slate-900">{custom.name}</div>
                    <div className="text-xs text-slate-500 mt-0.5">
                      {custom.kcal_per_100g} kcal • <span className="font-bold text-emerald-700">{custom.protein_per_100g}g P</span> • {custom.carbs_per_100g}g C • {custom.fat_per_100g}g F
                    </div>
                    {custom.notes && (
                      <div className="text-[11px] text-slate-400 italic mt-0.5 truncate max-w-xs">
                        "{custom.notes}"
                      </div>
                    )}
                  </div>

                  <div className="flex items-center space-x-1 pl-2 border-l border-slate-100">
                    <button
                      onClick={() => onOpenCustomFoodModal(custom)}
                      className="p-1.5 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-lg transition"
                      title="Edit custom food"
                    >
                      <Edit2 className="w-3.5 h-3.5" />
                    </button>
                    <button
                      onClick={() => onDeleteCustomFood(custom.id)}
                      className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition"
                      title="Delete custom food"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                    <button
                      onClick={() => {
                        onSelectFood({
                          id: custom.id,
                          name: custom.name,
                          source: 'CUSTOM',
                          kcal_per_100g: custom.kcal_per_100g,
                          protein_per_100g: custom.protein_per_100g,
                          carbs_per_100g: custom.carbs_per_100g,
                          fat_per_100g: custom.fat_per_100g,
                          typical_serving_description: custom.typical_serving_description,
                          typical_serving_grams: custom.typical_serving_grams,
                        });
                      }}
                      className="ml-1 bg-amber-600 text-white hover:bg-amber-700 text-xs font-bold px-2.5 py-1 rounded-lg transition"
                    >
                      Log
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
};
