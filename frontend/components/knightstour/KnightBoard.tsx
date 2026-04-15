"use client";

import { motion } from "framer-motion";
import { Move } from "./types";

type KnightBoardProps = {
  boardSize: number;
  selectedMoves: Move[];
  correctMoves?: Move[];
  hintMoves?: Move[];
  onCellClick: (x: number, y: number) => void;
  disabled?: boolean;
  startX?: number;
  startY?: number;
  playbackMoves?: Move[];
  playbackStep?: number;
  showPlayerPath?: boolean;
};

export default function KnightBoard({
  boardSize,
  selectedMoves,
  correctMoves = [],
  hintMoves = [],
  onCellClick,
  disabled = false,
  startX,
  startY,
  playbackMoves = [],
  playbackStep,
  showPlayerPath = true,
}: KnightBoardProps) {
  const getPlayerMoveForCell = (x: number, y: number) =>
    showPlayerPath ? selectedMoves.find((move) => move.x === x && move.y === y) : undefined;

  const getCorrectMoveForCell = (x: number, y: number) =>
    correctMoves.find((move) => move.x === x && move.y === y);

  const getHintMoveForCell = (x: number, y: number) =>
    hintMoves.find((move) => move.x === x && move.y === y);

  const playbackActive =
    playbackMoves.length > 0 &&
    typeof playbackStep === "number" &&
    playbackStep >= 0 &&
    playbackStep < playbackMoves.length;

  const playbackMove = playbackActive ? playbackMoves[playbackStep!] : null;

  const latestSelectedMove =
    selectedMoves.length > 0 ? selectedMoves[selectedMoves.length - 1] : null;

  const knightX = playbackMove?.x ?? latestSelectedMove?.x ?? startX;
  const knightY = playbackMove?.y ?? latestSelectedMove?.y ?? startY;

  const cellBaseSize = boardSize === 16 ? "minmax(0, 1fr)" : "minmax(72px, 1fr)";
  const numberSize = boardSize === 16 ? "text-sm" : "text-xl";
  const coordinateSize = boardSize === 16 ? "text-[10px]" : "text-xs";
  const knightSize = boardSize === 16 ? "text-3xl" : "text-6xl";
  const hintSize = boardSize === 16 ? "text-[10px]" : "text-xs";

  return (
    <div
      className="grid gap-[4px] rounded-[28px] border border-white/10 bg-black/35 p-4 shadow-2xl"
      style={{
        gridTemplateColumns: `repeat(${boardSize}, ${cellBaseSize})`,
      }}
    >
      {Array.from({ length: boardSize * boardSize }, (_, index) => {
        const row = Math.floor(index / boardSize) + 1;
        const col = (index % boardSize) + 1;

        const isDark = (row + col) % 2 === 0;
        const playerMove = getPlayerMoveForCell(col, row);
        const correctMove = getCorrectMoveForCell(col, row);
        const hintMove = getHintMoveForCell(col, row);
        const isStart = startX === col && startY === row;
        const isKnightHere = knightX === col && knightY === row;
        const isPlaybackCurrent = playbackMove?.x === col && playbackMove?.y === row;

        let cellClass = isDark
          ? "border-slate-700 bg-slate-800/90 text-slate-100"
          : "border-slate-300/30 bg-slate-100 text-slate-900";

        if (hintMove) {
          cellClass = "border-amber-300 bg-amber-500 text-slate-950";
        }

        if (playerMove) {
          cellClass = "border-purple-300 bg-purple-600 text-white";
        }

        if (correctMove) {
          cellClass = "border-emerald-300 bg-emerald-600 text-white";
        }

        if (isPlaybackCurrent) {
          cellClass = "border-cyan-300 bg-cyan-700 text-white";
        }

        if (isKnightHere && !playerMove && !correctMove && !hintMove && !isPlaybackCurrent) {
          cellClass = "border-fuchsia-300 bg-fuchsia-700 text-white";
        }

        return (
          <button
            key={`${col}-${row}`}
            type="button"
            onClick={() => onCellClick(col, row)}
            disabled={disabled}
            className={`relative flex aspect-square min-h-[50px] items-center justify-center overflow-hidden rounded-xl border font-bold transition-all duration-200 ${
              disabled ? "cursor-not-allowed opacity-90" : "hover:scale-[1.03]"
            } ${cellClass}`}
            title={`(${col}, ${row})`}
          >
            <span
              className={`absolute left-1.5 top-1.5 z-10 ${coordinateSize} opacity-70`}
            >
              {col},{row}
            </span>

            {isStart && !playerMove && !correctMove && !hintMove && !isKnightHere && !isPlaybackCurrent && (
              <span className="z-10 text-[11px] font-extrabold tracking-wide text-emerald-200">
                START
              </span>
            )}

            {correctMove ? (
              <span className={`z-10 ${numberSize}`}>{correctMove.stepNo}</span>
            ) : playerMove ? (
              <span className={`z-10 ${numberSize}`}>{playerMove.stepNo}</span>
            ) : hintMove ? (
              <span className={`z-10 ${hintSize} font-extrabold`}>HINT</span>
            ) : null}

            {isKnightHere && (
              <motion.span
                layoutId="knight-piece"
                transition={{ type: "spring", stiffness: 360, damping: 28 }}
                className={`pointer-events-none absolute inset-0 z-20 flex items-center justify-center ${knightSize} drop-shadow-[0_0_16px_rgba(255,255,255,0.55)]`}
              >
                <span className="inline-block translate-y-[2px] select-none">♞</span>
              </motion.span>
            )}
          </button>
        );
      })}
    </div>
  );
}