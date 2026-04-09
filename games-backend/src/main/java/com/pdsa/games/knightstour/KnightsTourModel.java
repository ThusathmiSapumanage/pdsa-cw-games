package com.pdsa.games.knightstour;

import java.util.List;

public class KnightsTourModel {

    private Long playerId;
    private Integer boardSize;

    private Long sessionId;
    private Integer startX;
    private Integer startY;

    private AlgorithmResult warnsdorffResult;
    private AlgorithmResult backtrackingResult;

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public Integer getBoardSize() {
        return boardSize;
    }

    public void setBoardSize(Integer boardSize) {
        this.boardSize = boardSize;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Integer getStartX() {
        return startX;
    }

    public void setStartX(Integer startX) {
        this.startX = startX;
    }

    public Integer getStartY() {
        return startY;
    }

    public void setStartY(Integer startY) {
        this.startY = startY;
    }

    public AlgorithmResult getWarnsdorffResult() {
        return warnsdorffResult;
    }

    public void setWarnsdorffResult(AlgorithmResult warnsdorffResult) {
        this.warnsdorffResult = warnsdorffResult;
    }

    public AlgorithmResult getBacktrackingResult() {
        return backtrackingResult;
    }

    public void setBacktrackingResult(AlgorithmResult backtrackingResult) {
        this.backtrackingResult = backtrackingResult;
    }

    public static class Move {
        private int stepNo;
        private int x;
        private int y;

        public Move() {
        }

        public Move(int stepNo, int x, int y) {
            this.stepNo = stepNo;
            this.x = x;
            this.y = y;
        }

        public int getStepNo() {
            return stepNo;
        }

        public void setStepNo(int stepNo) {
            this.stepNo = stepNo;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }
    }

    public static class AlgorithmResult {
        private String algorithmName;
        private String status;
        private Double executionTimeMs;
        private Integer moveCount;
        private List<Move> moves;
        private String message;

        public String getAlgorithmName() {
            return algorithmName;
        }

        public void setAlgorithmName(String algorithmName) {
            this.algorithmName = algorithmName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Double getExecutionTimeMs() {
            return executionTimeMs;
        }

        public void setExecutionTimeMs(Double executionTimeMs) {
            this.executionTimeMs = executionTimeMs;
        }

        public Integer getMoveCount() {
            return moveCount;
        }

        public void setMoveCount(Integer moveCount) {
            this.moveCount = moveCount;
        }

        public List<Move> getMoves() {
            return moves;
        }

        public void setMoves(List<Move> moves) {
            this.moves = moves;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}