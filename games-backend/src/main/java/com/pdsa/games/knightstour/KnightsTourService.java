package com.pdsa.games.knightstour;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

@Service
public class KnightsTourService {

    private static final int[] DX = {2, 1, -1, -2, -2, -1, 1, 2};
    private static final int[] DY = {1, 2, 2, 1, -1, -2, -2, -1};

    private static final String WARNSDORFF_NAME = "Warnsdorff";
    private static final int MAX_ATTEMPTS_TO_GENERATE = 40;

    private final KnightsTourRepository repository;
    private final Random random = new Random();

    public KnightsTourService(KnightsTourRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public KnightsTourModel.StartResponse startRound(KnightsTourModel.StartRequest request) {
        validateStartRequest(request);

        if (!repository.playerExists(request.getPlayerId())) {
            throw new IllegalArgumentException("Player does not exist.");
        }

        int algorithmId = mustFindAlgorithmId(WARNSDORFF_NAME);
        GeneratedRound round = generateSuccessfulRound(request.getBoardSize());

        Long sessionId = repository.createGameSession(request.getPlayerId());

        repository.saveKnightTourGame(
                sessionId,
                request.getBoardSize(),
                round.startX + 1,
                round.startY + 1,
                round.result.getMoveCount()
        );

        repository.saveAlgorithmExecution(
                sessionId,
                algorithmId,
                round.result.getExecutionTimeMs(),
                normalizeStatusForDb(round.result.getStatus())
        );

        repository.saveKnightMoves(
                sessionId,
                algorithmId,
                round.result.getMoves()
        );

        KnightsTourModel.StartResponse response = new KnightsTourModel.StartResponse();
        response.setSessionId(sessionId);
        response.setPlayerId(request.getPlayerId());
        response.setBoardSize(request.getBoardSize());
        response.setStartX(round.startX + 1);
        response.setStartY(round.startY + 1);
        response.setExpectedMoveCount(round.result.getMoveCount());
        response.setAlgorithmName(round.result.getAlgorithmName());
        response.setAlgorithmExecutionTimeMs(round.result.getExecutionTimeMs());
        response.setStatus("READY");
        response.setMessage("Knight's Tour round generated successfully.");

        return response;
    }

    @Transactional
    public KnightsTourModel.AnswerResponse submitAnswer(KnightsTourModel.AnswerRequest request) {
        validateAnswerRequest(request);

        if (!repository.playerExists(request.getPlayerId())) {
            throw new IllegalArgumentException("Player does not exist.");
        }

        if (!repository.sessionBelongsToPlayer(request.getSessionId(), request.getPlayerId())) {
            throw new IllegalArgumentException("This session does not belong to the given player.");
        }

        KnightsTourModel.GameConfig gameConfig = repository.findGameConfig(request.getSessionId());
        if (gameConfig == null) {
            throw new IllegalStateException("Knight Tour game configuration not found for this session.");
        }

        int algorithmId = mustFindAlgorithmId(WARNSDORFF_NAME);
        List<KnightsTourModel.Move> warnsdorffCorrectMoves =
                repository.findCorrectMoves(request.getSessionId(), algorithmId);

        if (warnsdorffCorrectMoves.isEmpty()) {
            throw new IllegalStateException("Correct Warnsdorff path not found for this session.");
        }

        List<KnightsTourModel.Move> submittedMoves = normalizeMovesByStep(request.getMoves());

        StructuralValidationResult structural = validateStructure(
                submittedMoves,
                gameConfig.getBoardSize(),
                gameConfig.getStartX(),
                gameConfig.getStartY(),
                gameConfig.getMoveCount()
        );

        boolean warnsdorffValid = false;
        String validationSource = null;
        String outcome;
        String reasonCode;
        String message;

        if (!structural.isValid()) {
            outcome = "LOSE";
            reasonCode = structural.getReasonCode();
            message = buildStructuralFailureMessage(reasonCode);
        } else {
            warnsdorffValid = areMovesExactlyEqual(warnsdorffCorrectMoves, submittedMoves);

            if (warnsdorffValid) {
                outcome = "WIN";
                validationSource = "WARNSDORFF";
                reasonCode = "VALID_TOUR";
                message = "Correct answer submitted. Tour validated by Warnsdorff.";
            } else {
                outcome = "LOSE";
                reasonCode = "FAILED_WARNSDORFF_VALIDATION";
                message = "Submitted path is structurally valid but does not match the correct Warnsdorff solution.";
            }
        }

        boolean isCorrect = "WIN".equals(outcome);

        String responseSummary = buildResponseSummary(
                submittedMoves.size(),
                isCorrect,
                reasonCode,
                validationSource
        );

        Long responseId = repository.saveResponse(
                request.getSessionId(),
                responseSummary,
                isCorrect
        );

        repository.saveResponseMoves(responseId, submittedMoves);

        KnightsTourModel.AnswerResponse response = new KnightsTourModel.AnswerResponse();
        response.setResponseId(responseId);
        response.setSessionId(request.getSessionId());
        response.setPlayerId(request.getPlayerId());
        response.setCorrect(isCorrect);
        response.setOutcome(outcome);
        response.setExpectedMoveCount(gameConfig.getMoveCount());
        response.setSubmittedMoveCount(submittedMoves.size());
        response.setStructuralValid(structural.isValid());
        response.setCompleteTour(structural.isComplete());
        response.setDeadEnd(structural.isDeadEnd());
        response.setWarnsdorffValid(warnsdorffValid);
        response.setBacktrackingValid(false); // placeholder only
        response.setValidationSource(validationSource);
        response.setReasonCode(reasonCode);
        response.setMessage(message);

        if (!isCorrect) {
            response.setWarnsdorffCorrectMoves(warnsdorffCorrectMoves);
            response.setWarnsdorffMoveCount(warnsdorffCorrectMoves.size());
            response.setBacktrackingCorrectMoves(new ArrayList<>()); // placeholder only
            response.setBacktrackingMoveCount(0); // placeholder only
        }

        return response;
    }

    @Transactional(readOnly = true)
    public KnightsTourModel.CorrectAnswerResponse getCorrectAnswer(Long sessionId, Long playerId) {
        if (sessionId == null || sessionId <= 0) {
            throw new IllegalArgumentException("Valid sessionId is required.");
        }

        if (playerId == null || playerId <= 0) {
            throw new IllegalArgumentException("Valid playerId is required.");
        }

        if (!repository.playerExists(playerId)) {
            throw new IllegalArgumentException("Player does not exist.");
        }

        if (!repository.sessionExists(sessionId)) {
            throw new IllegalArgumentException("Session does not exist.");
        }

        if (!repository.sessionBelongsToPlayer(sessionId, playerId)) {
            throw new IllegalArgumentException("This session does not belong to the given player.");
        }

        int algorithmId = mustFindAlgorithmId(WARNSDORFF_NAME);
        List<KnightsTourModel.Move> correctMoves = repository.findCorrectMoves(sessionId, algorithmId);

        if (correctMoves.isEmpty()) {
            throw new IllegalStateException("Correct Knight Tour path not found for this session.");
        }

        KnightsTourModel.CorrectAnswerResponse response = new KnightsTourModel.CorrectAnswerResponse();
        response.setSessionId(sessionId);
        response.setPlayerId(playerId);
        response.setAlgorithmName(WARNSDORFF_NAME);
        response.setMoveCount(correctMoves.size());
        response.setMoves(correctMoves);
        response.setMessage("Correct answer loaded successfully.");

        return response;
    }

    private GeneratedRound generateSuccessfulRound(int boardSize) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS_TO_GENERATE; attempt++) {
            int startX = random.nextInt(boardSize);
            int startY = random.nextInt(boardSize);

            KnightsTourModel.AlgorithmResult result = executeWarnsdorff(boardSize, startX, startY);

            if ("SUCCESS".equals(result.getStatus())) {
                return new GeneratedRound(startX, startY, result);
            }
        }

        throw new IllegalStateException("Could not generate a full Knight's Tour after several attempts.");
    }

    private int mustFindAlgorithmId(String algorithmName) {
        Integer algorithmId = repository.findAlgorithmId(algorithmName);
        if (algorithmId == null) {
            throw new IllegalStateException("Algorithm '" + algorithmName + "' not found.");
        }
        return algorithmId;
    }

    private KnightsTourModel.AlgorithmResult executeWarnsdorff(int boardSize, int startX, int startY) {
        long startNano = System.nanoTime();

        KnightsTourModel.AlgorithmResult result = new KnightsTourModel.AlgorithmResult();
        result.setAlgorithmName(WARNSDORFF_NAME);

        try {
            List<KnightsTourModel.Move> moves = solveWarnsdorff(boardSize, startX, startY);
            result.setStatus("SUCCESS");
            result.setExecutionTimeMs(elapsedMs(startNano));
            result.setMoveCount(moves.size());
            result.setMoves(moves);
            result.setMessage("Warnsdorff completed the full tour successfully.");
        } catch (Exception e) {
            result.setStatus("ERROR");
            result.setExecutionTimeMs(elapsedMs(startNano));
            result.setMoveCount(0);
            result.setMoves(new ArrayList<>());
            result.setMessage(e.getMessage());
        }

        return result;
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

            if (isValidZeroBased(nextX, nextY, boardSize) && board[nextX][nextY] == -1) {
                int onwardMoves = countOnwardMoves(board, nextX, nextY, boardSize);
                candidates.add(new int[]{nextX, nextY, onwardMoves});
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        candidates.sort(Comparator.comparingInt(a -> a[2]));

        int minimumOnwardMoves = candidates.get(0)[2];
        List<int[]> bestCandidates = new ArrayList<>();

        for (int[] candidate : candidates) {
            if (candidate[2] == minimumOnwardMoves) {
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

            if (isValidZeroBased(nextX, nextY, boardSize) && board[nextX][nextY] == -1) {
                count++;
            }
        }

        return count;
    }

    private StructuralValidationResult validateStructure(
            List<KnightsTourModel.Move> submittedMoves,
            int boardSize,
            int expectedStartX,
            int expectedStartY,
            int expectedMoveCount
    ) {
        if (submittedMoves.isEmpty()) {
            return StructuralValidationResult.invalid("EMPTY_PATH");
        }

        Set<String> visitedCells = new HashSet<>();

        for (int i = 0; i < submittedMoves.size(); i++) {
            KnightsTourModel.Move current = submittedMoves.get(i);

            int expectedStepNo = i + 1;
            if (current.getStepNo() != expectedStepNo) {
                return StructuralValidationResult.invalid("INVALID_STEP_SEQUENCE");
            }

            if (!isWithinBoard(current.getX(), current.getY(), boardSize)) {
                return StructuralValidationResult.invalid("OUT_OF_BOUNDS");
            }

            String cellKey = current.getX() + "_" + current.getY();
            if (!visitedCells.add(cellKey)) {
                return StructuralValidationResult.invalid("DUPLICATE_CELL");
            }

            if (i == 0) {
                if (current.getX() != expectedStartX || current.getY() != expectedStartY) {
                    return StructuralValidationResult.invalid("WRONG_START_POSITION");
                }
            } else {
                KnightsTourModel.Move previous = submittedMoves.get(i - 1);
                if (!isKnightMove(previous.getX(), previous.getY(), current.getX(), current.getY())) {
                    return StructuralValidationResult.invalid("ILLEGAL_MOVE_SEQUENCE");
                }
            }
        }

        boolean complete = submittedMoves.size() == expectedMoveCount;

        if (!complete) {
            KnightsTourModel.Move lastMove = submittedMoves.get(submittedMoves.size() - 1);
            boolean deadEnd = hasNoLegalUnvisitedMove(lastMove, visitedCells, boardSize);

            if (deadEnd) {
                return StructuralValidationResult.deadEnd("DEAD_END_BEFORE_COMPLETION");
            }

            return StructuralValidationResult.incomplete("INCOMPLETE_TOUR");
        }

        return StructuralValidationResult.validComplete();
    }

    private boolean hasNoLegalUnvisitedMove(
            KnightsTourModel.Move lastMove,
            Set<String> visitedCells,
            int boardSize
    ) {
        for (int i = 0; i < 8; i++) {
            int nextX = lastMove.getX() + DX[i];
            int nextY = lastMove.getY() + DY[i];

            if (isWithinBoard(nextX, nextY, boardSize)) {
                String key = nextX + "_" + nextY;
                if (!visitedCells.contains(key)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isKnightMove(int fromX, int fromY, int toX, int toY) {
        int dx = Math.abs(toX - fromX);
        int dy = Math.abs(toY - fromY);
        return (dx == 2 && dy == 1) || (dx == 1 && dy == 2);
    }

    private boolean isWithinBoard(int x, int y, int boardSize) {
        return x >= 1 && x <= boardSize && y >= 1 && y <= boardSize;
    }

    private List<KnightsTourModel.Move> normalizeMovesByStep(List<KnightsTourModel.Move> moves) {
        List<KnightsTourModel.Move> normalized = new ArrayList<>(moves);
        normalized.sort(Comparator.comparingInt(KnightsTourModel.Move::getStepNo));
        return normalized;
    }

    private boolean areMovesExactlyEqual(
            List<KnightsTourModel.Move> correctMoves,
            List<KnightsTourModel.Move> submittedMoves
    ) {
        if (correctMoves.size() != submittedMoves.size()) {
            return false;
        }

        for (int i = 0; i < correctMoves.size(); i++) {
            KnightsTourModel.Move correct = correctMoves.get(i);
            KnightsTourModel.Move submitted = submittedMoves.get(i);

            if (correct.getStepNo() != submitted.getStepNo()
                    || correct.getX() != submitted.getX()
                    || correct.getY() != submitted.getY()) {
                return false;
            }
        }

        return true;
    }

    private String buildStructuralFailureMessage(String reasonCode) {
        return switch (reasonCode) {
            case "WRONG_START_POSITION" -> "Submitted path does not start from the required starting square.";
            case "OUT_OF_BOUNDS" -> "Submitted path contains coordinates outside the board.";
            case "DUPLICATE_CELL" -> "Submitted path visits the same square more than once.";
            case "INVALID_STEP_SEQUENCE" -> "Submitted path contains invalid or missing step numbers.";
            case "ILLEGAL_MOVE_SEQUENCE" -> "Submitted path contains a move that is not a legal knight move.";
            case "DEAD_END_BEFORE_COMPLETION" -> "Player reached a dead end before completing the full tour.";
            case "INCOMPLETE_TOUR" -> "Submitted path is incomplete and the tour was not finished.";
            case "EMPTY_PATH" -> "No moves were submitted.";
            default -> "Submitted path failed structural validation.";
        };
    }

    private String buildResponseSummary(
            int submittedMoveCount,
            boolean isCorrect,
            String reasonCode,
            String validationSource
    ) {
        String resultPrefix = isCorrect ? "CORRECT" : "WRONG";
        String source = validationSource == null ? "NONE" : validationSource;
        return resultPrefix + "_" + submittedMoveCount + "_MOVES_" + reasonCode + "_" + source;
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

    private boolean isValidZeroBased(int x, int y, int boardSize) {
        return x >= 0 && x < boardSize && y >= 0 && y < boardSize;
    }

    private double elapsedMs(long startNano) {
        return (System.nanoTime() - startNano) / 1_000_000.0;
    }

    private String normalizeStatusForDb(String status) {
        return "SUCCESS".equals(status) ? "SUCCESS" : "ERROR";
    }

    private void validateStartRequest(KnightsTourModel.StartRequest request) {
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

    private void validateAnswerRequest(KnightsTourModel.AnswerRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null.");
        }

        if (request.getSessionId() == null || request.getSessionId() <= 0) {
            throw new IllegalArgumentException("Valid sessionId is required.");
        }

        if (request.getPlayerId() == null || request.getPlayerId() <= 0) {
            throw new IllegalArgumentException("Valid playerId is required.");
        }

        if (request.getMoves() == null || request.getMoves().isEmpty()) {
            throw new IllegalArgumentException("Move sequence is required.");
        }

        Set<Integer> seenSteps = new HashSet<>();
        Set<String> seenCells = new HashSet<>();

        for (KnightsTourModel.Move move : request.getMoves()) {
            if (move == null) {
                throw new IllegalArgumentException("Move list contains null entry.");
            }

            if (move.getStepNo() <= 0) {
                throw new IllegalArgumentException("Each move must have a positive step number.");
            }

            if (move.getX() <= 0 || move.getY() <= 0) {
                throw new IllegalArgumentException("Coordinates must be positive.");
            }

            if (!seenSteps.add(move.getStepNo())) {
                throw new IllegalArgumentException("Duplicate step number found: " + move.getStepNo());
            }

            String cellKey = move.getX() + "_" + move.getY();
            if (!seenCells.add(cellKey)) {
                throw new IllegalArgumentException(
                        "Duplicate board cell found in response: (" + move.getX() + "," + move.getY() + ")"
                );
            }
        }
    }

    private static class GeneratedRound {
        private final int startX;
        private final int startY;
        private final KnightsTourModel.AlgorithmResult result;

        public GeneratedRound(int startX, int startY, KnightsTourModel.AlgorithmResult result) {
            this.startX = startX;
            this.startY = startY;
            this.result = result;
        }
    }

    private static class StructuralValidationResult {
        private final boolean valid;
        private final boolean complete;
        private final boolean deadEnd;
        private final String reasonCode;

        private StructuralValidationResult(boolean valid, boolean complete, boolean deadEnd, String reasonCode) {
            this.valid = valid;
            this.complete = complete;
            this.deadEnd = deadEnd;
            this.reasonCode = reasonCode;
        }

        public static StructuralValidationResult validComplete() {
            return new StructuralValidationResult(true, true, false, "VALID_STRUCTURE");
        }

        public static StructuralValidationResult invalid(String reasonCode) {
            return new StructuralValidationResult(false, false, false, reasonCode);
        }

        public static StructuralValidationResult deadEnd(String reasonCode) {
            return new StructuralValidationResult(false, false, true, reasonCode);
        }

        public static StructuralValidationResult incomplete(String reasonCode) {
            return new StructuralValidationResult(false, false, false, reasonCode);
        }

        public boolean isValid() {
            return valid;
        }

        public boolean isComplete() {
            return complete;
        }

        public boolean isDeadEnd() {
            return deadEnd;
        }

        public String getReasonCode() {
            return reasonCode;
        }
    }
}