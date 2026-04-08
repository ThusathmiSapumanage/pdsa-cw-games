"use client";

import { useState } from "react";
import GameLayoutShell from "../../../components/layouts/GameLayoutShell";
import AlgoInput from "../../../components/forms/AlgoInput";
import AlgoNotify, { NotifyType } from "../../../components/forms/AlgoNotify";

// One selected assignment (task -> employee).
type Assignment = {
  row: number;
  col: number;
  cost: number;
};

// Expected response from backend API.
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

// Reusable matrix table UI.
// It highlights selected cells for one algorithm.
function MatrixTable({
  title,
  matrix,
  assignments,
}: {
  title: string;
  matrix: number[][];
  assignments: Assignment[];
}) {
  // Makes lookup fast when checking if a cell is selected.
  const assignmentSet = new Set(assignments.map((a) => `${a.row}-${a.col}`));

  return (
    <div className="w-full">
      <h4 className="text-xs font-mono uppercase tracking-[0.2em] text-white/50 mb-3">{title}</h4>
      <div className="overflow-auto max-h-[420px] border border-white/10 rounded-lg">
        <table className="w-full text-xs md:text-sm border-collapse">
          <tbody>
            {matrix.map((row, rowIndex) => (
              <tr key={rowIndex}>
                {row.map((value, colIndex) => {
                  // This checks if current cell is in selected assignments.
                  const isPicked = assignmentSet.has(`${rowIndex}-${colIndex}`);
                  return (
                    <td
                      key={`${rowIndex}-${colIndex}`}
                      className={`border border-white/5 p-2 text-center min-w-[42px] ${
                        isPicked ? "bg-green-500/20 text-green-300 font-bold" : "text-white/70"
                      }`}
                    >
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
  // Input states.
  const [n, setN] = useState("50");
  const [minCost, setMinCost] = useState("20");
  const [maxCost, setMaxCost] = useState("200");
  // UI states.
  const [isLoading, setIsLoading] = useState(false);
  const [result, setResult] = useState<ApiResponse | null>(null);
  // Notification popup state.
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

  // Called when user clicks Execute Round.
  const runGame = async () => {
    // Convert input text to numbers.
    const nValue = Number(n);
    const minValue = Number(minCost);
    const maxValue = Number(maxCost);

    // Simple validation for N.
    if (!Number.isInteger(nValue) || nValue < 5 || nValue > 100) {
      setNotify({
        show: true,
        type: "error",
        title: "Invalid N",
        message: "N must be a whole number between 5 and 100.",
      });
      return;
    }
    // Simple validation for cost range.
    if (!Number.isInteger(minValue) || !Number.isInteger(maxValue) || minValue < 1 || minValue > maxValue) {
      setNotify({
        show: true,
        type: "error",
        title: "Invalid cost range",
        message: "Use positive numbers and keep min <= max.",
      });
      return;
    }

    setIsLoading(true);
    try {
      // Call backend API and start one round.
      const response = await fetch("http://localhost:8080/api/mincost/start", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          n: nValue,
          minCost: minValue,
          maxCost: maxValue,
        }),
      });
      // Read API response JSON.
      const data = await response.json();

      // If backend says error, show it.
      if (!response.ok) {
        throw new Error(data.error || "Server error");
      }

      // Save result so matrix + stats can render.
      setResult(data);
      // Show success popup.
      setNotify({
        show: true,
        type: "success",
        title: "Round complete",
        message: "Both algorithms finished. You can now compare them.",
      });
    } catch (error) {
      // Show friendly error popup.
      setNotify({
        show: true,
        type: "error",
        title: "Run failed",
        message: error instanceof Error ? error.message : "Could not connect to backend.",
      });
    } finally {
      // Stop loading spinner state.
      setIsLoading(false);
    }
  };

  return (
    <>
      <GameLayoutShell
        gameId="01"
        title="Minimum Cost Problem"
        description="Compare Greedy and Hungarian algorithms for task assignment."
        controls={
          <>
            {/* User inputs for one game round */}
            <AlgoInput label="Matrix Size (N)" type="number" value={n} onChange={setN} required />
            <AlgoInput label="Minimum Cost" type="number" value={minCost} onChange={setMinCost} required />
            <AlgoInput label="Maximum Cost" type="number" value={maxCost} onChange={setMaxCost} required />
            <button
              onClick={runGame}
              disabled={isLoading}
              className="bg-white text-black py-3 rounded font-bold uppercase tracking-widest hover:bg-blue-600 hover:text-white transition-colors text-xs disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isLoading ? "Running..." : "Execute Round"}
            </button>
          </>
        }
        visualization={
          result ? (
            <div className="w-full h-full flex flex-col gap-6 overflow-auto">
              {/* Two side-by-side matrix views for easy compare */}
              <div className="grid grid-cols-1 xl:grid-cols-2 gap-6">
                <MatrixTable title="Greedy Picks" matrix={result.matrix} assignments={result.greedyAssignments} />
                <MatrixTable
                  title="Hungarian Picks (Optimal)"
                  matrix={result.matrix}
                  assignments={result.hungarianAssignments}
                />
              </div>
              {/* Logs area to explain algorithm steps */}
              <div className="grid grid-cols-1 xl:grid-cols-2 gap-6">
                <div className="bg-black/20 border border-white/10 rounded-lg p-4">
                  <p className="text-xs uppercase tracking-[0.2em] text-white/40 mb-2">Greedy Steps</p>
                  <div className="space-y-2 text-xs text-white/70 max-h-36 overflow-auto">
                    {result.greedyLogs.map((line, index) => (
                      <p key={index}>{line}</p>
                    ))}
                  </div>
                </div>
                <div className="bg-black/20 border border-white/10 rounded-lg p-4">
                  <p className="text-xs uppercase tracking-[0.2em] text-white/40 mb-2">Hungarian Notes</p>
                  <div className="space-y-2 text-xs text-white/70 max-h-36 overflow-auto">
                    {result.hungarianLogs.map((line, index) => (
                      <p key={index}>{line}</p>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          ) : (
            // Shown before first run.
            <div className="text-center text-white/50">Run one round to see matrix and assignments.</div>
          )
        }
        results={
          // Summary panel for totals + execution times.
          <div className="flex flex-col gap-3 text-sm">
            <p className="text-white/70">Status: {result ? <span className="text-green-400">Completed</span> : "Waiting"}</p>
            <p className="text-white/70">
              Greedy Total: <span className="text-white font-semibold">{result?.greedyTotalCost ?? "--"}</span>
            </p>
            <p className="text-white/70">
              Hungarian Total: <span className="text-white font-semibold">{result?.hungarianTotalCost ?? "--"}</span>
            </p>
            <p className="text-white/70">
              Greedy Time: <span className="text-blue-400 font-semibold">{result?.greedyTimeMs ?? "--"} ms</span>
            </p>
            <p className="text-white/70">
              Hungarian Time: <span className="text-blue-400 font-semibold">{result?.hungarianTimeMs ?? "--"} ms</span>
            </p>
          </div>
        }
      />
      <AlgoNotify
        show={notify.show}
        type={notify.type}
        title={notify.title}
        message={notify.message}
        // Close popup manually or on timer.
        onClose={() => setNotify((prev) => ({ ...prev, show: false }))}
      />
    </>
  );
}
