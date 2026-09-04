import React from 'react';
import { ChevronLeft, ChevronRight, Calendar as CalendarIcon, Sparkles } from 'lucide-react';
import { formatDateKey, getOffsetDate } from '../services/storage';

interface TopBarProps {
  selectedDate: string;
  onDateChange: (date: string) => void;
  onOpenQuickLog?: () => void;
}

export const TopBar: React.FC<TopBarProps> = ({
  selectedDate,
  onDateChange,
}) => {
  const todayStr = formatDateKey(new Date());
  const isToday = selectedDate === todayStr;

  const handlePrevDay = () => {
    const current = new Date(selectedDate);
    current.setDate(current.getDate() - 1);
    onDateChange(formatDateKey(current));
  };

  const handleNextDay = () => {
    const current = new Date(selectedDate);
    current.setDate(current.getDate() + 1);
    onDateChange(formatDateKey(current));
  };

  const handleTodayJump = () => {
    onDateChange(todayStr);
  };

  const formatDisplayDate = (dateStr: string) => {
    if (dateStr === todayStr) return 'Today';
    if (dateStr === getOffsetDate(-1)) return 'Yesterday';
    if (dateStr === getOffsetDate(1)) return 'Tomorrow';
    
    const d = new Date(dateStr + 'T00:00:00');
    return d.toLocaleDateString('en-US', {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
    });
  };

  return (
    <header
      id="app-top-header"
      className="sticky top-0 z-30 bg-slate-900 text-white px-4 py-3 shadow-md md:rounded-t-2xl"
    >
      <div className="flex items-center justify-between mb-2">
        <div className="flex items-center space-x-2">
          <div className="w-8 h-8 rounded-lg bg-teal-500 flex items-center justify-center text-slate-950 font-black text-sm tracking-tighter">
            IN
          </div>
          <div>
            <h1 className="text-sm font-bold tracking-tight leading-none text-white">
              Indian Nutrition &amp; Weight
            </h1>
            <p className="text-[10px] text-teal-400 font-medium tracking-wide">
              NIN / IFCT • Open Food Facts
            </p>
          </div>
        </div>

        <div className="flex items-center space-x-1">
          {!isToday && (
            <button
              id="jump-to-today-btn"
              onClick={handleTodayJump}
              className="text-xs bg-teal-500/20 text-teal-300 border border-teal-500/40 hover:bg-teal-500/30 px-2.5 py-1 rounded-full font-medium transition"
            >
              Jump to Today
            </button>
          )}
        </div>
      </div>

      {/* Date Switcher Bar */}
      <div className="flex items-center justify-between bg-slate-800/90 rounded-xl px-2 py-1.5 border border-slate-700/60">
        <button
          id="prev-date-button"
          onClick={handlePrevDay}
          className="p-1 text-slate-300 hover:text-white hover:bg-slate-700 rounded-lg transition"
          aria-label="Previous Day"
        >
          <ChevronLeft className="w-4 h-4" />
        </button>

        <div className="flex items-center space-x-2">
          <CalendarIcon className="w-3.5 h-3.5 text-teal-400" />
          <span className="text-xs font-semibold text-slate-100 tracking-wide">
            {formatDisplayDate(selectedDate)}
          </span>
          <span className="text-[11px] text-slate-400">({selectedDate})</span>
        </div>

        <button
          id="next-date-button"
          onClick={handleNextDay}
          className="p-1 text-slate-300 hover:text-white hover:bg-slate-700 rounded-lg transition"
          aria-label="Next Day"
        >
          <ChevronRight className="w-4 h-4" />
        </button>
      </div>
    </header>
  );
};
