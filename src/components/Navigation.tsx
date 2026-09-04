import React from 'react';
import { Utensils, TrendingUp, Calculator, Calendar } from 'lucide-react';

export type TabType = 'today' | 'food' | 'progress' | 'calculator';

interface NavigationProps {
  activeTab: TabType;
  onTabChange: (tab: TabType) => void;
}

export const Navigation: React.FC<NavigationProps> = ({ activeTab, onTabChange }) => {
  const tabs = [
    {
      id: 'today' as TabType,
      label: 'Today',
      icon: Calendar,
      sublabel: 'Daily Summary',
    },
    {
      id: 'food' as TabType,
      label: 'Food Log',
      icon: Utensils,
      sublabel: 'Search & Log',
    },
    {
      id: 'progress' as TabType,
      label: 'Progress',
      icon: TrendingUp,
      sublabel: 'Charts & Stats',
    },
    {
      id: 'calculator' as TabType,
      label: 'Calculator',
      icon: Calculator,
      sublabel: 'TDEE & Settings',
    },
  ];

  return (
    <nav
      id="bottom-navigation-bar"
      aria-label="Main Navigation"
      className="fixed bottom-0 left-0 right-0 z-40 bg-white/95 backdrop-blur-md border-t border-slate-200/80 shadow-lg md:max-w-md md:mx-auto md:bottom-2 md:rounded-2xl md:border"
    >
      <div className="flex items-center justify-around px-2 py-1.5">
        {tabs.map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              id={`nav-tab-${tab.id}`}
              onClick={() => onTabChange(tab.id)}
              className={`flex flex-col items-center justify-center flex-1 py-1 px-1 rounded-xl transition-all duration-200 ${
                isActive
                  ? 'text-teal-700 font-semibold'
                  : 'text-slate-500 hover:text-slate-800'
              }`}
            >
              <div
                className={`relative flex items-center justify-center w-10 h-7 rounded-full transition-all duration-200 ${
                  isActive ? 'bg-teal-100 text-teal-800' : 'bg-transparent'
                }`}
              >
                <Icon className={`w-5 h-5 ${isActive ? 'stroke-[2.4]' : 'stroke-[1.8]'}`} />
              </div>
              <span className="text-[11px] mt-0.5 tracking-tight">{tab.label}</span>
            </button>
          );
        })}
      </div>
    </nav>
  );
};
