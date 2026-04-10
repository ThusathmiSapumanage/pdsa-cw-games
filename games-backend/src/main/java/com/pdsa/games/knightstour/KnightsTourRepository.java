package com.pdsa.games.knightstour;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class KnightsTourRepository {

    private final JdbcTemplate jdbcTemplate;

    public KnightsTourRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long createGameSession(Long playerId) {
        Integer gameId = jdbcTemplate.queryForObject(
                "SELECT Game_Id FROM Game WHERE Game_Name = ?",
                Integer.class,
                "Knight Tour"
        );

        if (gameId == null) {
            throw new IllegalStateException("Game 'Knight Tour' not found in Game table.");
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO Game_Session (Game_Id, Player_Id) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setInt(1, gameId);
            ps.setLong(2, playerId);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to create Game_Session.");
        }

        return key.longValue();
    }

    public Integer findAlgorithmId(String algorithmName) {
        return jdbcTemplate.queryForObject(
                "SELECT Algorithm_Id FROM Algorithm WHERE Algorithm_Name = ?",
                Integer.class,
                algorithmName
        );
    }

    public void saveKnightTourGame(Long sessionId, int boardSize, int startX, int startY, int moveCount) {
        jdbcTemplate.update(
                "INSERT INTO Knight_Tour_Game (Session_Id, Board_Size, X_Start, Y_Start, Move_Count) VALUES (?, ?, ?, ?, ?)",
                sessionId, boardSize, startX, startY, moveCount
        );
    }

    public void saveKnightMoves(Long sessionId, int algorithmId, List<KnightsTourModel.Move> moves) {
        jdbcTemplate.batchUpdate(
                "INSERT INTO Knight_Move (Session_Id, Algorithm_Id, Step_No, X_Value, Y_Value) VALUES (?, ?, ?, ?, ?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws java.sql.SQLException {
                        KnightsTourModel.Move move = moves.get(i);
                        ps.setLong(1, sessionId);
                        ps.setInt(2, algorithmId);
                        ps.setInt(3, move.getStepNo());
                        ps.setInt(4, move.getX());
                        ps.setInt(5, move.getY());
                    }

                    @Override
                    public int getBatchSize() {
                        return moves.size();
                    }
                }
        );
    }

    public void saveAlgorithmExecution(Long sessionId, int algorithmId, double executionTimeMs, String outputResult) {
        jdbcTemplate.update(
                "INSERT INTO Algorithm_Execute (Session_Id, Algorithm_Id, Execution_Time_MS, Output_Result) VALUES (?, ?, ?, ?)",
                sessionId, algorithmId, executionTimeMs, outputResult
        );
    }
}