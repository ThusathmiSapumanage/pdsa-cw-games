"use client";

import { useState, useEffect } from "react";
import GameLayoutShell from "../../../components/layouts/GameLayoutShell";
import AuthGate from "../../../components/auth/AuthGate";
import AlgoInput from "../../../components/forms/AlgoInput";
import AlgoNotify, { NotifyType } from "../../../components/forms/AlgoNotify";

// One task-to-employee pair. 
// row = task index, col = employee index.
type Assignment = {
  row: number;
  col: number;
  cost: number;
};

// Shape of the data we get from our Spring Boot backend.
type ApiResponse = {
  matrix: number[][];
  greedyTotalCost: number;
  hungarianTotalCost: number;
  greedyAssignments: Assignment[];
  hungarianAssignments: Assignment[];
  greedyLogs: string[];
  hungarianLogs: string[];
  greedyTimeMs: number;
  hungarianTimeMs: number;
};

// This is our matrix grid component.
// It shows values and handles the special "Day 2" highlights.
function MatrixTable({
  title,
  matrix,
  assignments,
  currentStep, // NEW: which step we are looking at in greedy
  isGreedy,    // NEW: so we know if we apply blocking logic
}: {
  title: string;
  matrix: number[][];
  assignments: Assignment[];
  currentStep?: number;
  isGreedy?: boolean;
}) {
  // Step logic: if currentStep is undefined, we show everything (normal mode).
  // If defined, we only show up to that step.
  const activeStep = currentStep !== undefined ? currentStep : assignments.length - 1;
  const showStepByStep = currentStep !== undefined;

  // Track which rows/cols are blocked BEFORE the current step.
  const blockedRows = new Set<number>();
  const blockedCols = new Set<number>();
  
  // Fill blocked sets based on previous steps in greedy.
  if (showStepByStep && isGreedy) {
    for (let i = 0; i < activeStep; i++) {
      blockedRows.add(assignments[i].row);
      blockedCols.add(assignments[i].col);
    }
  }

  // Quick lookup for assigned cells.
  const assignmentMap = new Map<string, boolean>();
  assignments.slice(0, activeStep + 1).forEach((a) => {
    assignmentMap.set(`${a.row}-${a.col}`, true);
  });

  return (
    <div className="w-full">
      {/* Title with some spacing */}
      {/* Scrollable container for large matrices */}
      <div className="overflow-auto max-h-[420px] border border-white/5 rounded-lg bg-[#0A0A0A]">
        <table className="w-full text-xs border-collapse">
          <tbody>
            {matrix.map((row, rowIndex) => (
              <tr key={rowIndex}>
                {row.map((value, colIndex) => {
                  const cellKey = `${rowIndex}-${colIndex}`;
                  const isAssigned = assignmentMap.has(cellKey);
                  const isCurrentPick = showStepByStep && assignments[activeStep]?.row === rowIndex && assignments[activeStep]?.col === colIndex;
                  
                  // Check if this row or col is already blocked (for greedy dimming).
                  const isBlocked = showStepByStep && isGreedy && (blockedRows.has(rowIndex) || blockedCols.has(colIndex));

                  // Style logic:
                  // 1. Current pick = Purple highlight.
                  // 2. Past pick = Green highlight.
                  // 3. Blocked = Very dim (opacity).
                  // 4. Default = Normal text.
                  let cellClass = "border border-white/5 p-2 text-center transition-all duration-300 ";
                  if (isCurrentPick) {
                    cellClass += "bg-purple-500/40 text-purple-200 font-bold scale-110 relative z-10 shadow-lg shadow-purple-500/20";
                  } else if (isAssigned) {
                    cellClass += "bg-green-500/20 text-green-300 font-medium";
                  } else if (isBlocked) {
                    cellClass += "opacity-10 text-white/20";
                  } else {
                    cellClass += "text-white/50";
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
  // Input states for N and costs.
  const [n, setN] = useState("10"); // Default small for easier testing
  const [minCost, setMinCost] = useState("20");
  const [maxCost, setMaxCost] = useState("200");
  
  // Logic states.
  const [isLoading, setIsLoading] = useState(false);
  const [result, setResult] = useState<ApiResponse | null>(null);
  
  // NEW: Track current step for greedy visualization.
  const [currentStep, setCurrentStep] = useState<number | undefined>(undefined);
  
  // Notification system state.
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

  // Reset step-by-step when new results come in.
  useEffect(() => {
    if (result) setCurrentStep(undefined);
  }, [result]);

  // This is the main function to trigger our game logic
  const runGame = async () => {
    // Basic numbers conversion
    const nValue = Number(n);
    const minValue = Number(minCost);
    const maxValue = Number(maxCost);

    // Initial validation check before we call backend
    if (!Number.isInteger(nValue) || nValue < 5 || nValue > 100) {
      setNotify({ 
        show: true, 
        type: "error", 
        title: "Input Error", 
        message: "Size (N) must be a whole number between 5 and 100!" 
      });
      return;
    }

    // Set loading to true so user knows something is happening
    setIsLoading(true);
    
    try {
      // Connect to our Spring Boot backend using a POST request
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
      
      // Get the JSON data from our response
      const data = await response.json();
      
      // If the backend sent an error, we throw it to catch block
      if (!response.ok) {
        throw new Error(data.error || "Backend server failed to process request");
      }

      // If all good, we save the result and show a success popup
      setResult(data);
      setNotify({ 
        show: true, 
        type: "success", 
        title: "Game Started", 
        message: "Matrix generated and strategies executed successfully!" 
      });

    } catch (error) {
      // If network is down or server crashed, we show this error
      setNotify({
        show: true,
        type: "error",
        title: "System Error",
        message: error instanceof Error ? error.message : "Cannot reach the backend server.",
      });
    } finally {
      // Always stop loading at the end
      setIsLoading(false);
    }
  };

  // Step-by-step navigation helpers.
  const nextStepGreedy = () => {
    if (!result) return;
    const maxSteps = result.greedyAssignments.length;
    setCurrentStep((prev) => (prev === undefined ? 0 : Math.min(prev + 1, maxSteps - 1)));
  };

  const prevStepGreedy = () => {
    setCurrentStep((prev) => (prev === undefined || prev === 0 ? 0 : prev - 1));
  };

  const optimalAlgorithm =
    (result?.greedyTotalCost ?? Infinity) <= (result?.hungarianTotalCost ?? Infinity)
      ? "Greedy"
      : "Hungarian";

  return (
    <AuthGate>
      <GameLayoutShell
        gameId="02" 
        title="MinCost Assignment"
        description="Day 2: Implementing Visual Highlighting and Blocked Row/Column dimming for Greedy strategy."
        controls={
          <>
            {/* Input fields for the game parameters */}
            <AlgoInput label="Size (N)" type="number" value={n} onChange={setN} required />
            <AlgoInput label="Min Cost" type="number" value={minCost} onChange={setMinCost} required />
            <AlgoInput label="Max Cost" type="number" value={maxCost} onChange={setMaxCost} required />
            
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
                  <span>Running Algorithms...</span>
                </>
              ) : (
                "GENERATE & RUN"
              )}
            </button>

            {/* NEW: Step-by-Step UI Controls */}
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
                    Reset to Full View
                  </button>
                )}
              </div>
            )}
          </>
        }
        visualization={
          result ? (
            <div className="w-full h-full flex flex-col gap-8 overflow-auto pb-10">
              {/* Matrix display area */}
              <div className="grid grid-cols-1 xl:grid-cols-2 gap-8">
                <MatrixTable 
                  title={`Greedy Picks ${currentStep !== undefined ? `(Step ${currentStep + 1})` : "(Final View)"}`} 
                  matrix={result.matrix} 
                  assignments={result.greedyAssignments} 
                  currentStep={currentStep}
                  isGreedy={true}
                />
                <MatrixTable 
                  title="Hungarian (Optimal Result)" 
                  matrix={result.matrix} 
                  assignments={result.hungarianAssignments} 
                />
              </div>

              {/* Log Panels (INTERACTIVE) */}
              <div className="grid grid-cols-1 xl:grid-cols-2 gap-8">
                {/* Greedy Logs */}
                <div className="bg-[#050505] border border-white/5 rounded-xl p-6 shadow-2xl relative overflow-hidden group">
                  <div className="absolute top-0 right-0 w-24 h-24 bg-blue-500/5 blur-3xl rounded-full" />
                  <p className="text-[10px] uppercase tracking-[0.3em] text-blue-500 mb-4 font-bold">Greedy Logic Steps</p>
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

                {/* Info Panel for students (Helpful for VIVA) */}
                <div className="bg-[#050505] border border-white/5 rounded-xl p-6 shadow-2xl">
                  <p className="text-[10px] uppercase tracking-[0.3em] text-green-500 mb-4 font-bold">Comparison Logic</p>
                  <div className="text-[11px] text-white/40 leading-relaxed font-mono">
                    <p className="mb-3">
                      <span className="text-white/60 font-bold">GREEDY:</span> Picks the local minimum in each step. 
                      Once a cell is chosen, it blocks that <span className="text-white/60">Task (Row)</span> and 
                      <span className="text-white/60">Employee (Column)</span>.
                    </p>
                    <p>
                      <span className="text-white/60 font-bold">HUNGARIAN:</span> Looks at the global matrix to find 
                      the lowest overall total. It usually beats Greedy in big matrices.
                    </p>
                  </div>
                </div>
              </div>
            </div>
          ) : (
            /* Placeholder before run */
            <div className="flex flex-col items-center gap-4 text-white/20">
               <svg className="w-16 h-16 opacity-30 animate-pulse" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M9 17V7m0 10a2 2 0 01-2 2H5a2 2 0 01-2-2V7a2 2 0 012-2h2a2 2 0 012 2m0 10a2 2 0 002 2h2a2 2 0 002-2M9 7a2 2 0 012-2h2a2 2 0 012 2m0 10V7m0 10a2 2 0 002 2h2a2 2 0 002-2V7a2 2 0 00-2-2h-2a2 2 0 00-2 2" />
               </svg>
               <p className="text-xs uppercase tracking-[0.4em] font-mono">Standby: No Matrix Generated</p>
            </div>
          )
        }
        results={
          <div className="space-y-4">
            <div className="flex justify-between items-center bg-white/5 p-4 rounded-lg border border-white/5">
              <span className="text-[10px] font-mono text-white/40 uppercase">
                Greedy Total
              </span>
              <span className="text-xl font-black text-white">
                {result?.greedyTotalCost ?? "--"}
              </span>
            </div>

            <div className="flex justify-between items-center bg-white/5 p-4 rounded-lg border border-white/5">
              <span className="text-[10px] font-mono text-white/40 uppercase">
                Optimal Total
              </span>
              <span className="text-xl font-black text-green-400">
                {result?.hungarianTotalCost ?? "--"}
              </span>
            </div>

            <div className="flex justify-between items-center bg-green-600/10 p-3 rounded-lg border border-green-500/20">
              <span className="text-[10px] font-mono text-green-400 uppercase">
                Optimal Algorithm
              </span>
              <span className="text-sm font-bold text-green-300">
                {result ? optimalAlgorithm : "--"}
              </span>
            </div>

            <div className="grid grid-cols-2 gap-2 pt-4">
              <div className="text-center p-3 bg-blue-600/10 rounded-lg border border-blue-500/20">
                <p className="text-[9px] text-blue-400 uppercase font-bold mb-1">
                  Greedy Time
                </p>
                <p className="text-xs font-mono text-white">
                  {result?.greedyTimeMs ?? "--"}ms
                </p>
              </div>

              <div className="text-center p-3 bg-purple-600/10 rounded-lg border border-purple-500/20">
                <p className="text-[9px] text-purple-400 uppercase font-bold mb-1">
                  Hungarian Time
                </p>
                <p className="text-xs font-mono text-white">
                  {result?.hungarianTimeMs ?? "--"}ms
                </p>
              </div>
            </div>
          </div>
        }
      />

      {/* Professional Popups (System style, no localhost messages) */}
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
