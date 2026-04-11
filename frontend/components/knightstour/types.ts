export type Move = {
  stepNo: number;
  x: number;
  y: number;
};

export type StartRequest = {
  playerId: number;
  boardSize: 8 | 16;
};

export type StartResponse = {
  sessionId: number;
  playerId: number;
  boardSize: number;
  startX: number;
  startY: number;
  expectedMoveCount: number;
  algorithmName: string;
  algorithmExecutionTimeMs: number;
  status: string;
  message: string;
};

export type SubmitRequest = {
  sessionId: number;
  playerId: number;
  moves: Move[];
};

export type SubmitResponse = {
  responseId: number;
  sessionId: number;
  playerId: number;
  correct: boolean;
  outcome: "WIN" | "LOSE" | "DRAW";
  expectedMoveCount: number;
  submittedMoveCount: number;

  structuralValid: boolean;
  completeTour: boolean;
  deadEnd: boolean;
  warnsdorffValid: boolean;
  backtrackingValid: boolean;
  validationSource: string | null;
  reasonCode: string;

  warnsdorffCorrectMoves: Move[];
  warnsdorffMoveCount: number;
  backtrackingCorrectMoves: Move[];
  backtrackingMoveCount: number;

  message: string;
};

export type HintRequest = {
  sessionId: number;
  playerId: number;
  moves: Move[];
};

export type HintResponse = {
  sessionId: number;
  playerId: number;
  fromStepNo: number;
  fromX: number;
  fromY: number;
  hintMoves: Move[];
  message: string;
};

export type CorrectAnswerResponse = {
  sessionId: number;
  playerId: number;
  algorithmName: string;
  moveCount: number;
  moves: Move[];
  message: string;
};

export type ApiError = {
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
};