export type Position = {
  x: number;
  y: number;
};

export type StartRequest = {
  boardSize: number;
  queenCount: number;
};

export type StartResponse = {
  sessionId: number;
  boardSize: number;
  queenCount: number;
  totalSolutions: number;
  sequentialTimeMs: number;
  threadedTimeMs: number;
  message: string;
};

export type SubmitRequest = {
  sessionId: number;
  queens: Position[];
};

export type SubmitResponse = {
  sessionId: number;
  playerId: number;
  correct: boolean;
  alreadyDiscovered: boolean;
  solutionsDiscovered: number;
  totalSolutions: number;
  message: string;
};

export type StatusResponse = {
  sessionId: number;
  totalSolutions: number;
  solutionsDiscovered: number;
  message: string;
};

export type ApiError = {
  message?: string;
  error?: string;
};
