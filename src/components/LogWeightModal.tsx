import React, { useState } from 'react';
import { X, Scale, Check, Calendar, StickyNote, ArrowDownRight, ArrowUpRight } from 'lucide-react';
import { formatDateKey } from '../services/storage';
import { UserSettings, WeightLog } from '../types';
import { formatWeight, kgToLb, lbToKg } from '../utils/calculator';

interface LogWeightModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSaveWeight: (date: string, weightKg: number, note?: string) => void;
  settings: UserSettings;
  weightLogs: WeightLog[];
}

export const LogWeightModal: React.FC<LogWeightModalProps> = ({
  isOpen,
  onClose,
  onSaveWeight,
  settings,
  weightLogs,
}) => {
  if (!isOpen) return null;

  const today = formatDateKey(new Date());
  const [date, setDate] = useState<string>(today);
  const [weightInput, setWeightInput] = useState<string>(
    settings.unit_system === 'lb'
      ? kgToLb(settings.current_weight_kg).toString()
      : settings.current_weight_kg.toString()
  );
  const [note, setNote] = useState<string>('');

  const startingWeight = weightLogs.length > 0 ? weightLogs[0].weight_kg : settings.current_weight_kg;
  const targetWeight = settings.target_weight_kg;

  const numericWeight = parseFloat(weightInput) || 0;
  const effectiveKg = settings.unit_system === 'lb' ? lbToKg(numericWeight) : numericWeight;

  const diffFromStart = effectiveKg > 0 ? Number((effectiveKg - startingWeight).toFixed(1)) : 0;
  const diffFromTarget = effectiveKg > 0 ? Number((effectiveKg - targetWeight).toFixed(1)) : 0;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (effectiveKg <= 20 || effectiveKg >= 350) {
      alert('Please enter a realistic weight value.');
      return;
    }
    onSaveWeight(date, effectiveKg, note.trim() || undefined);
    onClose();
  };

  return (
    <div
      id="log-weight-modal-overlay"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/70 backdrop-blur-sm animate-fade-in"
      onClick={onClose}
    >
      <div
        id="log-weight-modal-dialog"
        className="bg-white rounded-2xl w-full max-w-md shadow-2xl overflow-hidden border border-slate-200"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="bg-slate-900 text-white p-4 flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <div className="p-2 bg-teal-500/20 text-teal-400 rounded-xl">
              <Scale className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base font-bold">Log Body Weight</h2>
              <p className="text-xs text-slate-400">Track your progression over time</p>
            </div>
          </div>
          <button
            id="close-log-weight-btn"
            onClick={onClose}
            className="text-slate-400 hover:text-white p-1 rounded-lg hover:bg-slate-800 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-4 space-y-4">
          {/* Date Selector */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Date
            </label>
            <div className="relative">
              <input
                type="date"
                id="weight-log-date-input"
                value={date}
                max={today}
                onChange={(e) => setDate(e.target.value)}
                className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3 py-2 text-sm font-semibold text-slate-800 focus:ring-2 focus:ring-teal-500 focus:outline-none"
              />
              <Calendar className="w-4 h-4 text-slate-400 absolute right-3 top-2.5 pointer-events-none" />
            </div>
          </div>

          {/* Weight Input */}
          <div>
            <div className="flex items-center justify-between mb-1">
              <label className="text-xs font-semibold text-slate-700 uppercase tracking-wider">
                Weight ({settings.unit_system})
              </label>
              <span className="text-xs text-teal-700 font-medium">
                Target: {formatWeight(targetWeight, settings.unit_system)}
              </span>
            </div>
            <div className="relative">
              <input
                type="number"
                id="weight-log-val-input"
                step="0.1"
                min="20"
                max="500"
                value={weightInput}
                onChange={(e) => setWeightInput(e.target.value)}
                placeholder="e.g. 78.5"
                required
                className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3 py-2.5 text-lg font-bold text-slate-900 focus:ring-2 focus:ring-teal-500 focus:outline-none"
              />
              <span className="absolute right-3 top-3 text-sm font-bold text-slate-400">
                {settings.unit_system}
              </span>
            </div>
          </div>

          {/* Diff Indicators */}
          {effectiveKg > 0 && (
            <div className="grid grid-cols-2 gap-2 bg-slate-50 p-3 rounded-xl border border-slate-200">
              <div className="text-center">
                <div className="text-[11px] font-semibold text-slate-500">From Starting</div>
                <div className={`text-sm font-bold flex items-center justify-center ${diffFromStart <= 0 ? 'text-emerald-600' : 'text-amber-600'}`}>
                  {diffFromStart > 0 ? '+' : ''}{formatWeight(diffFromStart, settings.unit_system)}
                </div>
              </div>
              <div className="text-center">
                <div className="text-[11px] font-semibold text-slate-500">To Target</div>
                <div className="text-sm font-bold text-teal-700">
                  {diffFromTarget > 0 ? `${formatWeight(diffFromTarget, settings.unit_system)} to go` : 'Target Reached!'}
                </div>
              </div>
            </div>
          )}

          {/* Optional Note */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Note (Optional)
            </label>
            <div className="relative">
              <input
                type="text"
                id="weight-log-note-input"
                value={note}
                onChange={(e) => setNote(e.target.value)}
                placeholder="e.g. Fasted morning weigh-in, post gym"
                className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3 py-2 text-xs text-slate-800 focus:ring-2 focus:ring-teal-500 focus:outline-none"
              />
              <StickyNote className="w-3.5 h-3.5 text-slate-400 absolute right-3 top-2.5 pointer-events-none" />
            </div>
          </div>

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
              id="confirm-log-weight-btn"
              className="flex-1 py-2.5 px-4 rounded-xl bg-teal-600 hover:bg-teal-700 text-white text-sm font-bold shadow-md hover:shadow-lg transition flex items-center justify-center space-x-1.5"
            >
              <Check className="w-4 h-4" />
              <span>Save Weight</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
