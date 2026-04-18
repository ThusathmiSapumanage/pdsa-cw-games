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
const QUEEN_COUNT = 16;
const MAX_BENCHMARK_POINTS = 12;

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
  const [roundData, setRoundData] = useState<StartResponse | null>(null);
  const [queens, setQueens] = useState<Position[]>([]);
  const [lastResult, setLastResult] = useState<SubmitResponse | null>(null);
  const [discovered, setDiscovered] = useState(0);
  const [totalSolutions, setTotalSolutions] = useState(0);
  const [error, setError] = useState("");
  const [benchmarkData, setBenchmarkData] = useState<BenchmarkRow[]>([]);

  const handleStartRound = useCallback(async () => {
    setError("");
    setLastResult(null);
    setQueens([]);
    setPhase("starting");

    try {
      const response = await startQueensRound({
        boardSize: BOARD_SIZE,
        queenCount: QUEEN_COUNT,
      });
      setRoundData(response);
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
  }, []);

  const handleCellClick = useCallback(
    (x: number, y: number) => {
      if (phase !== "ready" && phase !== "result") return;

      setLastResult(null);

      setQueens((prev) => {
        const existing = prev.find((q) => q.x === x && q.y === y);
        if (existing) {
          return prev.filter((q) => !(q.x === x && q.y === y));
        }
        if (prev.length >= QUEEN_COUNT) {
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
    if (queens.length !== QUEEN_COUNT) {
      setError(`Place exactly ${QUEEN_COUNT} queens before submitting.`);
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
    setPhase("idle");
    setError("");
  }, []);

  const isBoardDisabled = phase === "starting" || phase === "submitting";
  const canSubmit = queens.length === QUEEN_COUNT && phase === "ready";

  const controls = (
    <div className="flex flex-col gap-6">
      {phase === "idle" && (
        <>
          <div className="text-[10px] font-mono uppercase tracking-[0.2em] text-white/30 leading-relaxed">
            Backend counts all valid placements of {QUEEN_COUNT} queens on a{" "}
            {BOARD_SIZE}×{BOARD_SIZE} board using sequential and threaded
            backtracking. Expect ~2 min total.
          </div>

          <button
            onClick={handleStartRound}
            className="bg-white text-black py-3 rounded font-bold uppercase tracking-widest hover:bg-blue-600 hover:text-white transition-colors text-xs"
          >
            Deploy Queens
          </button>
        </>
      )}

      {phase === "starting" && (
        <div className="text-center py-8">
          <div className="inline-block w-8 h-8 border-2 border-blue-500 border-t-transparent rounded-full animate-spin mb-4" />
          <div className="text-[10px] font-mono uppercase tracking-[0.2em] text-white/60 mb-2">
            Solving...
          </div>
          <div className="text-[9px] font-mono uppercase tracking-[0.15em] text-white/30 leading-relaxed max-w-[200px] mx-auto">
            Counting all {BOARD_SIZE}×{BOARD_SIZE} placements.
            <br />
            Sequential ~2 min, threaded ~20 sec.
          </div>
        </div>
      )}

      {roundData && phase !== "idle" && phase !== "starting" && (
        <>
          <PerformanceStats
            sequentialTimeMs={roundData.sequentialTimeMs}
            threadedTimeMs={roundData.threadedTimeMs}
            totalSolutions={roundData.totalSolutions}
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
            Click <span className="text-blue-400 font-bold">Deploy Queens</span>{" "}
            to begin. The solver will calculate all valid {QUEEN_COUNT}-queen
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
                <span className="text-white/30 text-sm">/ {QUEEN_COUNT}</span>
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
            Place exactly {QUEEN_COUNT} queens with no conflicts, then submit.
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
                />
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
        description={`Place ${QUEEN_COUNT} queens on a ${BOARD_SIZE}×${BOARD_SIZE} board. Backend compares sequential vs threaded backtracking execution times.`}
        controls={controls}
        visualization={visualization}
      />
    </AuthGate>
  );
}
