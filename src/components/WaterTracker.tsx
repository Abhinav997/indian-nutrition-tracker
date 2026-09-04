import React, { useState } from 'react';
import {
  Droplets,
  Plus,
  Minus,
  Trash2,
  CheckCircle2,
  Clock,
  Sparkles,
  CupSoda,
  GlassWater,
  RotateCcw,
} from 'lucide-react';
import { UserSettings, WaterLog } from '../types';

interface WaterTrackerProps {
  selectedDate: string;
  waterLogs: WaterLog[];
  settings: UserSettings;
  onAddWater: (amountMl: number, time?: string) => void;
  onDeleteWaterLog: (id: string) => void;
}

export const WaterTracker: React.FC<WaterTrackerProps> = ({
  selectedDate,
  waterLogs,
  settings,
  onAddWater,
  onDeleteWaterLog,
}) => {
  const [showCustomInput, setShowCustomInput] = useState(false);
  const [customAmount, setCustomAmount] = useState('250');
  const [showHistory, setShowHistory] = useState(false);

  // Total water for the selected date
  const totalWaterMl = waterLogs.reduce((acc, curr) => acc + curr.amount_ml, 0);
  const waterTargetMl = settings.daily_water_target_ml || 2500;
  const progressPercent = Math.min(100, Math.round((totalWaterMl / waterTargetMl) * 100));
  const remainingMl = waterTargetMl - totalWaterMl;
  
  // Standard glass is 250ml
  const glassesLogged = Math.round((totalWaterMl / 250) * 10) / 10;
  const targetGlasses = Math.round(waterTargetMl / 250);

  const handleQuickAdd = (ml: number) => {
    onAddWater(ml);
  };

  const handleCustomSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const amount = parseInt(customAmount, 10);
    if (!isNaN(amount) && amount > 0) {
      onAddWater(amount);
      setShowCustomInput(false);
      setCustomAmount('250');
    }
  };

  return (
    <div
      id="water-tracker-widget"
      className="bg-white rounded-2xl p-4 shadow-sm border border-slate-200 space-y-3.5 relative overflow-hidden"
    >
      {/* Background soft ambient tint */}
      <div className="absolute top-0 right-0 w-32 h-32 bg-sky-50 rounded-full blur-2xl -mr-10 -mt-10 pointer-events-none" />

      {/* Header */}
      <div className="flex items-center justify-between relative z-10">
        <div className="flex items-center space-x-2">
          <div className="p-2 bg-sky-100 text-sky-600 rounded-xl">
            <Droplets className="w-5 h-5 fill-sky-500" />
          </div>
          <div>
            <h3 className="text-sm font-extrabold text-slate-900 tracking-tight flex items-center space-x-1.5">
              <span>Water &amp; Hydration</span>
              {totalWaterMl >= waterTargetMl && (
                <span className="bg-sky-100 text-sky-800 text-[10px] font-bold px-2 py-0.5 rounded-full flex items-center space-x-1">
                  <CheckCircle2 className="w-3 h-3 text-sky-600" />
                  <span>Target Met!</span>
                </span>
              )}
            </h3>
            <p className="text-xs text-slate-500">
              {glassesLogged} of {targetGlasses} glasses ({totalWaterMl} / {waterTargetMl} ml)
            </p>
          </div>
        </div>

        <button
          id="toggle-water-history-btn"
          onClick={() => setShowHistory(!showHistory)}
          className="text-xs font-semibold text-sky-700 hover:text-sky-900 bg-sky-50 hover:bg-sky-100 px-2.5 py-1.5 rounded-lg transition"
        >
          {showHistory ? 'Hide Logs' : `Logs (${waterLogs.length})`}
        </button>
      </div>

      {/* Progress Gauge */}
      <div className="space-y-1.5 relative z-10">
        <div className="w-full bg-slate-100 rounded-full h-3.5 p-0.5 border border-slate-200/80 overflow-hidden shadow-inner">
          <div
            className={`h-full rounded-full transition-all duration-500 ${
              totalWaterMl >= waterTargetMl
                ? 'bg-gradient-to-r from-sky-400 via-cyan-400 to-teal-400'
                : 'bg-gradient-to-r from-sky-500 to-blue-500'
            }`}
            style={{ width: `${progressPercent}%` }}
          />
        </div>

        <div className="flex items-center justify-between text-[11px] font-semibold text-slate-500 px-0.5">
          <span>{progressPercent}% of daily goal</span>
          <span>
            {remainingMl > 0 ? (
              <span className="text-sky-700 font-bold">{remainingMl} ml remaining</span>
            ) : (
              <span className="text-teal-700 font-bold">
                +{Math.abs(remainingMl)} ml over target
              </span>
            )}
          </span>
        </div>
      </div>

      {/* Visual Glass Icons Representation (Up to 10-12 glasses) */}
      <div className="flex items-center justify-between bg-slate-50/90 p-2.5 rounded-xl border border-slate-100">
        <div className="flex flex-wrap gap-1.5 flex-1 items-center">
          {Array.from({ length: Math.max(targetGlasses, Math.ceil(totalWaterMl / 250)) }).map(
            (_, idx) => {
              const isFilled = idx < Math.floor(totalWaterMl / 250);
              const isPartial =
                !isFilled && idx === Math.floor(totalWaterMl / 250) && totalWaterMl % 250 > 0;

              return (
                <div
                  key={idx}
                  title={`Glass ${idx + 1} (250ml)`}
                  className={`w-6 h-7 rounded-md flex items-end justify-center p-0.5 transition border ${
                    isFilled
                      ? 'bg-sky-500 border-sky-600 text-white shadow-xs'
                      : isPartial
                      ? 'bg-sky-100 border-sky-300'
                      : 'bg-white border-slate-200 opacity-60'
                  }`}
                >
                  <span className="text-[8px] font-black leading-none mb-0.5">
                    {isFilled ? '✓' : idx + 1}
                  </span>
                </div>
              );
            }
          )}
        </div>
      </div>

      {/* Quick Add Action Buttons */}
      <div className="space-y-2 relative z-10">
        <div className="grid grid-cols-4 gap-2">
          <button
            id="water-add-250-btn"
            onClick={() => handleQuickAdd(250)}
            className="flex flex-col items-center justify-center p-2 rounded-xl bg-sky-50 hover:bg-sky-100 text-sky-900 border border-sky-200 transition shadow-2xs hover:shadow-xs active:scale-95"
          >
            <span className="text-xs font-black">+250 ml</span>
            <span className="text-[10px] text-sky-600 font-medium">1 Glass</span>
          </button>

          <button
            id="water-add-500-btn"
            onClick={() => handleQuickAdd(500)}
            className="flex flex-col items-center justify-center p-2 rounded-xl bg-sky-50 hover:bg-sky-100 text-sky-900 border border-sky-200 transition shadow-2xs hover:shadow-xs active:scale-95"
          >
            <span className="text-xs font-black">+500 ml</span>
            <span className="text-[10px] text-sky-600 font-medium">1 Bottle</span>
          </button>

          <button
            id="water-add-750-btn"
            onClick={() => handleQuickAdd(750)}
            className="flex flex-col items-center justify-center p-2 rounded-xl bg-sky-50 hover:bg-sky-100 text-sky-900 border border-sky-200 transition shadow-2xs hover:shadow-xs active:scale-95"
          >
            <span className="text-xs font-black">+750 ml</span>
            <span className="text-[10px] text-sky-600 font-medium">1 Sipper</span>
          </button>

          <button
            id="water-add-custom-toggle-btn"
            onClick={() => setShowCustomInput(!showCustomInput)}
            className="flex flex-col items-center justify-center p-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-800 border border-slate-300 transition shadow-2xs hover:shadow-xs active:scale-95"
          >
            <span className="text-xs font-black">Custom</span>
            <span className="text-[10px] text-slate-500 font-medium">Set ml</span>
          </button>
        </div>

        {/* Custom Input Form */}
        {showCustomInput && (
          <form
            onSubmit={handleCustomSubmit}
            className="flex items-center space-x-2 bg-slate-50 p-2.5 rounded-xl border border-slate-200"
          >
            <input
              type="number"
              min="20"
              max="5000"
              step="10"
              value={customAmount}
              onChange={(e) => setCustomAmount(e.target.value)}
              placeholder="e.g. 350"
              className="flex-1 bg-white border border-slate-300 rounded-lg px-3 py-1.5 text-xs font-bold text-slate-900 focus:outline-none focus:ring-2 focus:ring-sky-500"
              autoFocus
            />
            <span className="text-xs font-bold text-slate-600">ml</span>
            <button
              type="submit"
              className="bg-sky-600 hover:bg-sky-700 text-white text-xs font-bold px-3 py-1.5 rounded-lg transition shadow-xs"
            >
              Add
            </button>
            <button
              type="button"
              onClick={() => setShowCustomInput(false)}
              className="text-xs text-slate-500 hover:text-slate-800 px-2 py-1.5"
            >
              Cancel
            </button>
          </form>
        )}
      </div>

      {/* History / Logged Items Breakdown for selected date */}
      {showHistory && (
        <div className="pt-2 border-t border-slate-100 space-y-2">
          <div className="flex items-center justify-between text-xs font-bold text-slate-700">
            <span>Hydration Logs for {selectedDate}</span>
            <span className="text-slate-400 font-normal">{waterLogs.length} entries</span>
          </div>

          {waterLogs.length === 0 ? (
            <p className="text-xs text-slate-400 text-center py-2">
              No water intake logged yet today. Tap +250ml or +500ml above.
            </p>
          ) : (
            <div className="space-y-1.5 max-h-48 overflow-y-auto pr-1">
              {waterLogs.map((log) => (
                <div
                  key={log.id}
                  className="flex items-center justify-between bg-sky-50/60 p-2 rounded-xl border border-sky-100 text-xs"
                >
                  <div className="flex items-center space-x-2">
                    <Droplets className="w-3.5 h-3.5 text-sky-600" />
                    <span className="font-bold text-slate-800">{log.amount_ml} ml</span>
                    <span className="text-slate-400 text-[11px]">({log.time || 'Logged'})</span>
                  </div>
                  <button
                    onClick={() => onDeleteWaterLog(log.id)}
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
      )}
    </div>
  );
};
