import React, { useState } from 'react';
import { TrendingUp, Flame, Dumbbell, Droplets, Target, CheckCircle2, AlertCircle } from 'lucide-react';
import { DailyLog, DateRange, UserSettings, WaterLog, WeightLog } from '../types';
import { formatWeight, kgToLb } from '../utils/calculator';
import { formatDateKey } from '../services/storage';

interface ChartsProps {
  dailyLogs: DailyLog[];
  weightLogs: WeightLog[];
  waterLogs?: WaterLog[];
  settings: UserSettings;
}

export const ProgressionCharts: React.FC<ChartsProps> = ({
  dailyLogs,
  weightLogs,
  waterLogs = [],
  settings,
}) => {
  const [activeMetric, setActiveMetric] = useState<'weight' | 'calories' | 'protein' | 'water'>('weight');
  const [dateRange, setDateRange] = useState<DateRange>(settings.default_chart_range || '14d');
  const [hoveredPoint, setHoveredPoint] = useState<{ label: string; value: number; target?: number; date: string } | null>(null);

  // Compute number of days based on selected range
  const rangeDays = dateRange === '7d' ? 7 : dateRange === '14d' ? 14 : dateRange === '30d' ? 30 : 60;
  const today = new Date();

  // Generate sequence of dates for the selected range
  const dateArray: string[] = [];
  for (let i = rangeDays - 1; i >= 0; i--) {
    const d = new Date(today);
    d.setDate(d.getDate() - i);
    dateArray.push(formatDateKey(d));
  }

  // Aggregate daily calories & protein & water
  const dailyCaloriesMap: Record<string, number> = {};
  const dailyProteinMap: Record<string, number> = {};
  const dailyWaterMap: Record<string, number> = {};

  dailyLogs.forEach((log) => {
    dailyCaloriesMap[log.date] = (dailyCaloriesMap[log.date] || 0) + log.calories;
    dailyProteinMap[log.date] = Number(((dailyProteinMap[log.date] || 0) + log.protein).toFixed(1));
  });

  waterLogs.forEach((log) => {
    dailyWaterMap[log.date] = (dailyWaterMap[log.date] || 0) + log.amount_ml;
  });

  // Map weight logs to dates with forward fill
  const weightMap: Record<string, number> = {};
  weightLogs.forEach((w) => {
    weightMap[w.date] = w.weight_kg;
  });

  // Calculate stats
  const startingWeight = weightLogs.length > 0 ? weightLogs[0].weight_kg : settings.current_weight_kg;
  const currentWeight = weightLogs.length > 0 ? weightLogs[weightLogs.length - 1].weight_kg : settings.current_weight_kg;
  const targetWeight = settings.target_weight_kg;

  const validCalorieDays = dateArray.filter((d) => (dailyCaloriesMap[d] || 0) > 0);
  const avgCalories = validCalorieDays.length > 0
    ? Math.round(validCalorieDays.reduce((sum, d) => sum + (dailyCaloriesMap[d] || 0), 0) / validCalorieDays.length)
    : 0;

  const validProteinDays = dateArray.filter((d) => (dailyProteinMap[d] || 0) > 0);
  const avgProtein = validProteinDays.length > 0
    ? Number((validProteinDays.reduce((sum, d) => sum + (dailyProteinMap[d] || 0), 0) / validProteinDays.length).toFixed(1))
    : 0;

  const validWaterDays = dateArray.filter((d) => (dailyWaterMap[d] || 0) > 0);
  const avgWater = validWaterDays.length > 0
    ? Math.round(validWaterDays.reduce((sum, d) => sum + (dailyWaterMap[d] || 0), 0) / validWaterDays.length)
    : 0;

  // Prepare chart series data
  let chartData: { date: string; displayDate: string; value: number; target: number }[] = [];
  let yUnit = '';
  let targetVal = 0;

  if (activeMetric === 'weight') {
    targetVal = settings.unit_system === 'lb' ? kgToLb(settings.target_weight_kg) : settings.target_weight_kg;
    yUnit = settings.unit_system;
    
    // Find closest recent weight for days without explicit weight
    let lastKnownWeight = startingWeight;
    chartData = dateArray.map((dateStr) => {
      if (weightMap[dateStr] !== undefined) {
        lastKnownWeight = weightMap[dateStr];
      }
      const val = settings.unit_system === 'lb' ? kgToLb(lastKnownWeight) : lastKnownWeight;
      const dObj = new Date(dateStr + 'T00:00:00');
      return {
        date: dateStr,
        displayDate: dObj.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
        value: Number(val.toFixed(1)),
        target: Number(targetVal.toFixed(1)),
      };
    });
  } else if (activeMetric === 'calories') {
    targetVal = settings.daily_calorie_target;
    yUnit = 'kcal';
    chartData = dateArray.map((dateStr) => {
      const val = dailyCaloriesMap[dateStr] || 0;
      const dObj = new Date(dateStr + 'T00:00:00');
      return {
        date: dateStr,
        displayDate: dObj.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
        value: val,
        target: targetVal,
      };
    });
  } else if (activeMetric === 'protein') {
    targetVal = settings.daily_protein_target;
    yUnit = 'g';
    chartData = dateArray.map((dateStr) => {
      const val = dailyProteinMap[dateStr] || 0;
      const dObj = new Date(dateStr + 'T00:00:00');
      return {
        date: dateStr,
        displayDate: dObj.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
        value: val,
        target: targetVal,
      };
    });
  } else {
    targetVal = settings.daily_water_target_ml || 2500;
    yUnit = 'ml';
    chartData = dateArray.map((dateStr) => {
      const val = dailyWaterMap[dateStr] || 0;
      const dObj = new Date(dateStr + 'T00:00:00');
      return {
        date: dateStr,
        displayDate: dObj.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
        value: val,
        target: targetVal,
      };
    });
  }

  // SVG Chart Geometry calculations
  const values = chartData.map((d) => d.value).concat([targetVal]);
  const nonZeroValues = chartData.map((d) => d.value).filter((v) => v > 0).concat([targetVal]);
  const minValue = activeMetric === 'weight'
    ? Math.floor(Math.min(...values) - 2)
    : 0;
  const maxValue = Math.ceil(Math.max(...values, targetVal * 1.1) + (activeMetric === 'weight' ? 2 : 10));

  const svgWidth = 600;
  const svgHeight = 220;
  const padLeft = 45;
  const padRight = 20;
  const padTop = 25;
  const padBottom = 35;
  const chartWidth = svgWidth - padLeft - padRight;
  const chartHeight = svgHeight - padTop - padBottom;

  const getY = (val: number) => {
    if (maxValue === minValue) return padTop + chartHeight / 2;
    const ratio = (val - minValue) / (maxValue - minValue);
    return padTop + chartHeight - ratio * chartHeight;
  };

  const getX = (idx: number) => {
    if (chartData.length <= 1) return padLeft + chartWidth / 2;
    return padLeft + (idx / (chartData.length - 1)) * chartWidth;
  };

  // Build SVG Path
  const points = chartData.map((d, i) => `${getX(i)},${getY(d.value)}`).join(' ');
  const targetY = getY(targetVal);

  return (
    <div className="space-y-4">
      {/* Metric Segmented Control */}
      <div className="grid grid-cols-4 bg-slate-200/80 p-1 rounded-xl shadow-inner gap-1">
        <button
          id="chart-metric-weight-btn"
          onClick={() => {
            setActiveMetric('weight');
            setHoveredPoint(null);
          }}
          className={`py-1.5 px-2 rounded-lg text-xs font-bold transition flex items-center justify-center space-x-1 ${
            activeMetric === 'weight'
              ? 'bg-white text-slate-900 shadow-sm'
              : 'text-slate-600 hover:text-slate-900'
          }`}
        >
          <TrendingUp className="w-3.5 h-3.5 text-teal-600" />
          <span>Weight</span>
        </button>

        <button
          id="chart-metric-calories-btn"
          onClick={() => {
            setActiveMetric('calories');
            setHoveredPoint(null);
          }}
          className={`py-1.5 px-2 rounded-lg text-xs font-bold transition flex items-center justify-center space-x-1 ${
            activeMetric === 'calories'
              ? 'bg-white text-slate-900 shadow-sm'
              : 'text-slate-600 hover:text-slate-900'
          }`}
        >
          <Flame className="w-3.5 h-3.5 text-amber-500" />
          <span>Calories</span>
        </button>

        <button
          id="chart-metric-protein-btn"
          onClick={() => {
            setActiveMetric('protein');
            setHoveredPoint(null);
          }}
          className={`py-1.5 px-2 rounded-lg text-xs font-bold transition flex items-center justify-center space-x-1 ${
            activeMetric === 'protein'
              ? 'bg-white text-slate-900 shadow-sm'
              : 'text-slate-600 hover:text-slate-900'
          }`}
        >
          <Dumbbell className="w-3.5 h-3.5 text-emerald-600" />
          <span>Protein</span>
        </button>

        <button
          id="chart-metric-water-btn"
          onClick={() => {
            setActiveMetric('water');
            setHoveredPoint(null);
          }}
          className={`py-1.5 px-2 rounded-lg text-xs font-bold transition flex items-center justify-center space-x-1 ${
            activeMetric === 'water'
              ? 'bg-white text-sky-900 shadow-sm'
              : 'text-slate-600 hover:text-slate-900'
          }`}
        >
          <Droplets className="w-3.5 h-3.5 text-sky-500" />
          <span>Water</span>
        </button>
      </div>

      {/* Date Range Selector */}
      <div className="flex items-center justify-between px-1">
        <div className="text-xs font-bold text-slate-700 flex items-center space-x-1.5">
          <Target className="w-4 h-4 text-teal-600" />
          <span>
            {activeMetric === 'weight'
              ? 'Body Weight Progression'
              : activeMetric === 'calories'
              ? 'Daily Calorie Intake vs Target'
              : activeMetric === 'protein'
              ? 'Daily Protein Intake vs Target'
              : 'Daily Water Hydration vs Target'}
          </span>
        </div>

        <div className="flex items-center space-x-1 bg-slate-100 p-0.5 rounded-lg border border-slate-200">
          {(['7d', '14d', '30d', 'All'] as DateRange[]).map((range) => (
            <button
              key={range}
              id={`date-range-btn-${range.toLowerCase()}`}
              onClick={() => {
                setDateRange(range);
                setHoveredPoint(null);
              }}
              className={`px-2 py-1 text-[11px] font-bold rounded-md transition ${
                dateRange === range
                  ? 'bg-teal-600 text-white shadow-xs'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              {range}
            </button>
          ))}
        </div>
      </div>

      {/* Interactive Chart Container */}
      <div className="bg-white rounded-2xl p-4 shadow-sm border border-slate-200">
        <div className="flex items-center justify-between mb-2">
          <div className="text-xs text-slate-500 font-medium">
            {hoveredPoint ? (
              <span className="text-slate-900 font-bold">
                {hoveredPoint.date}: <span className="text-teal-700">{hoveredPoint.value} {yUnit}</span>
                {hoveredPoint.target && (
                  <span className="text-slate-400 font-normal ml-1">
                    (Target: {hoveredPoint.target} {yUnit})
                  </span>
                )}
              </span>
            ) : (
              <span>Touch or hover over data points for exact values</span>
            )}
          </div>

          <div className="flex items-center space-x-3 text-[11px] font-semibold">
            <div className="flex items-center space-x-1">
              <span className={`w-2.5 h-2.5 rounded-full inline-block ${activeMetric === 'water' ? 'bg-sky-500' : 'bg-teal-600'}`} />
              <span className="text-slate-600">Logged</span>
            </div>
            <div className="flex items-center space-x-1">
              <span className="w-3.5 h-0.5 border-t-2 border-dashed border-rose-500 inline-block" />
              <span className="text-slate-600">Target ({targetVal} {yUnit})</span>
            </div>
          </div>
        </div>

        {/* SVG Chart */}
        <div className="w-full overflow-x-auto">
          <svg
            viewBox={`0 0 ${svgWidth} ${svgHeight}`}
            className="w-full h-48 select-none"
          >
            <defs>
              <linearGradient id="chartGradient" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor="#0d9488" stopOpacity="0.25" />
                <stop offset="100%" stopColor="#0d9488" stopOpacity="0.0" />
              </linearGradient>
            </defs>

            {/* Horizontal Grid lines */}
            {[0, 0.25, 0.5, 0.75, 1].map((pct, i) => {
              const yVal = Math.round(minValue + (maxValue - minValue) * (1 - pct));
              const y = padTop + chartHeight * pct;
              return (
                <g key={i}>
                  <line
                    x1={padLeft}
                    y1={y}
                    x2={svgWidth - padRight}
                    y2={y}
                    stroke="#e2e8f0"
                    strokeWidth="1"
                    strokeDasharray="4 4"
                  />
                  <text
                    x={padLeft - 8}
                    y={y + 3.5}
                    textAnchor="end"
                    fontSize="9"
                    fill="#94a3b8"
                    fontWeight="600"
                  >
                    {yVal}
                  </text>
                </g>
              );
            })}

            {/* Target Reference Line */}
            {targetY >= padTop && targetY <= padTop + chartHeight && (
              <g>
                <line
                  x1={padLeft}
                  y1={targetY}
                  x2={svgWidth - padRight}
                  y2={targetY}
                  stroke="#f43f5e"
                  strokeWidth="1.5"
                  strokeDasharray="5 4"
                />
                <text
                  x={svgWidth - padRight}
                  y={targetY - 4}
                  textAnchor="end"
                  fontSize="9"
                  fill="#f43f5e"
                  fontWeight="bold"
                >
                  Target {targetVal} {yUnit}
                </text>
              </g>
            )}

            {/* Area Fill for line charts */}
            {activeMetric === 'weight' && chartData.length > 1 && (
              <polygon
                points={`${padLeft},${padTop + chartHeight} ${points} ${getX(chartData.length - 1)},${padTop + chartHeight}`}
                fill="url(#chartGradient)"
              />
            )}

            {/* Chart Graphic: Bars for calories/protein/water or Line for weight */}
            {activeMetric === 'weight' ? (
              <polyline
                fill="none"
                stroke="#0d9488"
                strokeWidth="2.5"
                strokeLinecap="round"
                strokeLinejoin="round"
                points={points}
              />
            ) : (
              // Bar representation for daily intake
              chartData.map((d, i) => {
                const x = getX(i);
                const barWidth = Math.max(6, Math.min(22, chartWidth / chartData.length - 4));
                const y = getY(d.value);
                const height = Math.max(0, padTop + chartHeight - y);
                const isOver = d.value > targetVal && targetVal > 0;
                const isHit = Math.abs(d.value - targetVal) <= targetVal * 0.08;

                const fillColor =
                  d.value === 0
                    ? '#cbd5e1'
                    : activeMetric === 'water'
                    ? '#0284c7'
                    : isOver
                    ? '#f59e0b'
                    : isHit
                    ? '#10b981'
                    : activeMetric === 'calories'
                    ? '#0d9488'
                    : '#059669';

                return (
                  <rect
                    key={i}
                    x={x - barWidth / 2}
                    y={y}
                    width={barWidth}
                    height={height}
                    rx="3"
                    fill={fillColor}
                    opacity="0.9"
                    className="transition-all hover:opacity-100 cursor-pointer"
                    onMouseEnter={() =>
                      setHoveredPoint({
                        label: d.displayDate,
                        value: d.value,
                        target: d.target,
                        date: d.date,
                      })
                    }
                  />
                );
              })
            )}

            {/* Interactive Data Dots (for weight) */}
            {activeMetric === 'weight' &&
              chartData.map((d, i) => {
                const x = getX(i);
                const y = getY(d.value);
                return (
                  <circle
                    key={i}
                    cx={x}
                    cy={y}
                    r="4.5"
                    fill="#ffffff"
                    stroke="#0d9488"
                    strokeWidth="2"
                    className="cursor-pointer hover:scale-150 transition-transform"
                    onMouseEnter={() =>
                      setHoveredPoint({
                        label: d.displayDate,
                        value: d.value,
                        target: d.target,
                        date: d.date,
                      })
                    }
                  />
                );
              })}

            {/* X-Axis Dates */}
            {chartData.map((d, i) => {
              const step = chartData.length > 20 ? 5 : chartData.length > 10 ? 2 : 1;
              if (i % step !== 0 && i !== chartData.length - 1) return null;
              const x = getX(i);
              return (
                <text
                  key={i}
                  x={x}
                  y={svgHeight - 10}
                  textAnchor="middle"
                  fontSize="9"
                  fill="#64748b"
                  fontWeight="500"
                >
                  {d.displayDate}
                </text>
              );
            })}
          </svg>
        </div>
      </div>

      {/* Summary Stats Grid */}
      <div className="grid grid-cols-3 gap-2">
        <div className="bg-white p-3 rounded-xl border border-slate-200 text-center shadow-xs">
          <div className="text-[11px] font-semibold text-slate-500 mb-0.5">Starting Weight</div>
          <div className="text-sm font-bold text-slate-900">
            {formatWeight(startingWeight, settings.unit_system)}
          </div>
          <div className="text-[10px] text-slate-400">Baseline</div>
        </div>

        <div className="bg-white p-3 rounded-xl border border-slate-200 text-center shadow-xs">
          <div className="text-[11px] font-semibold text-slate-500 mb-0.5">Current Weight</div>
          <div className="text-sm font-black text-teal-700">
            {formatWeight(currentWeight, settings.unit_system)}
          </div>
          <div className="text-[10px] font-bold text-emerald-600">
            {currentWeight - startingWeight <= 0 ? '↓ ' : '↑ '}
            {formatWeight(Math.abs(currentWeight - startingWeight), settings.unit_system)}
          </div>
        </div>

        <div className="bg-white p-3 rounded-xl border border-slate-200 text-center shadow-xs">
          <div className="text-[11px] font-semibold text-slate-500 mb-0.5">Target Weight</div>
          <div className="text-sm font-bold text-slate-900">
            {formatWeight(targetWeight, settings.unit_system)}
          </div>
          <div className="text-[10px] text-teal-600 font-semibold">
            {Math.abs(currentWeight - targetWeight) <= 0.2 ? 'Goal Reached!' : `${formatWeight(Math.abs(currentWeight - targetWeight), settings.unit_system)} to go`}
          </div>
        </div>
      </div>

      {/* Secondary Intake Averages (7/14 Days) */}
      <div className="bg-slate-50 p-3.5 rounded-xl border border-slate-200">
        <div className="text-xs font-bold text-slate-700 mb-2">
          Intake &amp; Hydration Averages ({dateRange} Range)
        </div>
        <div className="grid grid-cols-3 gap-2">
          <div className="bg-white p-2.5 rounded-lg border border-slate-200 flex flex-col justify-between">
            <div>
              <div className="text-[10px] uppercase font-bold text-slate-500">Avg Calories</div>
              <div className="text-sm font-black text-amber-600">
                {avgCalories} <span className="text-[10px] font-normal text-slate-500">kcal/d</span>
              </div>
            </div>
            <div className="text-left mt-1">
              <span className="text-[9px] font-semibold text-slate-400">Target: {settings.daily_calorie_target}</span>
            </div>
          </div>

          <div className="bg-white p-2.5 rounded-lg border border-slate-200 flex flex-col justify-between">
            <div>
              <div className="text-[10px] uppercase font-bold text-slate-500">Avg Protein</div>
              <div className="text-sm font-black text-emerald-600">
                {avgProtein} <span className="text-[10px] font-normal text-slate-500">g/d</span>
              </div>
            </div>
            <div className="text-left mt-1">
              <span className="text-[9px] font-semibold text-slate-400">Target: {settings.daily_protein_target}g</span>
            </div>
          </div>

          <div className="bg-white p-2.5 rounded-lg border border-slate-200 flex flex-col justify-between">
            <div>
              <div className="text-[10px] uppercase font-bold text-slate-500">Avg Water</div>
              <div className="text-sm font-black text-sky-600">
                {avgWater} <span className="text-[10px] font-normal text-slate-500">ml/d</span>
              </div>
            </div>
            <div className="text-left mt-1">
              <span className="text-[9px] font-semibold text-slate-400">Target: {settings.daily_water_target_ml || 2500}ml</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

