"use client";

import { useState, useCallback } from "react";
import GameLayoutShell from "../../../components/layouts/GameLayoutShell";
import AuthGate from "../../../components/auth/AuthGate";
import { useAuth } from "../../../components/auth/AuthProvider";
import AlgoDropdown from "../../../components/forms/AlgoDropdown";
import {
  SnakeLadderBoard,
} from "../../../components/snake-ladder";
import ResultsPanel from "../../../components/snake-ladder/ResultsPanel";
import AnswerChallenge from "../../../components/snake-ladder/AnswerChallenge";
import PerformanceRunChart from "../../../components/snake-ladder/PerformanceRunChart";
import Modal from "../../../components/common/Modal";
import type { GameState, BoardItem, AlgorithmResult } from "../../../components/snake-ladder/types";

// ─── Helpers ──────────────────────────────────────────────────────────────

function extractCorrectAnswer(message: string): number | null {
  // "Correct! The minimum throws is 5."
  // "Close! You were only 1 throw off. The correct answer was 5."
  // "Incorrect. The correct answer was 5 but you chose 3. You were off by 2 throws."
  const match = message.match(/(\d+)/);
  return match ? parseInt(match[1]) : null;
}
// These hit your Spring Boot backend at localhost:8080

const BACKEND_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

async function startRound(n: number, playerId: number) {
  const res = await fetch(`${BACKEND_URL}/api/snake-ladder/start`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ n, playerId }),
  });
  if (!res.ok) throw new Error("Failed to start round");
  return res.json();          // { items, algorithmResults, choices, n, sessionId }
}

async function submitAnswer(sessionId: number, playerAnswer: number) {
  const res = await fetch(`${BACKEND_URL}/api/snake-ladder/answer`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ sessionId, playerAnswer }),
  });
  if (!res.ok) throw new Error("Failed to submit answer");
  return res.json();          // { outcome: "WIN" | "LOSE" | "DRAW", message: string }
}

// ─── Page ────────────────────────────────────────────────────────────────────

export default function SnakeLadderPage() {
  const [algorithm, setAlgorithm]   = useState("bfs");
  const [boardSize, setBoardSize]   = useState(8);
  const [loading, setLoading]       = useState(false);
  const [error, setError]           = useState<string | null>(null);
  const [gameState, setGameState]   = useState<GameState | null>(null);
  const [outcome, setOutcome]       = useState<"WIN" | "LOSE" | "DRAW" | null>(null);
  const [outcomeMessage, setOutcomeMessage] = useState<string | null>(null);
  const [correctAnswer, setCorrectAnswer] = useState<number | null>(null);
  const [selectedChoice, setSelectedChoice] = useState<number | null>(null);
  const [sessionId, setSessionId] = useState<number | null>(null);
  const [showPerformanceChart, setShowPerformanceChart] = useState(false);
  const { userId } = useAuth();

  const handleStart = useCallback(async () => {
    setLoading(true);
    setError(null);
    setOutcome(null);
    setOutcomeMessage(null);
    setSelectedChoice(null);
    setGameState(null);

    if (!userId) {
      setError("Logged-in player ID is required to start the game.");
      setLoading(false);
      return;
    }

    try {
      const data = await startRound(boardSize, userId);
      setGameState(data);
      setSessionId(data.sessionId);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Something went wrong");
    } finally {
      setLoading(false);
    }
  }, [boardSize, userId]);

  const handleSubmitAnswer = useCallback(async (choiceIndex: number) => {
    if (!gameState || sessionId == null) return;
    setSelectedChoice(choiceIndex);
    setLoading(true);
    try {
      // Get the actual answer value from the choices array
      const playerAnswer = gameState.choices[choiceIndex];
      const data = await submitAnswer(sessionId, playerAnswer);
      setOutcome(data.outcome);
      setOutcomeMessage(data.message);
      setCorrectAnswer(extractCorrectAnswer(data.message));
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to submit");
    } finally {
      setLoading(false);
    }
  }, [gameState, sessionId]);

  return (
    <AuthGate>
      <GameLayoutShell
        gameId="02"
        title="Snake and Ladder"
        description="Calculation of minimum dice throws required to navigate a dynamic board. Analyzes pathfinding efficiency."

        controls={
          <div className="flex flex-col gap-4">
            {/* Board size selector */}
            <div className="flex flex-col gap-1">
              <label className="text-[10px] uppercase tracking-widest text-white/50 font-mono">
                Board Size (N×N)
              </label>
              <div className="grid grid-cols-4 gap-2">
                {[6, 7, 8, 12].map((n) => (
                  <button
                    key={n}
                    onClick={() => setBoardSize(n)}
                    className={`
                      py-2 rounded font-mono text-xs font-bold uppercase tracking-wider
                      border transition-all duration-200
                      ${boardSize === n
                        ? "bg-white text-black border-white"
                        : "bg-transparent text-white/50 border-white/20 hover:border-white/50 hover:text-white"
                      }
                    `}
                  >
                    {n}×{n}
                  </button>
                ))}
              </div>
            </div>

            {/* Algorithm selector */}
            <AlgoDropdown
              label="Algorithm Selection"
              value={algorithm}
              onChange={setAlgorithm}
              options={[
                { value: "bfs", label: "Breadth-First Search (BFS)" },
                { value: "dp",  label: "Dynamic Programming (DP)" },
              ]}
            />

            {/* Start button */}
            <button
              onClick={handleStart}
              disabled={loading}
              className="
                bg-white text-black py-3 rounded font-bold uppercase tracking-widest
                hover:bg-blue-400 hover:text-white transition-colors text-xs
                disabled:opacity-40 disabled:cursor-not-allowed
              "
            >
              {loading ? "Generating board…" : "Generate Board"}
            </button>

            {/* Error */}
            {error && (
              <p className="text-red-400 text-xs font-mono border border-red-500/30 bg-red-500/10 rounded px-3 py-2">
                {error}
              </p>
            )}

            {/* Performance Chart Button */}
            <button
              onClick={() => setShowPerformanceChart(true)}
              className="
                bg-cyan-500/20 text-cyan-400 py-2 rounded font-bold uppercase tracking-widest
                hover:bg-cyan-500/30 transition-colors text-xs border border-cyan-500/30 hover:border-cyan-500/50
              "
            >
              📊 View Performance
            </button>

            {/* Quick legend */}
            <div className="border-t border-white/10 pt-3 flex flex-col gap-1.5">
              <p className="text-[10px] uppercase tracking-widest text-white/30 font-mono mb-1">Legend</p>
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded-sm bg-emerald-500 flex-shrink-0" />
                <span className="text-[11px] text-white/60 font-mono">Ladder (climb up)</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded-sm bg-red-500 flex-shrink-0" />
                <span className="text-[11px] text-white/60 font-mono">Snake (slide down)</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded-sm bg-yellow-400 flex-shrink-0" />
                <span className="text-[11px] text-white/60 font-mono">Start cell</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded-sm bg-blue-400 flex-shrink-0" />
                <span className="text-[11px] text-white/60 font-mono">Goal cell</span>
              </div>
            </div>
          </div>
        }

        visualization={
          <SnakeLadderBoard
            n={boardSize}
            items={gameState?.items ?? []}
            loading={loading}
            outcome={outcome}
          />
        }

        results={
          gameState ? (
            <div className="flex flex-col gap-4">
              {/* Algorithm timing results */}
              <ResultsPanel
                results={gameState.algorithmResults}
              />
              {outcomeMessage && (
                <div className="mt-3 px-3 py-2 rounded border font-mono text-xs leading-relaxed bg-yellow-500/10 border-yellow-500/30 text-yellow-400">
                  {outcomeMessage}
                </div>
              )}

              {/* Answer challenge — always shown */}
              <AnswerChallenge
                choices={gameState.choices}
                selectedChoice={selectedChoice}
                loading={loading}
                onSubmit={handleSubmitAnswer}
              />

              {/* Play again button — shown after outcome */}
              {outcome && (
                <button
                  onClick={handleStart}
                  disabled={loading}
                  className="
                    bg-white text-black py-3 rounded font-bold uppercase tracking-widest
                    hover:bg-blue-400 hover:text-white transition-colors text-xs
                    disabled:opacity-40 disabled:cursor-not-allowed
                  "
                >
                  {loading ? "Generating…" : "Play Again"}
                </button>
              )}
            </div>
          ) : (
            <IdleResults />
          )
        }
      />

      {/* Performance Chart Modal */}
      <Modal
        isOpen={showPerformanceChart}
        onClose={() => setShowPerformanceChart(false)}
        title="Performance Analysis - Algorithm Comparison"
      >
        <PerformanceRunChart
          title="Execution Time Analysis"
          data={[
            { round: 1, bfs: 0.45, dp: 0.38 },
            { round: 2, bfs: 0.52, dp: 0.41 },
            { round: 3, bfs: 0.48, dp: 0.39 },
            { round: 4, bfs: 0.61, dp: 0.44 },
            { round: 5, bfs: 0.55, dp: 0.42 },
            { round: 6, bfs: 0.67, dp: 0.48 },
            { round: 7, bfs: 0.58, dp: 0.40 },
            { round: 8, bfs: 0.72, dp: 0.52 },
          ]}
          series={[
            {
              key: "bfs",
              label: "BFS",
              colorClass: "bg-blue-500",
              stroke: "#3b82f6",
            },
            {
              key: "dp",
              label: "DP",
              colorClass: "bg-emerald-500",
              stroke: "#10b981",
            },
          ]}
          yLabel="Time (ms)"
        />
      </Modal>
    </AuthGate>
  );
}

// ─── Small inline sub-components ─────────────────────────────────────────────

function IdleResults() {
  return (
    <div className="flex flex-col gap-2 font-mono text-[10px] text-white/50">
      <p className="text-white">
        Status: <span className="text-yellow-500">Awaiting Board Generation</span>
      </p>
      <p>Minimum Dice Rolls: --</p>
      <p>BFS Time: -- ms</p>
      <p>DP Time:  -- ms</p>
      <p>Board Items: []</p>
    </div>
  );
}

function OutcomeBanner({
  outcome,
  onPlayAgain,
}: {
  outcome: "WIN" | "LOSE" | "DRAW";
  onPlayAgain: () => void;
}) {
  const config = {
    WIN:  { label: "Correct!",    sub: "You identified the optimal path.",     color: "border-emerald-500/50 bg-emerald-500/10 text-emerald-400" },
    LOSE: { label: "Wrong!",      sub: "That wasn't the minimum throw count.", color: "border-red-500/50 bg-red-500/10 text-red-400" },
    DRAW: { label: "Close!",      sub: "You were off by just one throw.",      color: "border-yellow-500/50 bg-yellow-500/10 text-yellow-400" },
  }[outcome];

  return (
    <div className={`rounded-lg border p-4 ${config.color}`}>
      <p className="font-bold font-mono uppercase tracking-widest text-sm">{config.label}</p>
      <p className="text-xs mt-1 opacity-80">{config.sub}</p>
      <button
        onClick={onPlayAgain}
        className="mt-3 text-[10px] font-mono uppercase tracking-widest border border-current rounded px-3 py-1.5 hover:bg-white/10 transition-colors"
      >
        Play Again
      </button>
    </div>
  );
}
