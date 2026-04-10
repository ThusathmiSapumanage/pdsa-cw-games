"use client";

import { useMemo, useState } from "react";
import KnightBoard from "./KnightBoard";
import {
  CorrectAnswerResponse,
  Move,
  StartResponse,
  SubmitResponse,
} from "./types";
import {
  getKnightTourCorrectAnswer,
  startKnightTourRound,
  submitKnightTourAnswer,
} from "../../src/lib/KnightsTourApi";

function isValidKnightMove(fromX: number, fromY: number, toX: number, toY: number) {
  const dx = Math.abs(toX - fromX);
  const dy = Math.abs(toY - fromY);

  return (dx === 2 && dy === 1) || (dx === 1 && dy === 2);
}

export default function KnightTourGame() {
  const [playerId, setPlayerId] = useState("");
  const [boardSize, setBoardSize] = useState<8 | 16>(8);

  const [roundData, setRoundData] = useState<StartResponse | null>(null);
  const [submitResult, setSubmitResult] = useState<SubmitResponse | null>(null);
  const [correctAnswer, setCorrectAnswer] =
    useState<CorrectAnswerResponse | null>(null);

  const [selectedMoves, setSelectedMoves] = useState<Move[]>([]);

  const [loadingStart, setLoadingStart] = useState(false);
  const [loadingSubmit, setLoadingSubmit] = useState(false);

  const [error, setError] = useState("");
  const [moveError, setMoveError] = useState("");

  const maxMoves = useMemo(() => {
    return boardSize * boardSize;
  }, [boardSize]);

  const handleStartRound = async () => {
    try {
      setError("");
      setMoveError("");
      setSubmitResult(null);
      setCorrectAnswer(null);
      setSelectedMoves([]);

      const parsedPlayerId = Number(playerId);

      if (!parsedPlayerId || parsedPlayerId <= 0) {
        setError("Please enter a valid player ID.");
        return;
      }

      setLoadingStart(true);

      const response = await startKnightTourRound({
        playerId: parsedPlayerId,
        boardSize,
      });

      setRoundData(response);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to start round.");
    } finally {
      setLoadingStart(false);
    }
  };

  const handleCellClick = (x: number, y: number) => {
    if (!roundData || correctAnswer) return;

    setError("");
    setMoveError("");

    const existing = selectedMoves.find((move) => move.x === x && move.y === y);

    if (existing) {
    const lastMove = selectedMoves[selectedMoves.length - 1];

    if (lastMove.x === x && lastMove.y === y) {
        setMoveError("");
        setSelectedMoves((prev) => prev.slice(0, -1));
    } else {
        setMoveError(
        "You can only remove the most recent move. Use Undo Last to go back step by step."
        );
    }
    return;
    }

    if (selectedMoves.length >= maxMoves) {
      setMoveError("You have already selected the maximum number of moves.");
      return;
    }

    if (selectedMoves.length === 0) {
      if (x !== roundData.startX || y !== roundData.startY) {
        setMoveError(
          `Your first move must start at (${roundData.startX}, ${roundData.startY}).`
        );
        return;
      }

      setSelectedMoves([
        {
          stepNo: 1,
          x,
          y,
        },
      ]);
      return;
    }

    const lastMove = selectedMoves[selectedMoves.length - 1];
    const legal = isValidKnightMove(lastMove.x, lastMove.y, x, y);

    if (!legal) {
      setMoveError(
        `Illegal move. A knight must move in an L-shape from (${lastMove.x}, ${lastMove.y}).`
      );
      return;
    }

    setSelectedMoves((prev) => [
      ...prev,
      {
        stepNo: prev.length + 1,
        x,
        y,
      },
    ]);
  };

  const handleUndoLast = () => {
    if (correctAnswer) return;
    setMoveError("");
    setSelectedMoves((prev) => prev.slice(0, -1));
  };

  const handleClearAll = () => {
    if (correctAnswer) return;
    setMoveError("");
    setSelectedMoves([]);
    setSubmitResult(null);
    setError("");
  };

  const handleSubmit = async () => {
    try {
      if (!roundData) {
        setError("Start a round first.");
        return;
      }

      if (selectedMoves.length === 0) {
        setError("Please select at least one move before submitting.");
        return;
      }

      setError("");
      setMoveError("");
      setLoadingSubmit(true);

      const submitResponse = await submitKnightTourAnswer({
        sessionId: roundData.sessionId,
        playerId: roundData.playerId,
        moves: selectedMoves,
      });

      setSubmitResult(submitResponse);

      const correctResponse = await getKnightTourCorrectAnswer(
        roundData.sessionId,
        roundData.playerId
      );

      setCorrectAnswer(correctResponse);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to submit answer.");
    } finally {
      setLoadingSubmit(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-950 via-slate-900 to-purple-950 px-4 py-8 text-white">
      <div className="mx-auto max-w-7xl">
        <div className="mb-8 rounded-3xl border border-white/10 bg-white/5 p-6 shadow-2xl backdrop-blur">
          <h1 className="text-3xl font-bold">Knight’s Tour</h1>
          <p className="mt-2 text-sm text-slate-300">
            Start a round, then click only valid knight moves in order.
          </p>
        </div>

        <div className="grid gap-6 lg:grid-cols-[360px_1fr]">
          <div className="space-y-6">
            <div className="rounded-3xl border border-white/10 bg-white/5 p-6 shadow-xl backdrop-blur">
              <h2 className="mb-4 text-xl font-semibold">Start Game</h2>

              <div className="space-y-4">
                <div>
                  <label className="mb-2 block text-sm text-slate-300">
                    Player ID
                  </label>
                  <input
                    type="number"
                    value={playerId}
                    onChange={(e) => setPlayerId(e.target.value)}
                    className="w-full rounded-xl border border-white/10 bg-slate-900 px-4 py-3 outline-none transition focus:border-purple-400"
                    placeholder="Enter player ID"
                  />
                </div>

                <div>
                  <label className="mb-2 block text-sm text-slate-300">
                    Board Size
                  </label>
                  <select
                    value={boardSize}
                    onChange={(e) => setBoardSize(Number(e.target.value) as 8 | 16)}
                    className="w-full rounded-xl border border-white/10 bg-slate-900 px-4 py-3 outline-none transition focus:border-purple-400"
                  >
                    <option value={8}>8 x 8</option>
                    <option value={16}>16 x 16</option>
                  </select>
                </div>

                <button
                  onClick={handleStartRound}
                  disabled={loadingStart}
                  className="w-full rounded-xl bg-purple-600 px-4 py-3 font-semibold transition hover:bg-purple-500 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {loadingStart ? "Starting..." : "Start Round"}
                </button>
              </div>
            </div>

            <div className="rounded-3xl border border-white/10 bg-white/5 p-6 shadow-xl backdrop-blur">
              <h2 className="mb-4 text-xl font-semibold">Round Details</h2>

              {roundData ? (
                <div className="space-y-3 text-sm">
                  <div className="flex justify-between gap-4">
                    <span className="text-slate-400">Session ID</span>
                    <span>{roundData.sessionId}</span>
                  </div>
                  <div className="flex justify-between gap-4">
                    <span className="text-slate-400">Player ID</span>
                    <span>{roundData.playerId}</span>
                  </div>
                  <div className="flex justify-between gap-4">
                    <span className="text-slate-400">Board Size</span>
                    <span>
                      {roundData.boardSize} x {roundData.boardSize}
                    </span>
                  </div>
                  <div className="flex justify-between gap-4">
                    <span className="text-slate-400">Start Position</span>
                    <span>
                      ({roundData.startX}, {roundData.startY})
                    </span>
                  </div>
                  <div className="flex justify-between gap-4">
                    <span className="text-slate-400">Expected Moves</span>
                    <span>{roundData.expectedMoveCount}</span>
                  </div>
                  <div className="flex justify-between gap-4">
                    <span className="text-slate-400">Algorithm</span>
                    <span>{roundData.algorithmName}</span>
                  </div>
                  <div className="flex justify-between gap-4">
                    <span className="text-slate-400">Time</span>
                    <span>{roundData.algorithmExecutionTimeMs?.toFixed(3)} ms</span>
                  </div>
                </div>
              ) : (
                <p className="text-sm text-slate-400">No round started yet.</p>
              )}
            </div>

            <div className="rounded-3xl border border-white/10 bg-white/5 p-6 shadow-xl backdrop-blur">
              <h2 className="mb-4 text-xl font-semibold">Your Moves</h2>

              <div className="mb-4 flex flex-wrap gap-3">
                <button
                  onClick={handleUndoLast}
                  disabled={selectedMoves.length === 0 || !!correctAnswer}
                  className="rounded-xl bg-slate-700 px-4 py-2 text-sm font-medium transition hover:bg-slate-600 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  Undo Last
                </button>

                <button
                  onClick={handleClearAll}
                  disabled={selectedMoves.length === 0 || !!correctAnswer}
                  className="rounded-xl bg-rose-600 px-4 py-2 text-sm font-medium transition hover:bg-rose-500 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  Clear All
                </button>

                <button
                  onClick={handleSubmit}
                  disabled={
                    !roundData ||
                    selectedMoves.length === 0 ||
                    loadingSubmit ||
                    !!correctAnswer
                  }
                  className="rounded-xl bg-emerald-600 px-4 py-2 text-sm font-medium transition hover:bg-emerald-500 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  {loadingSubmit ? "Submitting..." : "Submit Answer"}
                </button>
              </div>

              <div className="max-h-72 overflow-y-auto rounded-xl border border-white/10 bg-slate-950/50 p-3">
                {selectedMoves.length === 0 ? (
                  <p className="text-sm text-slate-400">No moves selected yet.</p>
                ) : (
                  <div className="space-y-2">
                    {selectedMoves.map((move) => (
                      <div
                        key={`${move.stepNo}-${move.x}-${move.y}`}
                        className="flex items-center justify-between rounded-lg bg-white/5 px-3 py-2 text-sm"
                      >
                        <span>Step {move.stepNo}</span>
                        <span>
                          ({move.x}, {move.y})
                        </span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>

            {submitResult && (
              <div
                className={`rounded-3xl border p-6 shadow-xl backdrop-blur ${
                  submitResult.correct
                    ? "border-emerald-400/30 bg-emerald-500/10"
                    : "border-rose-400/30 bg-rose-500/10"
                }`}
              >
                <h2 className="mb-3 text-xl font-semibold">Game Result</h2>
                <div className="space-y-2 text-sm">
                  <div className="flex justify-between gap-4">
                    <span className="text-slate-300">Outcome</span>
                    <span className="font-bold">{submitResult.outcome}</span>
                  </div>
                  <div className="flex justify-between gap-4">
                    <span className="text-slate-300">Correct</span>
                    <span>{submitResult.correct ? "Yes" : "No"}</span>
                  </div>
                  <div className="flex justify-between gap-4">
                    <span className="text-slate-300">Expected Moves</span>
                    <span>{submitResult.expectedMoveCount}</span>
                  </div>
                  <div className="flex justify-between gap-4">
                    <span className="text-slate-300">Submitted Moves</span>
                    <span>{submitResult.submittedMoveCount}</span>
                  </div>
                  <p className="pt-2 text-slate-100">{submitResult.message}</p>
                </div>
              </div>
            )}

            {moveError && (
              <div className="rounded-2xl border border-amber-400/30 bg-amber-500/10 p-4 text-sm text-amber-100">
                {moveError}
              </div>
            )}

            {error && (
              <div className="rounded-2xl border border-rose-400/30 bg-rose-500/10 p-4 text-sm text-rose-100">
                {error}
              </div>
            )}
          </div>

          <div className="rounded-3xl border border-white/10 bg-white/5 p-6 shadow-2xl backdrop-blur">
            <div className="mb-4 flex items-center justify-between gap-4">
              <h2 className="text-2xl font-semibold">Board</h2>
              <div className="text-sm text-slate-300">
                Selected {selectedMoves.length} /{" "}
                {roundData?.expectedMoveCount ?? maxMoves}
              </div>
            </div>

            {correctAnswer && (
              <div className="mb-4 rounded-2xl border border-emerald-400/30 bg-emerald-500/10 p-4 text-sm text-emerald-100">
                Correct answer is now shown in green.
              </div>
            )}

            <div className={`${boardSize === 16 ? "max-w-[900px]" : "max-w-[700px]"}`}>
              <KnightBoard
                boardSize={roundData?.boardSize || boardSize}
                selectedMoves={selectedMoves}
                correctMoves={correctAnswer?.moves || []}
                onCellClick={handleCellClick}
                disabled={!roundData || !!correctAnswer}
                startX={roundData?.startX}
                startY={roundData?.startY}
              />
            </div>

            <div className="mt-4 rounded-2xl border border-white/10 bg-slate-950/40 p-4 text-sm text-slate-300">
              {correctAnswer
                ? "Purple = your answer, Green = correct answer."
                : "Your first click must be the starting square, and every next click must be a legal knight move."}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}