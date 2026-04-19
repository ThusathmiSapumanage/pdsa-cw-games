"use client";

import { useCallback, useState } from "react";
import GameLayoutShell from "../layouts/GameLayoutShell";
import AuthGate from "../auth/AuthGate";
import QueensBoard from "./QueensBoard";
import PerformanceStats from "./PerformanceStats";
import PerformanceRunChart from "../common/PerformanceRunChart";
import SolutionProgress from "./SolutionProgress";
import { Position, StartResponse, SubmitResponse } from "./types";
import { startQueensRound, submitQueensAnswer } from "../../src/lib/QueensApi";

const BOARD_SIZE = 16;
const MAX_BENCHMARK_POINTS = 12;

type QueenCountOption = 8;

type BenchmarkRow = {
  round: number;
  sequential: number;
  threaded: number;
  totalSolutions: number;
};

type GamePhase =
  | "idle"
  | "starting"
  | "ready"
  | "submitting"
  | "result"
  | "complete";

export default function QueensPuzzleGame() {
  const [phase, setPhase] = useState<GamePhase>("idle");
  const queenCount: QueenCountOption = 8;
  const [activeQueenCount, setActiveQueenCount] = useState<QueenCountOption>(8);
  const [roundData, setRoundData] = useState<StartResponse | null>(null);
  const [queens, setQueens] = useState<Position[]>([]);
  const [lastResult, setLastResult] = useState<SubmitResponse | null>(null);
  const [discovered, setDiscovered] = useState(0);
  const [totalSolutions, setTotalSolutions] = useState(0);
  const [error, setError] = useState("");
  const [benchmarkData, setBenchmarkData] = useState<BenchmarkRow[]>([]);
  const [loadingBenchmark, setLoadingBenchmark] = useState(false);

  const formatMs = (value: number) => {
    if (!Number.isFinite(value)) return "0.000";
    return value.toFixed(3);
  };

  const handleStartRound = useCallback(async () => {
    setError("");
    setLastResult(null);
    setQueens([]);
    setPhase("starting");

    try {
      const response = await startQueensRound({
        boardSize: BOARD_SIZE,
        queenCount,
      });
      setRoundData(response);
      setActiveQueenCount(queenCount);
      setTotalSolutions(response.totalSolutions);
      setDiscovered(0);
      setBenchmarkData((prev) => {
        const nextRound = prev.length + 1;
        const next = [
          ...prev,
          {
            round: nextRound,
            sequential: response.sequentialTimeMs,
            threaded: response.threadedTimeMs,
            totalSolutions: response.totalSolutions,
          },
        ];
        return next.slice(-MAX_BENCHMARK_POINTS);
      });
      setPhase("ready");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to start round.");
      setPhase("idle");
    }
  }, [queenCount]);

  const handleCellClick = useCallback(
    (x: number, y: number) => {
      if (phase !== "ready" && phase !== "result") return;

      setLastResult(null);

      setQueens((prev) => {
        const existing = prev.find((q) => q.x === x && q.y === y);
        if (existing) {
          return prev.filter((q) => !(q.x === x && q.y === y));
        }
        if (prev.length >= activeQueenCount) {
          return prev;
        }
        return [...prev, { x, y }];
      });

      if (phase === "result") {
        setPhase("ready");
      }
    },
    [phase],
  );

  const handleClearBoard = useCallback(() => {
    setQueens([]);
    setLastResult(null);
    if (phase === "result") setPhase("ready");
  }, [phase]);

  const handleSubmit = useCallback(async () => {
    if (!roundData) {
      setError("Start a round first.");
      return;
    }
    if (queens.length !== activeQueenCount) {
      setError(`Place exactly ${activeQueenCount} queens before submitting.`);
      return;
    }

    setError("");
    setPhase("submitting");

    try {
      const response = await submitQueensAnswer({
        sessionId: roundData.sessionId,
        queens,
      });
      setLastResult(response);
      setDiscovered(response.solutionsDiscovered);
      setTotalSolutions(response.totalSolutions);

      const isAllFound =
        response.correct &&
        !response.alreadyDiscovered &&
        response.solutionsDiscovered === 0 &&
        response.totalSolutions > 0;

      setPhase(isAllFound ? "complete" : "result");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to submit answer.");
      setPhase("ready");
    }
  }, [roundData, queens]);

  const handleNewRound = useCallback(() => {
    setRoundData(null);
    setQueens([]);
    setLastResult(null);
    setDiscovered(0);
    setTotalSolutions(0);
    setBenchmarkData([]);
    setPhase("idle");
    setError("");
  }, []);

  const handleRun20Times = useCallback(async () => {
    setError("");
    setLastResult(null);
    setQueens([]);
    setPhase("starting");
    setLoadingBenchmark(true);

    try {
      const runs: BenchmarkRow[] = [];

      for (let i = 1; i <= 20; i++) {
        const response = await startQueensRound({
          boardSize: BOARD_SIZE,
          queenCount,
        });

        setRoundData(response);
        setActiveQueenCount(queenCount);
        setTotalSolutions(response.totalSolutions);
        setDiscovered(0);

        runs.push({
          round: i,
          sequential: response.sequentialTimeMs,
          threaded: response.threadedTimeMs,
          totalSolutions: response.totalSolutions,
        });
      }

      setBenchmarkData(runs);
      setPhase("ready");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to run benchmark.");
      setPhase("idle");
    } finally {
      setLoadingBenchmark(false);
    }
  }, [queenCount]);

  const isBoardDisabled = phase === "starting" || phase === "submitting";
  const canSubmit = queens.length === activeQueenCount && phase === "ready";

  const controls = (
    <div className="flex flex-col gap-6">
      {phase === "idle" && (
        <>
          <div>
            <label className="text-[10px] font-mono uppercase tracking-[0.3em] text-white/40 mb-3 block">
              Queens to place
            </label>
            <div className="py-3 rounded font-bold uppercase tracking-widest text-xs bg-blue-600 text-white border border-blue-500 text-center">
              8 Queens
            </div>
            <p className="mt-2 text-[9px] font-mono uppercase tracking-[0.2em] text-white/30">
              Spec default: 8 queens on 16×16.
            </p>
          </div>

          <div className="text-[10px] font-mono uppercase tracking-[0.2em] text-white/30 leading-relaxed">
            Time-bounded search — sequential and threaded each run for ~15 s.
            For 8 queens on 16×16 (intractable, sampled).
          </div>

          <button
            onClick={handleStartRound}
            className="bg-white text-black py-3 rounded font-bold uppercase tracking-widest hover:bg-blue-600 hover:text-white transition-colors text-xs"
          >
            Deploy Queens
          </button>

          <button
            onClick={handleRun20Times}
            disabled={loadingBenchmark}
            className="bg-transparent border border-white/10 text-white/70 py-2.5 rounded font-bold uppercase tracking-widest hover:border-cyan-400/60 hover:text-cyan-200 transition-colors text-[10px] disabled:opacity-30 disabled:cursor-not-allowed"
          >
            {loadingBenchmark ? "Benchmarking..." : "Run 20 Times"}
          </button>
        </>
      )}

      {phase === "starting" && (
        <div className="text-center py-8">
          <div className="inline-block w-8 h-8 border-2 border-blue-500 border-t-transparent rounded-full animate-spin mb-4" />
          <div className="text-[10px] font-mono uppercase tracking-[0.2em] text-white/60 mb-2">
            {loadingBenchmark ? "Benchmarking..." : "Solving..."}
          </div>
          <div className="text-[9px] font-mono uppercase tracking-[0.15em] text-white/30 leading-relaxed max-w-[240px] mx-auto">
            Searching {queenCount}-queen placements on {BOARD_SIZE}×{BOARD_SIZE}.
            <br />
            Time-bounded — ~15 s per algorithm.
          </div>
        </div>
      )}

      {roundData && phase !== "idle" && phase !== "starting" && (
        <>
          <PerformanceStats
            sequentialTimeMs={roundData.sequentialTimeMs}
            threadedTimeMs={roundData.threadedTimeMs}
            totalSolutions={roundData.totalSolutions}
            sequentialFound={roundData.sequentialFound}
            threadedFound={roundData.threadedFound}
            sequentialHitLimit={roundData.sequentialHitLimit}
            threadedHitLimit={roundData.threadedHitLimit}
            timeBudgetMs={roundData.timeBudgetMs}
          />

          <div className="pt-4 border-t border-white/5">
            <SolutionProgress discovered={discovered} total={totalSolutions} />
          </div>

          <div className="flex flex-col gap-2 pt-4 border-t border-white/5">
            <button
              onClick={handleSubmit}
              disabled={!canSubmit}
              className="bg-white text-black py-3 rounded font-bold uppercase tracking-widest hover:bg-blue-600 hover:text-white transition-colors text-xs disabled:opacity-30 disabled:cursor-not-allowed disabled:hover:bg-white disabled:hover:text-black"
            >
              {phase === "submitting" ? "Checking..." : "Submit Solution"}
            </button>

            <button
              onClick={handleClearBoard}
              disabled={queens.length === 0 || isBoardDisabled}
              className="bg-transparent border border-white/10 text-white/70 py-2.5 rounded font-bold uppercase tracking-widest hover:border-red-500/50 hover:text-red-400 transition-colors text-[10px] disabled:opacity-30 disabled:cursor-not-allowed"
            >
              Clear Board
            </button>

            <button
              onClick={handleNewRound}
              className="bg-transparent border border-white/10 text-white/50 py-2.5 rounded font-bold uppercase tracking-widest hover:border-white/30 hover:text-white transition-colors text-[10px] mt-1"
            >
              New Round
            </button>

            <button
              onClick={handleRun20Times}
              disabled={loadingBenchmark || isBoardDisabled}
              className="bg-transparent border border-white/10 text-white/70 py-2.5 rounded font-bold uppercase tracking-widest hover:border-cyan-400/60 hover:text-cyan-200 transition-colors text-[10px] disabled:opacity-30 disabled:cursor-not-allowed"
            >
              {loadingBenchmark ? "Benchmarking..." : "Run 20 Times"}
            </button>
          </div>
        </>
      )}

      {error && (
        <div className="border border-red-500/30 bg-red-500/5 rounded p-3 text-[11px] text-red-300 font-mono">
          {error}
        </div>
      )}
    </div>
  );

  const visualization = (
    <div className="w-full h-full flex flex-col items-center justify-center gap-6">
      {phase === "idle" && !roundData ? (
        <div className="text-center text-white/50 border-2 border-dashed border-blue-500/50 bg-blue-500/5 p-12 rounded-xl max-w-xl">
          <span className="font-mono text-xl md:text-2xl uppercase tracking-widest text-white mb-4 block font-black">
            Ready to Deploy
          </span>
          <p className="text-sm leading-relaxed mb-6">
            Click{" "}
            <span className="text-blue-400 font-bold">Deploy Queens</span> to
            begin. The solver will calculate all valid {activeQueenCount}-queen
            placements on a {BOARD_SIZE}×{BOARD_SIZE} board.
          </p>
          <div className="inline-flex items-center gap-2 px-4 py-2 bg-black/50 rounded-full border border-white/10 text-[10px] font-mono">
            <div className="w-1.5 h-1.5 rounded-full bg-amber-500 animate-pulse" />
            Awaiting initialization
          </div>
        </div>
      ) : (
        <>
          <div className="w-full flex items-center justify-between max-w-[680px]">
            <div>
              <div className="text-[9px] font-mono uppercase tracking-[0.2em] text-white/40">
                Queens Placed
              </div>
              <div className="text-2xl font-black text-white tabular-nums">
                {queens.length}{" "}
                <span className="text-white/30 text-sm">/ {activeQueenCount}</span>
              </div>
            </div>

            {lastResult && (
              <div
                className={`px-4 py-2 rounded font-mono text-[10px] uppercase tracking-[0.2em] border ${
                  lastResult.correct && !lastResult.alreadyDiscovered
                    ? "border-green-500/50 bg-green-500/10 text-green-300"
                    : lastResult.alreadyDiscovered
                      ? "border-amber-500/50 bg-amber-500/10 text-amber-300"
                      : "border-red-500/50 bg-red-500/10 text-red-300"
                }`}
              >
                {lastResult.correct && !lastResult.alreadyDiscovered
                  ? "New Solution ✓"
                  : lastResult.alreadyDiscovered
                    ? "Already Discovered"
                    : "Incorrect"}
              </div>
            )}

            {phase === "complete" && (
              <div className="px-4 py-2 rounded font-mono text-[10px] uppercase tracking-[0.2em] border border-green-500/50 bg-green-500/10 text-green-300">
                All Solutions Found ✓
              </div>
            )}
          </div>

          <QueensBoard
            boardSize={BOARD_SIZE}
            queens={queens}
            onCellClick={handleCellClick}
            disabled={isBoardDisabled}
          />

          <div className="text-[10px] font-mono uppercase tracking-[0.2em] text-white/30 text-center max-w-md">
            Click a cell to place a queen. Red tint shows attacked squares.
            Place exactly {activeQueenCount} queens with no conflicts, then submit.
          </div>

          {lastResult && (
            <div className="text-[11px] font-mono text-white/60 text-center max-w-md">
              {lastResult.message}
            </div>
          )}

          {benchmarkData.length > 0 && (
            <div className="w-full max-w-[920px]">
              <PerformanceRunChart
                title="Queens Solver Timing"
                data={benchmarkData}
                series={[
                  {
                    key: "sequential",
                    label: "Sequential",
                    colorClass: "bg-amber-500",
                    stroke: "#f59e0b",
                  },
                  {
                    key: "threaded",
                    label: "Threaded",
                    colorClass: "bg-cyan-400",
                    stroke: "#22d3ee",
                  },
                ]}
                yLabel="Time (ms)"
              />

              <div className="mt-4">
                <PerformanceRunChart
                  title="Solutions Count"
                  data={benchmarkData}
                  series={[
                    {
                      key: "totalSolutions",
                      label: "Solutions",
                      colorClass: "bg-emerald-500",
                      stroke: "#10b981",
                    },
                  ]}
                  yLabel="Total Solutions"
                  valueFormatter={(value) => Math.round(value).toLocaleString()}
                  tooltipFormatter={(label, round, value) =>
                    `${label} - Round ${round}: ${Math.round(value).toLocaleString()}`
                  }
                />
              </div>

              <div className="mt-6 rounded-2xl border border-white/10 bg-[#050505] p-5 shadow-2xl">
                <div className="mb-4 text-[10px] font-bold uppercase tracking-[0.3em] text-cyan-400">
                  20-Run Benchmark Table
                </div>
                <div className="overflow-x-auto">
                  <table className="w-full border-collapse text-left text-[11px] text-white/70">
                    <thead className="text-[10px] uppercase tracking-[0.2em] text-white/40">
                      <tr className="border-b border-white/10">
                        <th className="py-2 pr-3">Round</th>
                        <th className="py-2 pr-3">Sequential (ms)</th>
                        <th className="py-2">Threaded (ms)</th>
                      </tr>
                    </thead>
                    <tbody className="font-mono">
                      {benchmarkData.map((row) => (
                        <tr key={row.round} className="border-b border-white/5">
                          <td className="py-2 pr-3 text-white/60">
                            {row.round}
                          </td>
                          <td className="py-2 pr-3 text-amber-300">
                            {formatMs(row.sequential)}
                          </td>
                          <td className="py-2 text-cyan-300">
                            {formatMs(row.threaded)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );

  return (
    <AuthGate>
      <GameLayoutShell
        gameId="05"
        title="Sixteen Queens Puzzle"
        description={`Place ${activeQueenCount} queens on a ${BOARD_SIZE}×${BOARD_SIZE} board. Backend compares sequential vs threaded backtracking execution times.`}
        controls={controls}
        visualization={visualization}
      />
    </AuthGate>
  );
}
