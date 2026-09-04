import React, { useState } from 'react';
import {
  TrendingUp,
  Scale,
  Plus,
  Trash2,
  Calendar,
  Sparkles,
  Info,
  CheckCircle2,
  AlertCircle,
} from 'lucide-react';
import { ProgressionCharts } from '../components/Charts';
import { DailyLog, UserSettings, WaterLog, WeightLog } from '../types';
import { formatWeight, calculateBMI } from '../utils/calculator';

interface ProgressScreenProps {
  dailyLogs: DailyLog[];
  weightLogs: WeightLog[];
  waterLogs?: WaterLog[];
  settings: UserSettings;
  onOpenLogWeight: () => void;
  onDeleteWeightLog: (id: string) => void;
}

export const ProgressScreen: React.FC<ProgressScreenProps> = ({
  dailyLogs,
  weightLogs,
  waterLogs = [],
  settings,
  onOpenLogWeight,
  onDeleteWeightLog,
}) => {
  const latestWeight = weightLogs.length > 0 ? weightLogs[weightLogs.length - 1].weight_kg : settings.current_weight_kg;
  const startingWeight = weightLogs.length > 0 ? weightLogs[0].weight_kg : settings.current_weight_kg;
  const targetWeight = settings.target_weight_kg;

  const bmiInfo = calculateBMI(latestWeight, settings.height_cm);
  const diffFromStart = Number((latestWeight - startingWeight).toFixed(1));
  const diffFromTarget = Number((latestWeight - targetWeight).toFixed(1));

  // Reverse list for chronological display
  const reversedWeightLogs = [...weightLogs].reverse();

  return (
    <div className="space-y-4 pb-20">
      {/* Top Banner Stats */}
      <div className="bg-gradient-to-br from-slate-900 via-slate-800 to-teal-950 text-white rounded-2xl p-4 shadow-md">
        <div className="flex items-center justify-between mb-3">
          <div>
            <span className="text-xs uppercase tracking-wider text-teal-400 font-bold">
              Progress &amp; Trends
            </span>
            <h2 className="text-lg font-black tracking-tight">Body Composition &amp; Intake</h2>
          </div>
          <button
            id="progress-log-weight-btn"
            onClick={onOpenLogWeight}
            className="flex items-center space-x-1.5 bg-teal-500 hover:bg-teal-400 text-slate-950 px-3 py-1.5 rounded-xl text-xs font-black transition shadow-sm"
          >
            <Plus className="w-4 h-4" />
            <span>Log Weight</span>
          </button>
        </div>

        <div className="grid grid-cols-3 gap-2 text-center pt-2 border-t border-slate-700/80">
          <div>
            <div className="text-[10px] uppercase font-bold text-slate-400">Current Weight</div>
            <div className="text-base font-black text-white">
              {formatWeight(latestWeight, settings.unit_system)}
            </div>
            <div className="text-[10px] text-teal-300 font-medium">{bmiInfo.category}</div>
          </div>

          <div>
            <div className="text-[10px] uppercase font-bold text-slate-400">Net Change</div>
            <div
              className={`text-base font-black ${
                diffFromStart <= 0 ? 'text-emerald-400' : 'text-amber-400'
              }`}
            >
              {diffFromStart > 0 ? '+' : ''}
              {formatWeight(diffFromStart, settings.unit_system)}
            </div>
            <div className="text-[10px] text-slate-400">From start</div>
          </div>

          <div>
            <div className="text-[10px] uppercase font-bold text-slate-400">Goal Target</div>
            <div className="text-base font-black text-teal-300">
              {formatWeight(targetWeight, settings.unit_system)}
            </div>
            <div className="text-[10px] text-slate-400">
              {Math.abs(diffFromTarget) <= 0.2
                ? 'Reached!'
                : `${formatWeight(Math.abs(diffFromTarget), settings.unit_system)} left`}
            </div>
          </div>
        </div>
      </div>

      {/* Main Interactive Charts Component */}
      <ProgressionCharts
        dailyLogs={dailyLogs}
        weightLogs={weightLogs}
        waterLogs={waterLogs}
        settings={settings}
      />

      {/* Weight Log History Table / List */}
      <div className="bg-white rounded-2xl p-4 shadow-sm border border-slate-200 space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <Scale className="w-4 h-4 text-teal-600" />
            <h3 className="text-xs font-extrabold text-slate-800 uppercase tracking-wider">
              Weight Log History ({weightLogs.length} entries)
            </h3>
          </div>
          <button
            onClick={onOpenLogWeight}
            className="text-xs font-bold text-teal-700 bg-teal-50 px-2.5 py-1 rounded-lg hover:bg-teal-100 transition"
          >
            + Add Entry
          </button>
        </div>

        {reversedWeightLogs.length === 0 ? (
          <div className="text-center py-6 text-slate-400 text-xs">
            No weight entries logged yet.
          </div>
        ) : (
          <div className="divide-y divide-slate-100">
            {reversedWeightLogs.map((log) => {
              const dObj = new Date(log.date + 'T00:00:00');
              const displayDate = dObj.toLocaleDateString('en-US', {
                weekday: 'short',
                month: 'short',
                day: 'numeric',
                year: 'numeric',
              });

              return (
                <div
                  key={log.id}
                  className="py-2.5 flex items-center justify-between hover:bg-slate-50 px-2 rounded-xl transition"
                >
                  <div className="flex items-center space-x-3">
                    <div className="w-8 h-8 rounded-lg bg-teal-50 text-teal-700 font-bold text-xs flex items-center justify-center border border-teal-100">
                      ⚖️
                    </div>
                    <div>
                      <div className="text-sm font-bold text-slate-900">
                        {formatWeight(log.weight_kg, settings.unit_system)}
                      </div>
                      <div className="text-[11px] text-slate-500 flex items-center space-x-1.5">
                        <span>{displayDate}</span>
                        {log.note && (
                          <>
                            <span>•</span>
                            <span className="italic text-slate-400 truncate max-w-[140px]">
                              "{log.note}"
                            </span>
                          </>
                        )}
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center space-x-2">
                    <button
                      onClick={() => onDeleteWeightLog(log.id)}
                      className="p-1 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition"
                      title="Delete weight entry"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};
