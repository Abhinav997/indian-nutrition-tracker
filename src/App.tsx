import React, { useState, useEffect } from 'react';
import { TopBar } from './components/TopBar';
import { Navigation, TabType } from './components/Navigation';
import { AddServingModal } from './components/AddServingModal';
import { LogWeightModal } from './components/LogWeightModal';
import { CustomFoodModal } from './components/CustomFoodModal';
import { HomeScreen } from './screens/HomeScreen';
import { FoodSearchScreen } from './screens/FoodSearchScreen';
import { ProgressScreen } from './screens/ProgressScreen';
import { CalculatorSettingsScreen } from './screens/CalculatorSettingsScreen';
import { CustomFood, DailyLog, FoodMaster, MealType, UserSettings, WaterLog, WeightLog } from './types';
import { db, formatDateKey } from './services/storage';

export default function App() {
  const [activeTab, setActiveTab] = useState<TabType>('today');
  const [selectedDate, setSelectedDate] = useState<string>(formatDateKey(new Date()));
  const [settings, setSettings] = useState<UserSettings>(db.getSettings());
  const [allDailyLogs, setAllDailyLogs] = useState<DailyLog[]>(db.getAllDailyLogs());
  const [weightLogs, setWeightLogs] = useState<WeightLog[]>(db.getAllWeightLogs());
  const [allWaterLogs, setAllWaterLogs] = useState<WaterLog[]>(db.getAllWaterLogs());

  // Modal States
  const [isAddServingOpen, setIsAddServingOpen] = useState(false);
  const [selectedFoodForServing, setSelectedFoodForServing] = useState<FoodMaster | null>(null);
  const [servingMealType, setServingMealType] = useState<MealType>('Lunch');

  const [isLogWeightOpen, setIsLogWeightOpen] = useState(false);
  const [isCustomFoodOpen, setIsCustomFoodOpen] = useState(false);
  const [customFoodToEdit, setCustomFoodToEdit] = useState<CustomFood | null>(null);

  // Sync data whenever logs or settings change
  const refreshAppData = () => {
    setSettings(db.getSettings());
    setAllDailyLogs(db.getAllDailyLogs());
    setWeightLogs(db.getAllWeightLogs());
    setAllWaterLogs(db.getAllWaterLogs());
  };

  useEffect(() => {
    refreshAppData();
  }, []);

  // Filter logs for currently selected date
  const logsForSelectedDate = allDailyLogs.filter((log) => log.date === selectedDate);
  const waterLogsForSelectedDate = allWaterLogs.filter((log) => log.date === selectedDate);

  // Handlers for Food Logging
  const handleOpenFoodSearch = (mealType?: MealType) => {
    if (mealType) {
      setServingMealType(mealType);
    }
    setActiveTab('food');
  };

  const handleSelectFoodForServing = (food: FoodMaster) => {
    setSelectedFoodForServing(food);
    setIsAddServingOpen(true);
  };

  const handleSaveDailyLog = (logData: {
    food_id: string;
    food_name: string;
    source: any;
    serving_grams: number;
    calories: number;
    protein: number;
    carbs: number;
    fat: number;
    meal_type: MealType;
  }) => {
    db.addDailyLog({
      ...logData,
      date: selectedDate,
    });
    refreshAppData();
  };

  const handleDeleteDailyLog = (id: string) => {
    db.deleteDailyLog(id);
    refreshAppData();
  };

  // Handlers for Water Logging
  const handleAddWater = (amountMl: number, time?: string) => {
    db.addWaterLog(selectedDate, amountMl, time);
    refreshAppData();
  };

  const handleDeleteWaterLog = (id: string) => {
    db.deleteWaterLog(id);
    refreshAppData();
  };

  // Handlers for Weight Logging
  const handleSaveWeight = (date: string, weightKg: number, note?: string) => {
    db.addWeightLog(date, weightKg, note);
    refreshAppData();
  };

  const handleDeleteWeightLog = (id: string) => {
    db.deleteWeightLog(id);
    refreshAppData();
  };

  // Handlers for Custom Foods
  const handleOpenCustomFoodModal = (foodToEdit?: CustomFood | null) => {
    setCustomFoodToEdit(foodToEdit ?? null);
    setIsCustomFoodOpen(true);
  };

  const handleSaveCustomFood = (food: Omit<CustomFood, 'id' | 'created_at'>, editId?: string) => {
    if (editId) {
      db.updateCustomFood(editId, food);
    } else {
      db.addCustomFood(food);
    }
    refreshAppData();
  };

  const handleDeleteCustomFood = (id: string) => {
    if (confirm('Delete this custom food?')) {
      db.deleteCustomFood(id);
      refreshAppData();
    }
  };

  // Handlers for Settings & Reset
  const handleSaveSettings = (newSettings: UserSettings) => {
    db.saveSettings(newSettings);
    refreshAppData();
  };

  const handleClearAllLogs = () => {
    db.clearAllLogsToZero();
    refreshAppData();
    setSelectedDate(formatDateKey(new Date()));
    setActiveTab('today');
  };

  return (
    <div className="min-h-screen bg-slate-100 flex flex-col font-sans">
      {/* Mobile Wrapper to keep clean Android-like ergonomic proportions while being fully responsive */}
      <div className="w-full max-w-md mx-auto min-h-screen bg-slate-50 flex flex-col shadow-2xl relative">
        {/* Top App Bar with Date Picker */}
        <TopBar
          selectedDate={selectedDate}
          onDateChange={setSelectedDate}
        />

        {/* Main Content View with Smooth Padding */}
        <main className="flex-1 p-3.5 overflow-y-auto">
          {activeTab === 'today' && (
            <HomeScreen
              selectedDate={selectedDate}
              dailyLogs={logsForSelectedDate}
              weightLogs={weightLogs}
              waterLogs={waterLogsForSelectedDate}
              settings={settings}
              onOpenFoodSearch={handleOpenFoodSearch}
              onOpenLogWeight={() => setIsLogWeightOpen(true)}
              onAddWater={handleAddWater}
              onDeleteWaterLog={handleDeleteWaterLog}
              onDeleteLog={handleDeleteDailyLog}
              onNavigateToTab={setActiveTab}
            />
          )}

          {activeTab === 'food' && (
            <FoodSearchScreen
              selectedDate={selectedDate}
              defaultMealType={servingMealType}
              onSelectFood={handleSelectFoodForServing}
              onOpenCustomFoodModal={handleOpenCustomFoodModal}
              onDeleteCustomFood={handleDeleteCustomFood}
            />
          )}

          {activeTab === 'progress' && (
            <ProgressScreen
              dailyLogs={allDailyLogs}
              weightLogs={weightLogs}
              waterLogs={allWaterLogs}
              settings={settings}
              onOpenLogWeight={() => setIsLogWeightOpen(true)}
              onDeleteWeightLog={handleDeleteWeightLog}
            />
          )}

          {activeTab === 'calculator' && (
            <CalculatorSettingsScreen
              settings={settings}
              onSaveSettings={handleSaveSettings}
              onClearAllLogs={handleClearAllLogs}
            />
          )}
        </main>

        {/* Bottom Navigation */}
        <Navigation
          activeTab={activeTab}
          onTabChange={setActiveTab}
        />

        {/* Dialogs / Modals */}
        <AddServingModal
          isOpen={isAddServingOpen}
          food={selectedFoodForServing}
          selectedDate={selectedDate}
          defaultMealType={servingMealType}
          onClose={() => setIsAddServingOpen(false)}
          onSaveLog={handleSaveDailyLog}
        />

        <LogWeightModal
          isOpen={isLogWeightOpen}
          onClose={() => setIsLogWeightOpen(false)}
          onSaveWeight={handleSaveWeight}
          settings={settings}
          weightLogs={weightLogs}
        />

        <CustomFoodModal
          isOpen={isCustomFoodOpen}
          onClose={() => setIsCustomFoodOpen(false)}
          onSaveCustomFood={handleSaveCustomFood}
          foodToEdit={customFoodToEdit}
        />
      </div>
    </div>
  );
}
