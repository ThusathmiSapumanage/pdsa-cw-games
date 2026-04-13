"use client";

import { useState, useEffect, useRef } from "react";
import { motion, AnimatePresence } from "framer-motion";
// These are our project components for layout, auth protection, and inputs
import GameLayoutShell from "../../../components/layouts/GameLayoutShell";
import AuthGate from "../../../components/auth/AuthGate";
import AlgoInput from "../../../components/forms/AlgoInput";
import AlgoNotify, { NotifyType } from "../../../components/forms/AlgoNotify";
import MinCostAnalytics from "../../../components/mincost/MinCostAnalytics";
import PlaybackToolbar from "../../../components/mincost/PlaybackToolbar";
import AlgoDropdown from "../../../components/forms/AlgoDropdown";

/* 
 * Assignment Type:
 * This represents one pair of (Task -> Worker)
 * row = Task index
 * col = Employee index
 * cost = The value at that cell
 */
type Assignment = {
  row: number;
  col: number;
  cost: number;
};

/* 
 * ApiResponse Type:
 * This matches the JSON payload we get from our Spring Boot backend.
 */
type ApiResponse = {
  matrix: number[][];
  greedyTotalCost: number;
  hungarianTotalCost: number;
  greedyAssignments: Assignment[];
  hungarianAssignments: Assignment[];
  greedyLogs: string[];
  hungarianLogs: string[];
  hungarianSteps: HungarianStep[];
  greedyTimeMs: number;
  hungarianTimeMs: number;
};

/*
 * Step-by-Step data for the Hungarian visualizer.
 * Helps us see the reduction stages.
 */
type HungarianStep = {
  type: string;
  matrix: number[][];
  description: string;
  rowMinVals: number[] | null;
  colMinVals: number[] | null;
  markedRows: boolean[] | null;
  markedCols: boolean[] | null;
  delta: number | null;
};

// Shape of a past game record for the history table
type HistoryItem = {
  date: string;
  n: number;
  cost: number;
  greedyTime: number;
  hungarianTime: number;
};

/* 
 * MatrixTable Component:
 * This paints the N x N grid on our screen.
 * It also handles the cool lighting effects for "Step-by-Step" mode.
 */
function MatrixTable({
  title,
  matrix,
  assignments,
  currentStep,
  isGreedy,
  hungarianStep,
}: {
  title: string;
  matrix: number[][];
  assignments: Assignment[];
  currentStep?: number;
  isGreedy?: boolean;
  hungarianStep?: HungarianStep;
}) {
  const activeStep = currentStep !== undefined ? currentStep : assignments.length - 1;
  const showStepByStep = currentStep !== undefined;

  // Use the matrix from the hungarian step if it exists (Reduction Phase)
  const displayMatrix = hungarianStep?.matrix || matrix;

  const blockedRows = new Set<number>();
  const blockedCols = new Set<number>();
  
  if (showStepByStep && isGreedy) {
    for (let i = 0; i < activeStep; i++) {
        if(assignments[i]){
            blockedRows.add(assignments[i].row);
            blockedCols.add(assignments[i].col);
        }
    }
  }

  const assignmentMap = new Map<string, boolean>();
  // Only show assignments if we are NOT in a reduction phase step
  if (!hungarianStep || hungarianStep.type === "INITIAL" || !showStepByStep) {
    assignments.slice(0, activeStep + 1).forEach((a) => {
        assignmentMap.set(`${a.row}-${a.col}`, true);
    });
  }

  return (
    <div className="w-full">
      <div className="flex justify-between items-center mb-2 px-1">
        <p className="text-[10px] uppercase tracking-widest text-white/30 font-mono">
          {title}
        </p>
        <AnimatePresence>
            {hungarianStep && (
                <motion.span 
                    initial={{ opacity: 0, x: 10 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0 }}
                    className="text-[9px] bg-blue-500/20 text-blue-300 px-2 py-0.5 rounded border border-blue-500/20 font-mono italic"
                >
                    {hungarianStep.type.replace("_", " ")}
                </motion.span>
            )}
        </AnimatePresence>
      </div>

      <div className="overflow-auto max-h-[420px] border border-white/5 rounded-xl bg-[#030303] custom-scrollbar shadow-2xl relative">
        <table className="w-full text-[10px] border-collapse relative z-10">
          <tbody>
            {displayMatrix.map((row, rowIndex) => (
              <tr key={rowIndex}>
                {row.map((value, colIndex) => {
                  const cellKey = `${rowIndex}-${colIndex}`;
                  const isAssigned = assignmentMap.has(cellKey);
                  const isCurrentPick = showStepByStep && !hungarianStep && assignments[activeStep]?.row === rowIndex && assignments[activeStep]?.col === colIndex;
                  const isBlocked = showStepByStep && isGreedy && (blockedRows.has(rowIndex) || blockedCols.has(colIndex));
                  
                  // Hungarian specific highlights
                  const isReduced = hungarianStep && (hungarianStep.markedRows?.[rowIndex] || hungarianStep.markedCols?.[colIndex]);
                  const isZero = value === 0;

                  let cellClass = "border border-white/3 p-2 text-center transition-all duration-300 ";
                  
                  if (isCurrentPick) {
                    cellClass += "bg-purple-500/40 text-purple-200 font-bold scale-110 relative z-20 shadow-lg shadow-purple-500/30";
                  } else if (isAssigned) {
                    cellClass += "bg-green-500/20 text-green-300 font-medium";
                  } else if (isBlocked) {
                    cellClass += "opacity-10 text-white/20";
                  } else if (isReduced) {
                    cellClass += "bg-blue-500/10 text-blue-200/50";
                  } else if (isZero && hungarianStep) {
                    cellClass += "text-green-500/80 font-bold";
                  } else {
                    cellClass += "text-white/40";
                  }

                  return (
                    <motion.td 
                        key={cellKey} 
                        className={cellClass}
                        layout
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                    >
                      {value}
                    </motion.td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>

        {/* HUD FOR REDUCTIONS */}
        {hungarianStep?.delta && (
            <div className="absolute inset-0 flex items-center justify-center pointer-events-none z-30">
                <motion.div 
                    initial={{ scale: 0.5, opacity: 0 }}
                    animate={{ scale: 1, opacity: 1 }}
                    className="bg-purple-600 text-white px-4 py-2 rounded-full text-[10px] font-bold shadow-2xl border border-white/20"
                >
                    DELTA ADJUST: {hungarianStep.delta}
                </motion.div>
            </div>
        )}
      </div>
    </div>
  );
}

export default function MinimumCostPage() {
  // Input states
  const [n, setN] = useState("10"); 
  const [minCost, setMinCost] = useState("20");
  const [maxCost, setMaxCost] = useState("200");
  
  // Algorithm selection
  const [selectedAlgo, setSelectedAlgo] = useState<"greedy" | "hungarian">("greedy");
  
  // Loading and result states
  const [isLoading, setIsLoading] = useState(false);
  const [result, setResult] = useState<ApiResponse | null>(null);
  const [history, setHistory] = useState<HistoryItem[]>([]);
  
  // Visualizer control state
  const [currentStep, setCurrentStep] = useState<number | undefined>(undefined);
  const [isPlaying, setIsPlaying] = useState(false);
  const [speed, setSpeed] = useState(1);
  const timerRef = useRef<NodeJS.Timeout | null>(null);
  
  // Custom notification state
  const [notify, setNotify] = useState<{
    show: boolean;
    type: NotifyType;
    title: string;
    message: string;
  }>({
    show: false,
    type: "warning",
    title: "",
    message: "",
  });

  // Pull history from backend
  useEffect(() => {
    if (result) {
        setCurrentStep(undefined);
        setIsPlaying(false);
    }
    fetchHistory();
  }, [result]);

  // --- PLAYBACK ENGINE ---
  useEffect(() => {
    if (isPlaying && result) {
      const maxSteps = selectedAlgo === "greedy" 
        ? result.greedyAssignments.length 
        : result.hungarianSteps.length;
      
      timerRef.current = setInterval(() => {
        setCurrentStep((prev) => {
          const next = prev === undefined ? 0 : prev + 1;
          if (next >= maxSteps) {
            setIsPlaying(false);
            return prev;
          }
          return next;
        });
      }, 1000 / speed);
    } else {
      if (timerRef.current) clearInterval(timerRef.current);
    }
    return () => { if (timerRef.current) clearInterval(timerRef.current); };
  }, [isPlaying, result, speed, selectedAlgo]);

  const fetchHistory = async () => {
    try {
      const response = await fetch("http://localhost:8080/api/mincost/history");
      if (response.ok) {
        const data = await response.json();
        setHistory(data);
      }
    } catch {
      console.log("History failed to load");
    }
  };

  const runGame = async () => {
    const nValue = Number(n);
    const minValue = Number(minCost);
    const maxValue = Number(maxCost);

    if (!Number.isInteger(nValue) || nValue < 5 || nValue > 100) {
      setNotify({ show: true, type: "error", title: "Input Error", message: "Size (N) must be between 5 and 100!" });
      return;
    }
    
    setIsLoading(true);
    try {
      const response = await fetch("http://localhost:8080/api/mincost/start", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ n: nValue, minCost: minValue, maxCost: maxValue }),
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.error || "Internal server error occurred!");
      setResult(data);
      setNotify({ show: true, type: "success", title: "Calculated!", message: "Explore the steps using the playback toolbar." });
    } catch (error) {
      setNotify({ show: true, type: "error", title: "Server Error", message: error instanceof Error ? error.message : "Backend offline." });
    } finally {
      setIsLoading(false);
    }
  };

  const maxSteps = result ? (selectedAlgo === "greedy" ? result.greedyAssignments.length : result.hungarianSteps.length) : 0;

  return (
    <AuthGate>
      <GameLayoutShell
        gameId="02" 
        title="MinCost Assignment"
        description="Optimize worker-task allocation using PDSA algorithms (Hungarian vs Greedy)."
        controls={
          <div className="flex flex-col gap-6">
            <div className="space-y-4">
                <AlgoInput label="Size (N)" type="number" value={n} onChange={setN} required />
                <AlgoInput label="Min Cost" type="number" value={minCost} onChange={setMinCost} required />
                <AlgoInput label="Max Cost" type="number" value={maxCost} onChange={setMaxCost} required />
                
                <button
                onClick={runGame}
                disabled={isLoading}
                className="w-full bg-white text-black py-4 rounded-lg font-bold uppercase tracking-[0.2em] hover:bg-blue-600 hover:text-white transition-all text-[10px] disabled:opacity-30 shadow-lg"
                >
                {isLoading ? "CALCULATING..." : "GENERATE & RUN"}
                </button>
            </div>

            {result && (
              <motion.div 
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                className="pt-6 border-t border-white/5 space-y-6"
              >
                <AlgoDropdown 
                  label="Visualize Algorithm"
                  value={selectedAlgo}
                  onChange={(val) => {
                      setSelectedAlgo(val as any);
                      setCurrentStep(undefined);
                      setIsPlaying(false);
                  }}
                  options={[
                      { value: "greedy", label: "Greedy Strategy" },
                      { value: "hungarian", label: "Hungarian (Optimal)" },
                  ]}
                />

                <PlaybackToolbar 
                    currentStep={currentStep === undefined ? -1 : currentStep}
                    totalSteps={maxSteps}
                    isPlaying={isPlaying}
                    speed={speed}
                    onNext={() => {
                        setIsPlaying(false);
                        setCurrentStep(prev => prev === undefined ? 0 : Math.min(prev + 1, maxSteps - 1));
                    }}
                    onPrev={() => {
                        setIsPlaying(false);
                        setCurrentStep(prev => prev === undefined || prev === 0 ? 0 : prev - 1);
                    }}
                    onPlayToggle={() => setIsPlaying(!isPlaying)}
                    onStop={() => {
                        setIsPlaying(false);
                        setCurrentStep(undefined);
                    }}
                    onSpeedChange={setSpeed}
                />
              </motion.div>
            )}
          </div>
        }
        visualization={
          result ? (
            <div className="w-full h-full flex flex-col gap-8 overflow-auto pb-10 pr-2 custom-scrollbar">
              <div className="grid grid-cols-1 gap-8">
                <AnimatePresence mode="wait">
                    {selectedAlgo === "greedy" ? (
                        <motion.div 
                            key="greedy-view"
                            initial={{ opacity: 0, scale: 0.95 }}
                            animate={{ opacity: 1, scale: 1 }}
                            exit={{ opacity: 0, scale: 0.95 }}
                        >
                            <MatrixTable 
                                title={`Greedy Strategy: ${currentStep !== undefined ? `Choosing Step ${currentStep + 1}` : "Final Assignments"}`} 
                                matrix={result.matrix} 
                                assignments={result.greedyAssignments} 
                                currentStep={currentStep}
                                isGreedy={true}
                            />
                        </motion.div>
                    ) : (
                        <motion.div 
                            key="hungarian-view"
                            initial={{ opacity: 0, scale: 0.95 }}
                            animate={{ opacity: 1, scale: 1 }}
                            exit={{ opacity: 0, scale: 0.95 }}
                        >
                            <MatrixTable 
                                title={`Hungarian Optimization: ${currentStep !== undefined ? `Phase: ${result.hungarianSteps[currentStep].type}` : "Final Result"}`} 
                                matrix={result.matrix} 
                                assignments={result.hungarianAssignments} 
                                currentStep={currentStep}
                                hungarianStep={currentStep !== undefined ? result.hungarianSteps[currentStep] : undefined}
                            />
                        </motion.div>
                    )}
                </AnimatePresence>
              </div>

              {/* Day 6 & 7: Dual Logs & Study Section */}
              <div className="grid grid-cols-1 xl:grid-cols-2 gap-8">
                <div className="bg-[#050505] border border-white/5 rounded-2xl p-6 shadow-2xl">
                  <p className="text-[10px] uppercase tracking-[0.3em] text-blue-500 mb-4 font-bold">Execution Steps</p>
                  <div className="space-y-1 text-[11px] font-mono max-h-64 overflow-auto custom-scrollbar">
                    {selectedAlgo === "greedy" ? (
                        result.greedyLogs.map((line, index) => (
                            <div 
                              key={index} 
                              onClick={() => { setIsPlaying(false); setCurrentStep(index); }}
                              className={`p-2 rounded cursor-pointer transition-all border border-transparent ${
                                currentStep === index 
                                  ? "bg-blue-600/20 text-blue-200 border-blue-500/30 font-bold" 
                                  : "text-white/40 hover:bg-white/5 hover:text-white/60"
                              }`}
                            >
                              <span className="opacity-30 mr-2">[{index + 1}]</span> {line}
                            </div>
                        ))
                    ) : (
                        result.hungarianSteps.map((step, index) => (
                            <div 
                              key={index} 
                              onClick={() => { setIsPlaying(false); setCurrentStep(index); }}
                              className={`p-2 rounded cursor-pointer transition-all border border-transparent ${
                                currentStep === index 
                                  ? "bg-purple-600/20 text-purple-200 border-purple-500/30 font-bold" 
                                  : "text-white/40 hover:bg-white/5 hover:text-white/60"
                              }`}
                            >
                              <span className="opacity-30 mr-2">[{index + 1}]</span> {step.description}
                            </div>
                        ))
                    )}
                  </div>
                </div>

                <div className="bg-[#050505] border border-white/5 rounded-2xl p-6 shadow-2xl relative overflow-hidden group">
                  <div className="absolute top-0 right-0 p-4 opacity-10 group-hover:opacity-20 transition-opacity">
                    <svg className="w-12 h-12 text-green-500" fill="currentColor" viewBox="0 0 24 24">
                        <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"/>
                    </svg>
                  </div>
                  <p className="text-[10px] uppercase tracking-[0.3em] text-green-500 mb-4 font-bold">Algorithm Logic</p>
                  <div className="text-[11px] text-white/40 leading-relaxed font-mono space-y-4">
                    <div className="p-3 bg-white/3 rounded border border-white/5">
                        <p className="text-white/70 font-bold mb-1">Row/Col Reduction</p>
                        <p>This phase "normalizes" the costs. We subtract the minimum from each row and then column to force zeros into every part of the matrix.</p>
                    </div>
                    <div className="p-3 bg-white/3 rounded border border-white/5">
                        <p className="text-white/70 font-bold mb-1">Covering Lines</p>
                        <p>The system draws the minimum number of lines to cover all zeros. If lines &lt; N, we use the "Delta" value to adjust and find more zeros.</p>
                    </div>
                  </div>
                </div>
              </div>

              <div className="pt-8 border-t border-white/5">
                <MinCostAnalytics history={history} />
              </div>
            </div>
          ) : (
            <div className="flex flex-col items-center gap-4 text-white/10 h-full justify-center">
               <motion.div
                  animate={{ scale: [1, 1.1, 1] }}
                  transition={{ repeat: Infinity, duration: 2 }}
               >
                  <svg className="w-20 h-20" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={0.5} d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2-2v10a2 2 0 002 2z" />
                  </svg>
               </motion.div>
               <p className="text-[10px] uppercase tracking-[0.5em] font-mono">Algorithm Processor Idle</p>
            </div>
          )
        }
        results={
          <div className="space-y-4">
            <div className="flex justify-between items-center bg-white/5 p-4 rounded-lg border border-white/5 shadow-inner">
              <span className="text-[10px] font-mono text-white/40 uppercase">Greedy Total</span>
              <span className="text-xl font-black text-white">{result?.greedyTotalCost ?? "--"}</span>
            </div>

            <div className="flex justify-between items-center bg-white/5 p-4 rounded-lg border border-white/5 shadow-inner">
              <span className="text-[10px] font-mono text-white/40 uppercase">Hungarian Total</span>
              <span className="text-xl font-black text-green-400">{result?.hungarianTotalCost ?? "--"}</span>
            </div>

            <div className="flex justify-between items-center bg-green-600/10 p-3 rounded-lg border border-green-500/20">
              <span className="text-[10px] font-mono text-green-400 uppercase tracking-widest font-bold">Status</span>
              <span className="text-[10px] font-bold text-green-300">
                {result ? (result.hungarianTotalCost < result.greedyTotalCost ? "Hungarian Wins!" : "Greedy Match!") : "Waiting..."}
              </span>
            </div>

            <div className="grid grid-cols-2 gap-2 pt-4">
              <div className="text-center p-3 bg-blue-600/10 rounded-lg border border-blue-500/20">
                <p className="text-[9px] text-blue-400 uppercase font-bold mb-1">Greedy Time</p>
                <p className="text-xs font-mono text-white">{result?.greedyTimeMs ?? "--"}ms</p>
              </div>

              <div className="text-center p-3 bg-purple-600/10 rounded-lg border border-purple-500/20">
                <p className="text-[9px] text-purple-400 uppercase font-bold mb-1">Hungarian Time</p>
                <p className="text-xs font-mono text-white">{result?.hungarianTimeMs ?? "--"}ms</p>
              </div>
            </div>

            {/* Recent Session List */}
            {history.length > 0 && (
              <div className="mt-8 pt-8 border-t border-white/5">
                <p className="text-[10px] font-mono text-white/30 uppercase tracking-widest mb-4">Previous Runs</p>
                <div className="space-y-2 max-h-40 overflow-auto pr-2 custom-scrollbar">
                  {history.slice(0, 5).map((item, idx) => (
                    <div key={idx} className="flex justify-between items-center p-2 rounded bg-white/[0.02] border border-white/5 hover:bg-white/5 transition-colors">
                      <div className="flex flex-col">
                        <span className="text-[8px] text-white/20 font-mono italic">
                          {new Date(item.date).toLocaleTimeString()}
                        </span>
                        <span className="text-[10px] text-white/60">Matrix {item.n}x{item.n}</span>
                      </div>
                      <span className="text-xs font-bold text-green-400/70">{item.cost}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        }
      />

      <AlgoNotify
        show={notify.show}
        type={notify.type}
        title={notify.title}
        message={notify.message}
        onClose={() => setNotify((prev) => ({ ...prev, show: false }))}
      />
    </AuthGate>
  );
}

