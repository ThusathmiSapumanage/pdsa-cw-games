package com.pdsa.games.knightstour;

import java.util.List;

public class KnightsTourModel {

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

    public static class StartRequest {
        private Long playerId;
        private Integer boardSize;

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
    }

    public static class StartResponse {
        private Long sessionId;
        private Long playerId;
        private Integer boardSize;
        private Integer startX;
        private Integer startY;
        private Integer expectedMoveCount;
        private String algorithmName;
        private Double algorithmExecutionTimeMs;
        private String status;
        private String message;

        public Long getSessionId() {
            return sessionId;
        }

        public void setSessionId(Long sessionId) {
            this.sessionId = sessionId;
        }

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

        public Integer getExpectedMoveCount() {
            return expectedMoveCount;
        }

        public void setExpectedMoveCount(Integer expectedMoveCount) {
            this.expectedMoveCount = expectedMoveCount;
        }

        public String getAlgorithmName() {
            return algorithmName;
        }

        public void setAlgorithmName(String algorithmName) {
            this.algorithmName = algorithmName;
        }

        public Double getAlgorithmExecutionTimeMs() {
            return algorithmExecutionTimeMs;
        }

        public void setAlgorithmExecutionTimeMs(Double algorithmExecutionTimeMs) {
            this.algorithmExecutionTimeMs = algorithmExecutionTimeMs;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class AnswerRequest {
        private Long sessionId;
        private Long playerId;
        private List<Move> moves;

        public Long getSessionId() {
            return sessionId;
        }

        public void setSessionId(Long sessionId) {
            this.sessionId = sessionId;
        }

        public Long getPlayerId() {
            return playerId;
        }

        public void setPlayerId(Long playerId) {
            this.playerId = playerId;
        }

        public List<Move> getMoves() {
            return moves;
        }

        public void setMoves(List<Move> moves) {
            this.moves = moves;
        }
    }

    public static class AnswerResponse {
        private Long responseId;
        private Long sessionId;
        private Long playerId;
        private Boolean correct;
        private String outcome;
        private Integer expectedMoveCount;
        private Integer submittedMoveCount;
        private String message;

        public Long getResponseId() {
            return responseId;
        }

        public void setResponseId(Long responseId) {
            this.responseId = responseId;
        }

        public Long getSessionId() {
            return sessionId;
        }

        public void setSessionId(Long sessionId) {
            this.sessionId = sessionId;
        }

        public Long getPlayerId() {
            return playerId;
        }

        public void setPlayerId(Long playerId) {
            this.playerId = playerId;
        }

        public Boolean getCorrect() {
            return correct;
        }

        public void setCorrect(Boolean correct) {
            this.correct = correct;
        }

        public String getOutcome() {
            return outcome;
        }

        public void setOutcome(String outcome) {
            this.outcome = outcome;
        }

        public Integer getExpectedMoveCount() {
            return expectedMoveCount;
        }

        public void setExpectedMoveCount(Integer expectedMoveCount) {
            this.expectedMoveCount = expectedMoveCount;
        }

        public Integer getSubmittedMoveCount() {
            return submittedMoveCount;
        }

        public void setSubmittedMoveCount(Integer submittedMoveCount) {
            this.submittedMoveCount = submittedMoveCount;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class CorrectAnswerResponse {
        private Long sessionId;
        private Long playerId;
        private String algorithmName;
        private Integer moveCount;
        private List<Move> moves;
        private String message;

        public Long getSessionId() {
            return sessionId;
        }

        public void setSessionId(Long sessionId) {
            this.sessionId = sessionId;
        }

        public Long getPlayerId() {
            return playerId;
        }

        public void setPlayerId(Long playerId) {
            this.playerId = playerId;
        }

        public String getAlgorithmName() {
            return algorithmName;
        }

        public void setAlgorithmName(String algorithmName) {
            this.algorithmName = algorithmName;
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