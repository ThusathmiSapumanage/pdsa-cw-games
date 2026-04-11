"use client";

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
}: KnightBoardProps) {
  const getPlayerMoveForCell = (x: number, y: number) => {
    return selectedMoves.find((move) => move.x === x && move.y === y);
  };

  const getCorrectMoveForCell = (x: number, y: number) => {
    return correctMoves.find((move) => move.x === x && move.y === y);
  };

  const getHintMoveForCell = (x: number, y: number) => {
    return hintMoves.find((move) => move.x === x && move.y === y);
  };

  return (
    <div
      className="grid gap-[2px] rounded-xl bg-slate-800 p-2 shadow-lg"
      style={{
        gridTemplateColumns: `repeat(${boardSize}, minmax(0, 1fr))`,
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

        let cellClass = isDark
          ? "border-slate-700 bg-slate-700 text-slate-200"
          : "border-slate-500 bg-slate-200 text-slate-900";

        if (hintMove) {
          cellClass = "border-amber-300 bg-amber-500 text-slate-950";
        }

        if (playerMove) {
          cellClass = "border-purple-300 bg-purple-600 text-white";
        }

        if (correctMove) {
          cellClass = "border-emerald-300 bg-emerald-600 text-white";
        }

        return (
          <button
            key={`${col}-${row}`}
            type="button"
            onClick={() => onCellClick(col, row)}
            disabled={disabled}
            className={`relative flex aspect-square items-center justify-center rounded-md border text-[10px] font-semibold transition ${
              disabled ? "cursor-not-allowed opacity-80" : "hover:scale-[1.02]"
            } ${cellClass}`}
            title={`(${col}, ${row})`}
          >
            <span className="absolute left-1 top-1 text-[9px] opacity-70">
              {col},{row}
            </span>

            {isStart && !playerMove && !correctMove && !hintMove && (
              <span className="text-[9px] font-bold text-emerald-600">
                START
              </span>
            )}

            {correctMove ? (
              <span className={boardSize === 16 ? "text-[10px]" : "text-sm"}>
                {correctMove.stepNo}
              </span>
            ) : playerMove ? (
              <span className={boardSize === 16 ? "text-[10px]" : "text-sm"}>
                {playerMove.stepNo}
              </span>
            ) : hintMove ? (
              <span className="text-[9px] font-bold">HINT</span>
            ) : null}
          </button>
        );
      })}
    </div>
  );
}