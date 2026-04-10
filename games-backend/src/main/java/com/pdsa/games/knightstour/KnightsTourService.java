    package com.pdsa.games.knightstour;

    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;

    import java.util.ArrayList;
    import java.util.Comparator;
    import java.util.List;
    import java.util.Random;

    @Service
    public class KnightsTourService {

        //Legal moves for a knight in chess (x and y offsets)
        private static final int[] DX = {2, 1, -1, -2, -2, -1, 1, 2};
        private static final int[] DY = {1, 2, 2, 1, -1, -2, -2, -1};

        private final KnightsTourRepository repository;
        private final Random random = new Random();

        public KnightsTourService(KnightsTourRepository repository) {
            this.repository = repository;
        }

        //why transactional ? Because we want to ensure that all database operations related to a single execution of the Warnsdorff algorithm are treated as a single unit of work. If any part of the process fails (e.g., an exception is thrown while saving the game session or algorithm execution), the entire transaction will be rolled back, ensuring data integrity and preventing partial or inconsistent data from being stored in the database.
        @Transactional
        public KnightsTourModel runWarnsdorff(KnightsTourModel request) {
            validateRequest(request);

            int boardSize = request.getBoardSize();

            int startXZeroBased = random.nextInt(boardSize);
            int startYZeroBased = random.nextInt(boardSize);

            Long sessionId = repository.createGameSession(request.getPlayerId());
            int warnsdorffAlgorithmId = mustFindAlgorithmId("Warnsdorff");

            KnightsTourModel.AlgorithmResult warnsdorffResult =
                    executeWarnsdorff(boardSize, startXZeroBased, startYZeroBased);

            int moveCountToSave = 0;
            if ("SUCCESS".equals(warnsdorffResult.getStatus())) {
                moveCountToSave = warnsdorffResult.getMoveCount();
            }

            repository.saveKnightTourGame(
                    sessionId,
                    boardSize,
                    startXZeroBased + 1, // Convert to 1-based indexing for DB storage
                    startYZeroBased + 1, // Convert to 1-based indexing for DB storage
                    moveCountToSave
            );

            repository.saveAlgorithmExecution(
                    sessionId,
                    warnsdorffAlgorithmId,
                    warnsdorffResult.getExecutionTimeMs(),
                    normalizeStatusForDb(warnsdorffResult.getStatus())
            );

            // null safe check
            if ("SUCCESS".equals(warnsdorffResult.getStatus())) {
                repository.saveKnightMoves(sessionId, warnsdorffAlgorithmId, warnsdorffResult.getMoves());
            }

            KnightsTourModel response = new KnightsTourModel();
            response.setPlayerId(request.getPlayerId());
            response.setSessionId(sessionId);
            response.setBoardSize(boardSize);
            response.setStartX(startXZeroBased + 1);
            response.setStartY(startYZeroBased + 1);
            response.setWarnsdorffResult(warnsdorffResult);

            return response;
        }

        private int mustFindAlgorithmId(String algorithmName) {
            //cause int cant be null
            Integer algorithmId = repository.findAlgorithmId(algorithmName);
            if (algorithmId == null) {
                throw new IllegalStateException("Algorithm '" + algorithmName + "' not found in Algorithm table.");
            }
            return algorithmId;
        }

        private KnightsTourModel.AlgorithmResult executeWarnsdorff(int boardSize, int startX, int startY) {
            long startNano = System.nanoTime(); //nanotime is used for measuring elapsed time.

            KnightsTourModel.AlgorithmResult result = new KnightsTourModel.AlgorithmResult();
            result.setAlgorithmName("Warnsdorff");

            try {
                List<KnightsTourModel.Move> moves = solveWarnsdorff(boardSize, startX, startY);
                result.setStatus("SUCCESS");
                result.setExecutionTimeMs(elapsedMs(startNano));
                result.setMoveCount(moves.size());
                result.setMoves(moves);
                result.setMessage("Warnsdorff completed the full tour successfully.");
                return result;
            } catch (Exception e) {
                result.setStatus("ERROR");
                result.setExecutionTimeMs(elapsedMs(startNano));
                result.setMoveCount(0);
                result.setMoves(new ArrayList<>());
                result.setMessage(e.getMessage());
                return result;
            }
        }

        private List<KnightsTourModel.Move> solveWarnsdorff(int boardSize, int startX, int startY) {
            int[][] board = createEmptyBoard(boardSize);

            int currentX = startX;
            int currentY = startY;
            board[currentX][currentY] = 0;

            List<KnightsTourModel.Move> path = new ArrayList<>();
            path.add(new KnightsTourModel.Move(1, currentX + 1, currentY + 1));

            for (int moveNo = 1; moveNo < boardSize * boardSize; moveNo++) {
                int[] nextMove = chooseBestNextMove(board, currentX, currentY, boardSize);

                if (nextMove == null) {
                    throw new IllegalStateException("Warnsdorff could not complete a full tour from the selected start position.");
                }

                currentX = nextMove[0];
                currentY = nextMove[1];
                board[currentX][currentY] = moveNo;

                path.add(new KnightsTourModel.Move(moveNo + 1, currentX + 1, currentY + 1));
            }

            return path;
        }

        private int[] chooseBestNextMove(int[][] board, int x, int y, int boardSize) {
            List<int[]> candidates = new ArrayList<>();

            for (int i = 0; i < 8; i++) {
                int nextX = x + DX[i];
                int nextY = y + DY[i];

                if (isValid(nextX, nextY, boardSize) && board[nextX][nextY] == -1) {
                    int onwardMoves = countOnwardMoves(board, nextX, nextY, boardSize);
                    candidates.add(new int[]{nextX, nextY, onwardMoves});
                }
            }

            if (candidates.isEmpty()) {
                return null;
            }

            //sort from least to most onward count
            candidates.sort(Comparator.comparingInt(a -> a[2]));

            //select all candidates with the smallest onward count
            int smallestOnwardCount = candidates.get(0)[2];
            List<int[]> bestCandidates = new ArrayList<>();

            for (int[] candidate : candidates) {
                if (candidate[2] == smallestOnwardCount) {
                    bestCandidates.add(candidate);
                } else {
                    break;
                }
            }

            return bestCandidates.get(random.nextInt(bestCandidates.size()));
        }

        private int countOnwardMoves(int[][] board, int x, int y, int boardSize) {
            int count = 0;

            for (int i = 0; i < 8; i++) {
                int nextX = x + DX[i];
                int nextY = y + DY[i];

                if (isValid(nextX, nextY, boardSize) && board[nextX][nextY] == -1) {
                    count++;
                }
            }

            return count;
        }

        private int[][] createEmptyBoard(int boardSize) {
            int[][] board = new int[boardSize][boardSize];

            for (int row = 0; row < boardSize; row++) {
                for (int col = 0; col < boardSize; col++) {
                    board[row][col] = -1;
                }
            }

            return board;
        }

        private boolean isValid(int x, int y, int boardSize) {
            return x >= 0 && x < boardSize && y >= 0 && y < boardSize;
        }

        private double elapsedMs(long startNano) {
            return (System.nanoTime() - startNano) / 1_000_000.0;
        }

        private String normalizeStatusForDb(String status) {
            if ("SUCCESS".equals(status)) {
                return "SUCCESS";
            }
            return "ERROR";
        }

        private void validateRequest(KnightsTourModel request) {
            if (request == null) {
                throw new IllegalArgumentException("Request cannot be null.");
            }

            if (request.getPlayerId() == null || request.getPlayerId() <= 0) {
                throw new IllegalArgumentException("Valid playerId is required.");
            }

            if (request.getBoardSize() == null || (request.getBoardSize() != 8 && request.getBoardSize() != 16)) {
                throw new IllegalArgumentException("Board size must be 8 or 16.");
            }
        }
    }