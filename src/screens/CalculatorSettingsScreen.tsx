import React, { useState, useEffect } from 'react';
import {
  Calculator,
  Save,
  Check,
  RotateCcw,
  Download,
  FileSpreadsheet,
  FileJson,
  Info,
  Sparkles,
  Flame,
  Dumbbell,
  Droplets,
  Settings,
  HeartPulse,
} from 'lucide-react';
import {
  ActivityLevel,
  CalculatorResult,
  DateRange,
  GoalType,
  Sex,
  UnitSystem,
  UserSettings,
} from '../types';
import {
  ACTIVITY_MULTIPLIERS,
  PROTEIN_GUIDELINES,
  calculateTargets,
  formatWeight,
  kgToLb,
  lbToKg,
} from '../utils/calculator';
import { db } from '../services/storage';

interface CalculatorSettingsScreenProps {
  settings: UserSettings;
  onSaveSettings: (newSettings: UserSettings) => void;
  onClearAllLogs?: () => void;
}

export const CalculatorSettingsScreen: React.FC<CalculatorSettingsScreenProps> = ({
  settings,
  onSaveSettings,
  onClearAllLogs,
}) => {
  // Form State
  const [currentWeight, setCurrentWeight] = useState<string>(
    settings.unit_system === 'lb'
      ? kgToLb(settings.current_weight_kg).toString()
      : settings.current_weight_kg.toString()
  );
  const [targetWeight, setTargetWeight] = useState<string>(
    settings.unit_system === 'lb'
      ? kgToLb(settings.target_weight_kg).toString()
      : settings.target_weight_kg.toString()
  );
  const [heightCm, setHeightCm] = useState<number>(settings.height_cm);
  const [ageYears, setAgeYears] = useState<number>(settings.age_years);
  const [sex, setSex] = useState<Sex>(settings.sex);
  const [activityLevel, setActivityLevel] = useState<ActivityLevel>(settings.activity_level);
  const [goalType, setGoalType] = useState<GoalType>(settings.goal_type);
  const [goalRate, setGoalRate] = useState<number>(settings.goal_rate_kg_per_week);
  const [proteinBasis, setProteinBasis] = useState<'current' | 'target'>(settings.protein_basis || 'current');
  const [unitSystem, setUnitSystem] = useState<UnitSystem>(settings.unit_system);
  const [defaultChartRange, setDefaultChartRange] = useState<DateRange>(settings.default_chart_range);
  const [exportDays, setExportDays] = useState<number>(30);

  // Target overrides (allows custom values or pre-defined auto formula values)
  const [customCalorieTarget, setCustomCalorieTarget] = useState<number>(settings.daily_calorie_target);
  const [customProteinTarget, setCustomProteinTarget] = useState<number>(settings.daily_protein_target);
  const [customWaterTarget, setCustomWaterTarget] = useState<number>(settings.daily_water_target_ml || 2500);
  const [isCustomOverride, setIsCustomOverride] = useState<boolean>(false);

  const [savedSuccess, setSavedSuccess] = useState(false);
  const [calcResult, setCalcResult] = useState<CalculatorResult | null>(null);

  // Convert inputs to kg for math
  const getKg = (val: string) => {
    const num = parseFloat(val) || 0;
    return unitSystem === 'lb' ? lbToKg(num) : num;
  };

  const recompute = () => {
    const cKg = getKg(currentWeight);
    const tKg = getKg(targetWeight);
    if (cKg > 0 && heightCm > 0 && ageYears > 0) {
      const res = calculateTargets({
        current_weight_kg: cKg,
        target_weight_kg: tKg,
        height_cm: heightCm,
        age_years: ageYears,
        sex,
        activity_level: activityLevel,
        goal_type: goalType,
        goal_rate_kg_per_week: goalRate,
        protein_basis: proteinBasis,
      });
      setCalcResult(res);
      // If user is not actively locking custom manual targets, keep in sync with scientific formula
      if (!isCustomOverride) {
        setCustomCalorieTarget(res.recommendedCalories);
        setCustomProteinTarget(res.recommendedProtein);
        setCustomWaterTarget(res.recommendedWaterMl);
      }
    }
  };

  useEffect(() => {
    recompute();
  }, [currentWeight, targetWeight, heightCm, ageYears, sex, activityLevel, goalType, goalRate, proteinBasis, unitSystem]);

  const handleUnitToggle = (newUnit: UnitSystem) => {
    if (newUnit === unitSystem) return;
    const cKg = getKg(currentWeight);
    const tKg = getKg(targetWeight);
    setUnitSystem(newUnit);
    if (newUnit === 'lb') {
      setCurrentWeight(kgToLb(cKg).toString());
      setTargetWeight(kgToLb(tKg).toString());
    } else {
      setCurrentWeight(cKg.toString());
      setTargetWeight(tKg.toString());
    }
  };

  const handleSaveAndApply = (e: React.FormEvent) => {
    e.preventDefault();
    const cKg = getKg(currentWeight);
    const tKg = getKg(targetWeight);

    if (!calcResult) return;

    const finalCalories = isCustomOverride ? customCalorieTarget : calcResult.recommendedCalories;
    const finalProtein = isCustomOverride ? customProteinTarget : calcResult.recommendedProtein;
    const finalWater = isCustomOverride ? customWaterTarget : calcResult.recommendedWaterMl;

    const newSettings: UserSettings = {
      current_weight_kg: cKg,
      target_weight_kg: tKg,
      height_cm: heightCm,
      age_years: ageYears,
      sex,
      activity_level: activityLevel,
      goal_type: goalType,
      goal_rate_kg_per_week: goalRate,
      daily_calorie_target: finalCalories,
      daily_protein_target: finalProtein,
      daily_water_target_ml: finalWater,
      protein_basis: proteinBasis,
      unit_system: unitSystem,
      default_chart_range: defaultChartRange,
    };

    onSaveSettings(newSettings);
    setSavedSuccess(true);
    setTimeout(() => setSavedSuccess(false), 3000);
  };

  const handleExportCSV = () => {
    const csv = db.exportDataCSV(exportDays);
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.setAttribute('href', url);
    const suffix = exportDays > 0 ? `_last_${exportDays}_days` : '_all';
    link.setAttribute('download', `nutrition_logs${suffix}_${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const handleExportJSON = () => {
    const json = db.exportDataJSON();
    const blob = new Blob([json], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.setAttribute('href', url);
    link.setAttribute('download', `nutrition_weight_backup_${new Date().toISOString().slice(0, 10)}.json`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="space-y-4 pb-20">
      {/* Header Banner */}
      <div className="bg-slate-900 text-white p-4 rounded-2xl shadow-sm flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <div className="p-2.5 bg-teal-500/20 text-teal-400 rounded-xl">
            <Calculator className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-base font-bold">Target Calculator &amp; Profile</h2>
            <p className="text-xs text-slate-400">
              Mifflin–St Jeor BMR, TDEE, &amp; Macronutrient Estimator
            </p>
          </div>
        </div>
      </div>

      {/* Main Profile & Calculator Form */}
      <form onSubmit={handleSaveAndApply} className="space-y-4">
        <div className="bg-white rounded-2xl p-4 shadow-sm border border-slate-200 space-y-4">
          <div className="flex items-center justify-between border-b border-slate-100 pb-2">
            <h3 className="text-xs font-extrabold text-slate-800 uppercase tracking-wider">
              1. Physical Profile &amp; Biometrics
            </h3>
            {/* Units Toggle */}
            <div className="flex items-center space-x-1 bg-slate-100 p-0.5 rounded-lg border border-slate-200">
              <button
                type="button"
                id="unit-system-kg-btn"
                onClick={() => handleUnitToggle('kg')}
                className={`px-2.5 py-0.5 text-xs font-bold rounded-md transition ${
                  unitSystem === 'kg' ? 'bg-teal-600 text-white' : 'text-slate-600'
                }`}
              >
                kg
              </button>
              <button
                type="button"
                id="unit-system-lb-btn"
                onClick={() => handleUnitToggle('lb')}
                className={`px-2.5 py-0.5 text-xs font-bold rounded-md transition ${
                  unitSystem === 'lb' ? 'bg-teal-600 text-white' : 'text-slate-600'
                }`}
              >
                lb
              </button>
            </div>
          </div>

          {/* Current & Target Weight */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Current Weight ({unitSystem})
              </label>
              <input
                type="number"
                id="profile-current-weight"
                step="0.1"
                min="25"
                max="400"
                value={currentWeight}
                onChange={(e) => setCurrentWeight(e.target.value)}
                required
                className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3 py-2 text-sm font-bold text-slate-900 focus:ring-2 focus:ring-teal-500 focus:outline-none"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Target Weight ({unitSystem})
              </label>
              <input
                type="number"
                id="profile-target-weight"
                step="0.1"
                min="25"
                max="400"
                value={targetWeight}
                onChange={(e) => setTargetWeight(e.target.value)}
                required
                className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3 py-2 text-sm font-bold text-slate-900 focus:ring-2 focus:ring-teal-500 focus:outline-none"
              />
            </div>
          </div>

          {/* Height, Age, Sex */}
          <div className="grid grid-cols-3 gap-2">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Height (cm)
              </label>
              <input
                type="number"
                id="profile-height-cm"
                min="100"
                max="250"
                value={heightCm}
                onChange={(e) => setHeightCm(Number(e.target.value))}
                required
                className="w-full bg-slate-50 border border-slate-300 rounded-xl px-2.5 py-2 text-sm font-bold text-slate-900 focus:ring-2 focus:ring-teal-500 focus:outline-none"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Age (years)
              </label>
              <input
                type="number"
                id="profile-age-years"
                min="12"
                max="100"
                value={ageYears}
                onChange={(e) => setAgeYears(Number(e.target.value))}
                required
                className="w-full bg-slate-50 border border-slate-300 rounded-xl px-2.5 py-2 text-sm font-bold text-slate-900 focus:ring-2 focus:ring-teal-500 focus:outline-none"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Biological Sex
              </label>
              <select
                id="profile-sex-select"
                value={sex}
                onChange={(e) => setSex(e.target.value as Sex)}
                className="w-full bg-slate-50 border border-slate-300 rounded-xl px-2 py-2 text-sm font-bold text-slate-900 focus:ring-2 focus:ring-teal-500 focus:outline-none"
              >
                <option value="M">Male (+5)</option>
                <option value="F">Female (-161)</option>
                <option value="Other">Other (-78)</option>
              </select>
            </div>
          </div>

          {/* Activity Level */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Activity Level (TDEE Multiplier)
            </label>
            <select
              id="profile-activity-level-select"
              value={activityLevel}
              onChange={(e) => setActivityLevel(e.target.value as ActivityLevel)}
              className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3 py-2 text-xs font-bold text-slate-900 focus:ring-2 focus:ring-teal-500 focus:outline-none"
            >
              {(Object.keys(ACTIVITY_MULTIPLIERS) as ActivityLevel[]).map((level) => (
                <option key={level} value={level}>
                  {level} (×{ACTIVITY_MULTIPLIERS[level].factor}) – {ACTIVITY_MULTIPLIERS[level].desc}
                </option>
              ))}
            </select>
          </div>

          {/* Goal Type & Rate */}
          <div className="grid grid-cols-2 gap-3 pt-1">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Goal Strategy
              </label>
              <div className="grid grid-cols-3 gap-1">
                {(['Lose', 'Maintain', 'Gain'] as GoalType[]).map((type) => (
                  <button
                    type="button"
                    key={type}
                    id={`goal-type-btn-${type.toLowerCase()}`}
                    onClick={() => {
                      setGoalType(type);
                      if (type === 'Maintain') setGoalRate(0);
                      else if (type === 'Lose' && goalRate >= 0) setGoalRate(-0.5);
                      else if (type === 'Gain' && goalRate <= 0) setGoalRate(0.25);
                    }}
                    className={`py-1.5 text-xs font-bold rounded-lg border text-center transition ${
                      goalType === type
                        ? 'bg-slate-900 text-white border-slate-900'
                        : 'bg-slate-50 text-slate-700 border-slate-200 hover:bg-slate-100'
                    }`}
                  >
                    {type}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Rate of Change ({unitSystem}/wk)
              </label>
              <select
                id="profile-goal-rate-select"
                value={goalRate}
                disabled={goalType === 'Maintain'}
                onChange={(e) => setGoalRate(Number(e.target.value))}
                className="w-full bg-slate-50 border border-slate-300 rounded-xl px-2.5 py-2 text-xs font-bold text-slate-900 focus:ring-2 focus:ring-teal-500 focus:outline-none disabled:opacity-50"
              >
                {goalType === 'Lose' ? (
                  <>
                    <option value="-0.25">~0.25 kg/wk (Mild deficit ~250 kcal)</option>
                    <option value="-0.50">~0.50 kg/wk (Standard deficit ~500 kcal)</option>
                    <option value="-0.75">~0.75 kg/wk (Aggressive deficit ~750 kcal)</option>
                  </>
                ) : goalType === 'Gain' ? (
                  <>
                    <option value="0.25">~0.25 kg/wk (Lean surplus ~250 kcal)</option>
                    <option value="0.50">~0.50 kg/wk (Moderate surplus ~500 kcal)</option>
                  </>
                ) : (
                  <option value="0">0 kg/wk (Equilibrium)</option>
                )}
              </select>
            </div>
          </div>

          {/* Protein Calculation Basis */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Protein Target Basis
            </label>
            <div className="flex items-center space-x-3 text-xs">
              <label className="flex items-center space-x-1.5 cursor-pointer">
                <input
                  type="radio"
                  name="proteinBasis"
                  checked={proteinBasis === 'current'}
                  onChange={() => setProteinBasis('current')}
                  className="text-teal-600 focus:ring-teal-500"
                />
                <span className="font-semibold text-slate-800">
                  Current Weight ({formatWeight(getKg(currentWeight), unitSystem)})
                </span>
              </label>
              <label className="flex items-center space-x-1.5 cursor-pointer">
                <input
                  type="radio"
                  name="proteinBasis"
                  checked={proteinBasis === 'target'}
                  onChange={() => setProteinBasis('target')}
                  className="text-teal-600 focus:ring-teal-500"
                />
                <span className="font-semibold text-slate-800">
                  Target Weight ({formatWeight(getKg(targetWeight), unitSystem)})
                </span>
              </label>
            </div>
          </div>
        </div>

        {/* Calculated Results Display Section */}
        {calcResult && (
          <div className="bg-gradient-to-br from-teal-50 via-emerald-50 to-cyan-50 rounded-2xl p-4 border border-teal-200 space-y-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-1.5">
                <Sparkles className="w-4 h-4 text-teal-700" />
                <h3 className="text-xs font-extrabold text-teal-900 uppercase tracking-wider">
                  2. Calorie, Protein &amp; Water Goals
                </h3>
              </div>
              <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-teal-200/60 text-teal-900">
                Scientific &amp; Custom
              </span>
            </div>

            {/* Mode Switcher: Pre-defined Scientific Formula vs Custom Manual Target */}
            <div className="flex bg-teal-200/50 p-1 rounded-xl">
              <button
                type="button"
                id="target-mode-predefined-btn"
                onClick={() => {
                  setIsCustomOverride(false);
                  setCustomCalorieTarget(calcResult.recommendedCalories);
                  setCustomProteinTarget(calcResult.recommendedProtein);
                  setCustomWaterTarget(calcResult.recommendedWaterMl);
                }}
                className={`flex-1 py-1.5 px-2 rounded-lg text-xs font-bold transition flex items-center justify-center space-x-1.5 ${
                  !isCustomOverride
                    ? 'bg-white text-teal-950 shadow-xs'
                    : 'text-teal-800 hover:text-teal-950'
                }`}
              >
                <Sparkles className="w-3.5 h-3.5 text-teal-600" />
                <span>Pre-defined (Auto Formula)</span>
              </button>

              <button
                type="button"
                id="target-mode-custom-btn"
                onClick={() => setIsCustomOverride(true)}
                className={`flex-1 py-1.5 px-2 rounded-lg text-xs font-bold transition flex items-center justify-center space-x-1.5 ${
                  isCustomOverride
                    ? 'bg-white text-teal-950 shadow-xs'
                    : 'text-teal-800 hover:text-teal-950'
                }`}
              >
                <Settings className="w-3.5 h-3.5 text-teal-600" />
                <span>Custom Manual Targets</span>
              </button>
            </div>

            {/* Target Display or Custom Target Input Fields */}
            {!isCustomOverride ? (
              /* Pre-defined Scientific Target Cards */
              <div className="grid grid-cols-3 gap-2">
                <div className="bg-white p-3 rounded-xl border border-teal-100 shadow-xs text-center">
                  <div className="flex items-center justify-center space-x-1 text-amber-600 mb-1">
                    <Flame className="w-3.5 h-3.5" />
                    <span className="text-[10px] font-bold uppercase tracking-wider">Calories</span>
                  </div>
                  <div className="text-xl font-black text-slate-900">
                    {calcResult.recommendedCalories}
                  </div>
                  <div className="text-[10px] text-slate-500 mt-0.5">
                    kcal/day
                  </div>
                </div>

                <div className="bg-white p-3 rounded-xl border border-teal-100 shadow-xs text-center">
                  <div className="flex items-center justify-center space-x-1 text-emerald-600 mb-1">
                    <Dumbbell className="w-3.5 h-3.5" />
                    <span className="text-[10px] font-bold uppercase tracking-wider">Protein</span>
                  </div>
                  <div className="text-xl font-black text-emerald-700">
                    {calcResult.recommendedProtein}
                  </div>
                  <div className="text-[10px] text-slate-500 mt-0.5">
                    g/day
                  </div>
                </div>

                <div className="bg-white p-3 rounded-xl border border-teal-100 shadow-xs text-center">
                  <div className="flex items-center justify-center space-x-1 text-sky-600 mb-1">
                    <Droplets className="w-3.5 h-3.5" />
                    <span className="text-[10px] font-bold uppercase tracking-wider">Hydration</span>
                  </div>
                  <div className="text-xl font-black text-sky-700">
                    {calcResult.recommendedWaterMl}
                  </div>
                  <div className="text-[10px] text-slate-500 mt-0.5">
                    ml/day (~{Math.round(calcResult.recommendedWaterMl / 250)} glasses)
                  </div>
                </div>
              </div>
            ) : (
              /* Custom User Override Input Fields */
              <div className="bg-white p-3.5 rounded-xl border border-teal-200 space-y-3">
                <div className="text-[11px] font-bold text-teal-900">
                  Custom Daily Target Goals:
                </div>
                <div className="grid grid-cols-3 gap-2">
                  <div>
                    <label className="text-[10px] font-bold text-slate-600 block mb-1">
                      Calories (kcal)
                    </label>
                    <input
                      type="number"
                      min="800"
                      max="6000"
                      step="50"
                      value={customCalorieTarget}
                      onChange={(e) => setCustomCalorieTarget(Number(e.target.value))}
                      className="w-full bg-slate-50 border border-slate-300 rounded-lg p-2 text-xs font-bold text-slate-900 text-center focus:bg-white focus:ring-2 focus:ring-teal-500"
                    />
                  </div>

                  <div>
                    <label className="text-[10px] font-bold text-slate-600 block mb-1">
                      Protein (grams)
                    </label>
                    <input
                      type="number"
                      min="30"
                      max="400"
                      step="5"
                      value={customProteinTarget}
                      onChange={(e) => setCustomProteinTarget(Number(e.target.value))}
                      className="w-full bg-slate-50 border border-slate-300 rounded-lg p-2 text-xs font-bold text-emerald-800 text-center focus:bg-white focus:ring-2 focus:ring-teal-500"
                    />
                  </div>

                  <div>
                    <label className="text-[10px] font-bold text-slate-600 block mb-1">
                      Water (ml)
                    </label>
                    <input
                      type="number"
                      min="1000"
                      max="6000"
                      step="250"
                      value={customWaterTarget}
                      onChange={(e) => setCustomWaterTarget(Number(e.target.value))}
                      className="w-full bg-slate-50 border border-slate-300 rounded-lg p-2 text-xs font-bold text-sky-800 text-center focus:bg-white focus:ring-2 focus:ring-teal-500"
                    />
                  </div>
                </div>
                <p className="text-[10px] text-slate-500 italic">
                  Tip: Auto-formula suggests {calcResult.recommendedCalories} kcal, {calcResult.recommendedProtein}g protein, and {calcResult.recommendedWaterMl}ml water.
                </p>
              </div>
            )}

            {/* Formula Math Breakdown */}
            <div className="bg-white/90 p-3 rounded-xl border border-teal-100 text-xs space-y-1.5 font-mono text-slate-700">
              <div className="text-[11px] font-bold font-sans text-teal-900 mb-1">
                Body Metrics &amp; Formula Math:
              </div>
              <div className="flex justify-between text-[11px]">
                <span className="text-slate-500">BMR (Mifflin–St Jeor):</span>
                <span className="font-semibold text-slate-900">{calcResult.formulaDetails.bmrFormula}</span>
              </div>
              <div className="flex justify-between text-[11px]">
                <span className="text-slate-500">TDEE:</span>
                <span className="font-semibold text-slate-900">{calcResult.formulaDetails.tdeeFormula}</span>
              </div>
              <div className="flex justify-between text-[11px]">
                <span className="text-slate-500">Auto Calorie Goal:</span>
                <span className="font-semibold text-slate-900">{calcResult.formulaDetails.targetFormula}</span>
              </div>
              <div className="flex justify-between text-[11px]">
                <span className="text-slate-500">Auto Protein Goal:</span>
                <span className="font-semibold text-emerald-800">{calcResult.formulaDetails.proteinFormula}</span>
              </div>
              <div className="flex justify-between text-[11px]">
                <span className="text-slate-500">Auto Hydration Goal:</span>
                <span className="font-semibold text-sky-800">{calcResult.formulaDetails.waterFormula}</span>
              </div>
            </div>

            {/* Save & Apply Button */}
            <button
              type="submit"
              id="save-and-use-targets-btn"
              className="w-full py-3 bg-teal-600 hover:bg-teal-700 text-white font-bold text-sm rounded-xl shadow-md hover:shadow-lg transition flex items-center justify-center space-x-2"
            >
              {savedSuccess ? (
                <>
                  <Check className="w-5 h-5 text-white" />
                  <span>Saved &amp; Applied to Dashboard!</span>
                </>
              ) : (
                <>
                  <Save className="w-4 h-4" />
                  <span>Save &amp; Use These Targets</span>
                </>
              )}
            </button>
          </div>
        )}
      </form>

      {/* Settings & Data Management Section */}
      <div className="bg-white rounded-2xl p-4 shadow-sm border border-slate-200 space-y-4">
        <div className="flex items-center space-x-2 border-b border-slate-100 pb-2">
          <Settings className="w-4 h-4 text-slate-600" />
          <h3 className="text-xs font-extrabold text-slate-800 uppercase tracking-wider">
            Data Management &amp; Export
          </h3>
        </div>

        <div className="space-y-3">
          <div className="flex items-center space-x-3">
            <div className="flex-1">
              <label className="block text-[10px] font-bold text-slate-500 uppercase tracking-tight mb-1">
                Days to Export (0 for all)
              </label>
              <input
                type="number"
                min="0"
                max="3650"
                value={exportDays}
                onChange={(e) => setExportDays(Math.max(0, Number(e.target.value)))}
                className="w-full bg-slate-50 border border-slate-200 rounded-xl px-3 py-2 text-sm font-bold text-slate-900 focus:ring-2 focus:ring-teal-500 focus:outline-none"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-2">
            <button
              type="button"
              id="export-csv-btn"
              onClick={handleExportCSV}
              className="flex items-center justify-center space-x-2 p-2.5 rounded-xl border border-slate-200 bg-slate-50 hover:bg-slate-100 text-slate-800 text-xs font-bold transition"
            >
              <FileSpreadsheet className="w-4 h-4 text-emerald-600" />
              <span>Export CSV</span>
            </button>

            <button
              type="button"
              id="export-json-btn"
              onClick={handleExportJSON}
              className="flex items-center justify-center space-x-2 p-2.5 rounded-xl border border-slate-200 bg-slate-50 hover:bg-slate-100 text-slate-800 text-xs font-bold transition"
            >
              <FileJson className="w-4 h-4 text-indigo-600" />
              <span>Backup JSON</span>
            </button>
          </div>
        </div>

        <div className="pt-2 border-t border-slate-100 space-y-3">
          <div className="flex items-center justify-between">
            <div>
              <div className="text-xs font-bold text-slate-800">Clear All Logs (Reset to 0)</div>
              <div className="text-[11px] text-slate-500">
                Wipes all food entries, weight history, and water logs to start with 0
              </div>
            </div>
            <button
              type="button"
              id="clear-all-logs-zero-btn"
              onClick={() => {
                if (confirm('Clear all logged food, water, and weight data to 0? Your profile targets will be kept.')) {
                  if (onClearAllLogs) {
                    onClearAllLogs();
                  } else {
                    db.clearAllLogsToZero();
                  }
                  alert('All logged data reset to 0.');
                }
              }}
              className="flex items-center space-x-1.5 text-xs font-bold text-amber-700 bg-amber-50 hover:bg-amber-100 border border-amber-200 px-3 py-1.5 rounded-xl transition"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              <span>Reset to 0</span>
            </button>
          </div>
        </div>
      </div>

      {/* About & Attribution Section (Section 5.4 & 8) */}
      <div className="bg-slate-50 rounded-2xl p-4 border border-slate-200 space-y-2.5 text-xs text-slate-600">
        <div className="flex items-center space-x-1.5 text-slate-800 font-bold">
          <Info className="w-4 h-4 text-teal-600" />
          <span>About &amp; Data Attributions</span>
        </div>
        <p className="leading-relaxed">
          <strong className="text-slate-800">Indian Food Composition Database:</strong> Derived from Indian Food Composition Tables (IFCT 2017) published by the <em>National Institute of Nutrition (NIN), Indian Council of Medical Research (ICMR)</em>.
        </p>
        <p className="leading-relaxed">
          <strong className="text-slate-800">Branded Products:</strong> Branded product data provided by <a href="https://world.openfoodfacts.org" target="_blank" rel="noreferrer" className="text-teal-700 font-semibold underline">Open Food Facts</a> under Open Database License (ODbL).
        </p>
        <p className="leading-relaxed text-[11px] text-slate-500">
          All calculations and user logs remain 100% private and stored locally on your device in your browser's persistent database.
        </p>
      </div>
    </div>
  );
};
