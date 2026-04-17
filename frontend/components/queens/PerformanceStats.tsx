"use client";

type PerformanceStatsProps = {
  sequentialTimeMs: number;
  threadedTimeMs: number;
  totalSolutions: number;
};

export default function PerformanceStats({
  sequentialTimeMs,
  threadedTimeMs,
  totalSolutions,
}: PerformanceStatsProps) {
  const maxTime = Math.max(sequentialTimeMs, threadedTimeMs, 0.001);
  const seqPct = (sequentialTimeMs / maxTime) * 100;
  const thrPct = (threadedTimeMs / maxTime) * 100;

  const speedup =
    threadedTimeMs > 0 ? sequentialTimeMs / threadedTimeMs : 0;
  const faster = speedup > 1 ? "Threaded" : speedup > 0 ? "Sequential" : null;

  return (
    <div className="flex flex-col gap-5">
      <div>
        <div className="flex items-center justify-between mb-2">
          <span className="text-[10px] font-mono uppercase tracking-[0.2em] text-white/50">
            Sequential
          </span>
          <span className="text-xs font-mono text-white">
            {sequentialTimeMs.toFixed(2)} ms
          </span>
        </div>
        <div className="h-2 bg-white/5 rounded-full overflow-hidden">
          <div
            className="h-full bg-gradient-to-r from-amber-500 to-orange-600 transition-all duration-700"
            style={{ width: `${seqPct}%` }}
          />
        </div>
      </div>

      <div>
        <div className="flex items-center justify-between mb-2">
          <span className="text-[10px] font-mono uppercase tracking-[0.2em] text-white/50">
            Threaded
          </span>
          <span className="text-xs font-mono text-white">
            {threadedTimeMs.toFixed(2)} ms
          </span>
        </div>
        <div className="h-2 bg-white/5 rounded-full overflow-hidden">
          <div
            className="h-full bg-gradient-to-r from-blue-500 to-cyan-500 transition-all duration-700"
            style={{ width: `${thrPct}%` }}
          />
        </div>
      </div>

      <div className="grid grid-cols-2 gap-3 pt-3 border-t border-white/5">
        <div>
          <div className="text-[9px] font-mono uppercase tracking-[0.2em] text-white/40 mb-1">
            Solutions
          </div>
          <div className="text-lg font-black text-white tabular-nums">
            {totalSolutions.toLocaleString()}
          </div>
        </div>
        <div>
          <div className="text-[9px] font-mono uppercase tracking-[0.2em] text-white/40 mb-1">
            Speedup
          </div>
          <div
            className={`text-lg font-black tabular-nums ${
              speedup > 1 ? "text-green-400" : "text-red-400"
            }`}
          >
            {speedup.toFixed(2)}×
          </div>
        </div>
      </div>

      {faster && (
        <div className="text-[9px] font-mono uppercase tracking-[0.2em] text-white/40 text-center">
          {faster} faster
        </div>
      )}
    </div>
  );
}
