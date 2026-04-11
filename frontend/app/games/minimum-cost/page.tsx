"use client";

import { useState, useEffect } from "react";
// These are our project components for layout, auth protection, and inputs
import GameLayoutShell from "../../../components/layouts/GameLayoutShell";
import AuthGate from "../../../components/auth/AuthGate";
import AlgoInput from "../../../components/forms/AlgoInput";
import AlgoNotify, { NotifyType } from "../../../components/forms/AlgoNotify";

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
 * We get the matrix, total costs for both algos, and their assignment steps.
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

// Shape of a past game record
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
  currentStep, // Which assignment step are we showing?
  isGreedy,    // If true, we dim the cells that were skipped/blocked
}: {
  title: string;
  matrix: number[][];
  assignments: Assignment[];
  currentStep?: number;
  isGreedy?: boolean;
}) {
  // If user hasn't clicked "Step-by-Step", we show the full final result.
  const activeStep = currentStep !== undefined ? currentStep : assignments.length - 1;
  const showStepByStep = currentStep !== undefined;

  // These sets help us know which rows/cols were "blocked" in greedy logic
  const blockedRows = new Set<number>();
  const blockedCols = new Set<number>();
  
  // Fill blocked lists based on previous steps (only relevant for greedy visualization)
  if (showStepByStep && isGreedy) {
    for (let i = 0; i < activeStep; i++) {
      blockedRows.add(assignments[i].row);
      blockedCols.add(assignments[i].col);
    }
  }

  // Helper to quickly check if a cell was picked in the current steps
  const assignmentMap = new Map<string, boolean>();
  assignments.slice(0, activeStep + 1).forEach((a) => {
    assignmentMap.set(`${a.row}-${a.col}`, true);
  });

  return (
    <div className="w-full">
      <p className="text-[10px] uppercase tracking-widest text-white/30 mb-2 px-1 font-mono">
        {title}
      </p>
      {/* Container with scroll hack for huge 100x100 matrices */}
      <div className="overflow-auto max-h-[420px] border border-white/5 rounded-lg bg-[#0A0A0A] custom-scrollbar">
        <table className="w-full text-[10px] border-collapse">
          <tbody>
            {matrix.map((row, rowIndex) => (
              <tr key={rowIndex}>
                {row.map((value, colIndex) => {
                  const cellKey = `${rowIndex}-${colIndex}`;
                  const isAssigned = assignmentMap.has(cellKey);
                  // currentPick = The very last cell chosen in step-by-step
                  const isCurrentPick = showStepByStep && assignments[activeStep]?.row === rowIndex && assignments[activeStep]?.col === colIndex;
                  
                  // isBlocked = This cell can no longer be picked because its row/col is taken
                  const isBlocked = showStepByStep && isGreedy && (blockedRows.has(rowIndex) || blockedCols.has(colIndex));

                  // Logic for choosing cell colors based on its state
                  let cellClass = "border border-white/5 p-2 text-center transition-all duration-300 ";
                  if (isCurrentPick) {
                    // Purple = This is the cell being picked RIGHT NOW
                    cellClass += "bg-purple-500/40 text-purple-200 font-bold scale-110 relative z-10 shadow-lg shadow-purple-500/20";
                  } else if (isAssigned) {
                    // Green = This cell was already picked in a previous step
                    cellClass += "bg-green-500/20 text-green-300 font-medium";
                  } else if (isBlocked) {
                    // Dimmed = This cell is blocked (PDSA logic: row/col already used)
                    cellClass += "opacity-10 text-white/20";
                  } else {
                    // Generic color for free cells
                    cellClass += "text-white/40";
                  }

                  return (
                    <td key={cellKey} className={cellClass}>
                      {value}
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default function MinimumCostPage() {
  // Input states: we keep these as strings so typing feels natural
  const [n, setN] = useState("10"); 
  const [minCost, setMinCost] = useState("20");
  const [maxCost, setMaxCost] = useState("200");
  
  // App states: loading spinner status and backend results
  const [isLoading, setIsLoading] = useState(false);
  const [result, setResult] = useState<ApiResponse | null>(null);
  const [history, setHistory] = useState<HistoryItem[]>([]);
  
  // Visualization state: which step we are looking at
  const [currentStep, setCurrentStep] = useState<number | undefined>(undefined);
  
  // Notification system: shown for errors or success
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

  // Effect: whenever we get new results, reset the visualizer back to full view
  // Also fetch history on load
  useEffect(() => {
    if (result) setCurrentStep(undefined);
    fetchHistory();
  }, [result]);

  // Fetches the last 10 games from the database
  const fetchHistory = async () => {
    try {
      const response = await fetch("http://localhost:8080/api/mincost/history");
      const data = await response.json();
      if (response.ok) setHistory(data);
    } catch (err) {
      console.log("History failed to load");
    }
  };

  // Main Logic: Sends input to backend and gets the result
  const runGame = async () => {
    // 1. Convert our inputs to numbers
    const nValue = Number(n);
    const minValue = Number(minCost);
    const maxValue = Number(maxCost);

    // 2. BASIC VALIDATION (Student Level - Check ranges for viva)
    if (!Number.isInteger(nValue) || nValue < 5 || nValue > 100) {
      setNotify({ 
        show: true, 
        type: "error", 
        title: "Input Error", 
        message: "Size (N) must be a whole number between 5 and 100!" 
      });
      return;
    }
    
    // Check if costs make sense (Min must be less than Max)
    if (minValue < 1 || maxValue < minValue) {
      setNotify({ 
        show: true, 
        type: "error", 
        title: "Bad Range", 
        message: "Cost Min must be > 0 and cannot be larger than Max cost!" 
      });
      return;
    }

    // 3. START LOADING
    setIsLoading(true);
    
    try {
      // 4. FETCH DATA FROM SPRING BOOT
      const response = await fetch("http://localhost:8080/api/mincost/start", {
        method: "POST",
        headers: { 
          "Content-Type": "application/json" 
        },
        body: JSON.stringify({ 
          n: nValue, 
          minCost: minValue, 
          maxCost: maxValue 
        }),
      });
      
      const data = await response.json();
      
      // If backend throws an error (like N > 100), we handle it here
      if (!response.ok) {
        throw new Error(data.error || "Internal server error occurred!");
      }

      // 5. UPDATE UI WITH RESULTS
      setResult(data);
      setNotify({ 
        show: true, 
        type: "success", 
        title: "Matrix Generated", 
        message: "Both algorithms ran successfully! Check the graphs below." 
      });

    } catch (error) {
      // Handle network or system failures
      setNotify({
        show: true,
        type: "error",
        title: "System Fault",
        message: error instanceof Error ? error.message : "System seems offline. Try starting the backend!",
      });
    } finally {
      // Always stop loading spinner
      setIsLoading(false);
    }
  };

  /* Visualization Navigation Logic */
  
  // Go to next step in greedy animation
  const nextStepGreedy = () => {
    if (!result) return;
    const maxSteps = result.greedyAssignments.length;
    setCurrentStep((prev) => (prev === undefined ? 0 : Math.min(prev + 1, maxSteps - 1)));
  };

  // Go to previous step in greedy animation
  const prevStepGreedy = () => {
    setCurrentStep((prev) => (prev === undefined || prev === 0 ? 0 : prev - 1));
  };

  // Check which algorithm actually won (Lowest total cost)
  const optimalAlgorithm =
    (result?.greedyTotalCost ?? Infinity) <= (result?.hungarianTotalCost ?? Infinity)
      ? "Greedy"
      : "Hungarian";

  return (
    <AuthGate>
      <GameLayoutShell
        gameId="02" 
        title="MinCost Assignment"
        description="Connect with backend to find optimal task assignment using Hungarian and Greedy algorithms."
        controls={
          <>
            {/* Input fields with validation triggers */}
            <AlgoInput label="Size (N)" type="number" value={n} onChange={setN} required />
            <AlgoInput label="Min Cost" type="number" value={minCost} onChange={setMinCost} required />
            <AlgoInput label="Max Cost" type="number" value={maxCost} onChange={setMaxCost} required />
            
            {/* Execute Button with Loading Spinner */}
            <button
              onClick={runGame}
              disabled={isLoading}
              className="relative flex items-center justify-center bg-white text-black py-4 rounded-lg font-bold uppercase tracking-[0.2em] hover:bg-blue-600 hover:text-white transition-all text-[10px] disabled:opacity-30 shadow-lg active:scale-95 overflow-hidden group"
            >
              {isLoading ? (
                <>
                  <svg className="animate-spin -ml-1 mr-3 h-4 w-4 text-black group-hover:text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  <span>Calculating...</span>
                </>
              ) : (
                "GENERATE & RUN"
              )}
            </button>

            {/* Step-by-Step Logic Controls (shown only after first run) */}
            {result && (
              <div className="mt-8 pt-8 border-t border-white/5 space-y-4">
                <p className="text-[10px] font-mono text-white/30 uppercase tracking-widest">Visualizer Controls</p>
                <div className="flex gap-2">
                  <button 
                    onClick={prevStepGreedy}
                    className="flex-1 bg-white/5 hover:bg-white/10 p-3 rounded text-[10px] font-bold transition-all border border-white/5"
                  >
                    PREV
                  </button>
                  <button 
                    onClick={nextStepGreedy}
                    className="flex-1 bg-blue-600 text-white p-3 rounded text-[10px] font-bold transition-all shadow-lg active:scale-95"
                  >
                    {currentStep === undefined ? "START STEPS" : "NEXT STEP"}
                  </button>
                </div>
                {currentStep !== undefined && (
                  <button 
                    onClick={() => setCurrentStep(undefined)}
                    className="w-full text-[9px] text-white/40 hover:text-red-400 transition-colors uppercase tracking-widest"
                  >
                    Reset View
                  </button>
                )}
              </div>
            )}
          </>
        }
        visualization={
          result ? (
            <div className="w-full h-full flex flex-col gap-8 overflow-auto pb-10">
              {/* Display Matrix area: Greedy vs Hungarian */}
              <div className="grid grid-cols-1 xl:grid-cols-2 gap-8">
                <MatrixTable 
                  title={`Greedy Strategy Picks ${currentStep !== undefined ? `(Step ${currentStep + 1})` : "(Full View)"}`} 
                  matrix={result.matrix} 
                  assignments={result.greedyAssignments} 
                  currentStep={currentStep}
                  isGreedy={true}
                />
                <MatrixTable 
                  title="Hungarian (Optimal Global Result)" 
                  matrix={result.matrix} 
                  assignments={result.hungarianAssignments} 
                />
              </div>

              {/* Log Panels: Explain what the algorithms did in simple terms */}
              <div className="grid grid-cols-1 xl:grid-cols-2 gap-8">
                <div className="bg-[#050505] border border-white/5 rounded-xl p-6 shadow-2xl overflow-hidden">
                  <p className="text-[10px] uppercase tracking-[0.3em] text-blue-500 mb-4 font-bold">Execution Steps</p>
                  <div className="space-y-1 text-[11px] font-mono max-h-48 overflow-auto custom-scrollbar">
                    {result.greedyLogs.map((line, index) => (
                      <div 
                        key={index} 
                        onClick={() => setCurrentStep(index)}
                        className={`p-2 rounded cursor-pointer transition-all border border-transparent ${
                          currentStep === index 
                            ? "bg-blue-600/20 text-blue-200 border-blue-500/30 font-bold" 
                            : "text-white/40 hover:bg-white/5 hover:text-white/60"
                        }`}
                      >
                        <span className="opacity-30 mr-2">[{index + 1}]</span> {line}
                      </div>
                    ))}
                  </div>
                </div>

                <div className="bg-[#050505] border border-white/5 rounded-xl p-6 shadow-2xl">
                  <p className="text-[10px] uppercase tracking-[0.3em] text-green-500 mb-4 font-bold">Concept Summary</p>
                  <div className="text-[11px] text-white/40 leading-relaxed font-mono">
                    <p className="mb-3 italic">
                      Notes for Viva:
                    </p>
                    <p className="mb-3">
                      <span className="text-white/60 font-bold">GREEDY:</span> Fast but "selfish". Picks the cheapest cell it sees right now.
                    </p>
                    <p>
                      <span className="text-white/60 font-bold">HUNGARIAN:</span> Smart but slower. Uses matrix reduction (PDSA class) to find truly best price.
                    </p>
                  </div>
                </div>
              </div>
            </div>
          ) : (
            /* Show this before we run the game */
            <div className="flex flex-col items-center gap-4 text-white/20 h-full justify-center">
               <svg className="w-16 h-16 opacity-10 animate-pulse" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
               </svg>
               <p className="text-xs uppercase tracking-[0.4em] font-mono">System Standby: Click Run</p>
            </div>
          )
        }
        results={
          /* This shows the small result cards on the right */
          <div className="space-y-4">
            <div className="flex justify-between items-center bg-white/5 p-4 rounded-lg border border-white/5">
              <span className="text-[10px] font-mono text-white/40 uppercase">Greedy Tool Total</span>
              <span className="text-xl font-black text-white">{result?.greedyTotalCost ?? "--"}</span>
            </div>

            <div className="flex justify-between items-center bg-white/5 p-4 rounded-lg border border-white/5">
              <span className="text-[10px] font-mono text-white/40 uppercase">Optimal (Hungarian)</span>
              <span className="text-xl font-black text-green-400">{result?.hungarianTotalCost ?? "--"}</span>
            </div>

            <div className="flex justify-between items-center bg-green-600/10 p-3 rounded-lg border border-green-500/20">
              <span className="text-[10px] font-mono text-green-400 uppercase">Status</span>
              <span className="text-[10px] font-bold text-green-300">
                {result ? (result.hungarianTotalCost < result.greedyTotalCost ? "Optimal Wins!" : "Greedy matched Optimal!") : "--"}
              </span>
            </div>

            {/* Performance metrics section */}
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

            {/* History Table (New) */}
            {history.length > 0 && (
              <div className="mt-8 pt-8 border-t border-white/5">
                <p className="text-[10px] font-mono text-white/30 uppercase tracking-widest mb-4">Recent System Runs</p>
                <div className="space-y-2 max-h-40 overflow-auto pr-2 custom-scrollbar">
                  {history.map((item, idx) => (
                    <div key={idx} className="flex justify-between items-center p-2 rounded bg-white/[0.02] border border-white/5">
                      <div className="flex flex-col">
                        <span className="text-[8px] text-white/20 font-mono italic">
                          {new Date(item.date).toLocaleDateString()}
                        </span>
                        <span className="text-[10px] text-white/60">N={item.n}</span>
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

      {/* Professional Popups: Using our system-generated UI, not alert() */}
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
