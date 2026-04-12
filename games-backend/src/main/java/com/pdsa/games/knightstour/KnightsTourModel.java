package com.pdsa.games.knightstour;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class KnightsTourModel {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Move {
        private int stepNo;
        private int x;
        private int y;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GameConfig {
        private Long sessionId;
        private Integer boardSize;
        private Integer startX;
        private Integer startY;
        private Integer moveCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlgorithmResult {
        private String algorithmName;
        private String status;
        private Double executionTimeMs;
        private Integer moveCount;
        private List<Move> moves;
        private String message;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StartRequest {
        private Long playerId;
        private Integer boardSize;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
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
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerRequest {
        private Long sessionId;
        private Long playerId;
        private List<Move> moves;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HintRequest {
        private Long sessionId;
        private Long playerId;
        private List<Move> moves;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HintResponse {
        private Long sessionId;
        private Long playerId;
        private Integer fromStepNo;
        private Integer fromX;
        private Integer fromY;
        private List<Move> hintMoves;
        private String message;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerResponse {
        private Long responseId;
        private Long sessionId;
        private Long playerId;
        private Boolean correct;
        private String outcome;
        private Integer expectedMoveCount;
        private Integer submittedMoveCount;
        private Boolean structuralValid;
        private Boolean completeTour;
        private Boolean deadEnd;
        private Boolean warnsdorffValid;
        private Boolean backtrackingValid;
        private String validationSource;
        private String reasonCode;
        private List<Move> warnsdorffCorrectMoves;
        private Integer warnsdorffMoveCount;
        private List<Move> backtrackingCorrectMoves;
        private Integer backtrackingMoveCount;
        private String message;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CorrectAnswerResponse {
        private Long sessionId;
        private Long playerId;
        private String algorithmName;
        private Integer moveCount;
        private List<Move> moves;
        private String message;
    }
}