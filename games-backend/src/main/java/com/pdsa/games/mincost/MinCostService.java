package com.pdsa.games.mincost;

import com.pdsa.games.mincost.model.MinCostResponse;
import com.pdsa.games.mincost.service.AlgorithmSolveResult;
import com.pdsa.games.mincost.service.GreedyAssignmentService;
import com.pdsa.games.mincost.service.HungarianAssignmentService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
public class MinCostService {

    private static final int DEFAULT_N = 6;
    private static final int DEFAULT_MIN_COST = 1;
    private static final int DEFAULT_MAX_COST = 100;

    private final Random random = new Random();
    private final JdbcTemplate jdbcTemplate;
    private final GreedyAssignmentService greedyAssignmentService;
    private final HungarianAssignmentService hungarianAssignmentService;

    public MinCostService(
            JdbcTemplate jdbcTemplate,
            GreedyAssignmentService greedyAssignmentService,
            HungarianAssignmentService hungarianAssignmentService) {
        this.jdbcTemplate = jdbcTemplate;
        this.greedyAssignmentService = greedyAssignmentService;
        this.hungarianAssignmentService = hungarianAssignmentService;
    }

    public MinCostResponse startGame(MinCostStartRequest request) {
        MinCostStartRequest payload = request == null
                ? new MinCostStartRequest(null, null, null)
                : request;

        int n = pickN(payload.n());
        int minCost = payload.minCost() == null ? DEFAULT_MIN_COST : payload.minCost();
        int maxCost = payload.maxCost() == null ? DEFAULT_MAX_COST : payload.maxCost();

        validateInputs(n, minCost, maxCost);
        int[][] matrix = buildRandomMatrix(n, minCost, maxCost);

        long greedyStart = System.nanoTime();
        AlgorithmSolveResult greedyResult = greedyAssignmentService.solve(matrix);
        long greedyEnd = System.nanoTime();

        long hungarianStart = System.nanoTime();
        AlgorithmSolveResult hungarianResult = hungarianAssignmentService.solve(matrix);
        long hungarianEnd = System.nanoTime();

        double greedyMs = nanosToMs(greedyEnd - greedyStart);
        double hungarianMs = nanosToMs(hungarianEnd - hungarianStart);

        saveRunSafely(n, hungarianResult.totalCost(), greedyMs, hungarianMs);

        return new MinCostResponse(
                matrix,
                greedyResult.assignments(),
                hungarianResult.assignments(),
                greedyResult.logs(),
                hungarianResult.logs(),
                greedyResult.totalCost(),
                hungarianResult.totalCost(),
                greedyMs,
                hungarianMs,
                hungarianResult.hungarianSteps()
        );
    }

    public List<MinCostHistoryItem> getHistory() {
        String sql = "SELECT gs.Created_At, mcg.N_Value, mcg.Minimum_Total_Cost, " +
                "ae_g.Execution_Time_MS as Greedy_Time, ae_h.Execution_Time_MS as Hungarian_Time " +
                "FROM Game_Session gs " +
                "JOIN Minimum_Cost_Game mcg ON gs.Session_Id = mcg.Session_Id " +
                "LEFT JOIN Algorithm_Execute ae_g ON gs.Session_Id = ae_g.Session_Id AND ae_g.Algorithm_Id = " +
                "(SELECT Algorithm_Id FROM Algorithm WHERE Algorithm_Name = 'Minimum Cost - Greedy') " +
                "LEFT JOIN Algorithm_Execute ae_h ON gs.Session_Id = ae_h.Session_Id AND ae_h.Algorithm_Id = " +
                "(SELECT Algorithm_Id FROM Algorithm WHERE Algorithm_Name = 'Minimum Cost - Hungarian') " +
                "ORDER BY gs.Created_At DESC LIMIT 10";

        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> new MinCostHistoryItem(
                    rs.getString("Created_At"),
                    rs.getInt("N_Value"),
                    rs.getInt("Minimum_Total_Cost"),
                    rs.getDouble("Greedy_Time"),
                    rs.getDouble("Hungarian_Time")
            ));
        } catch (Exception e) {
            return List.of();
        }
    }

    private int pickN(Integer requestedN) {
        if (requestedN != null) {
            return requestedN;
        }
        return DEFAULT_N;
    }

    private void validateInputs(int n, int minCost, int maxCost) {
        if (n < 5 || n > 100) {
            throw new IllegalArgumentException("N should be between 5 and 100");
        }
        if (minCost < 1 || maxCost < 1) {
            throw new IllegalArgumentException("costs must be positive values");
        }
        if (minCost > maxCost) {
            throw new IllegalArgumentException("min cost cannot be more than max cost");
        }
    }

    private int[][] buildRandomMatrix(int n, int minCost, int maxCost) {
        int[][] matrix = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                matrix[i][j] = minCost + random.nextInt(maxCost - minCost + 1);
            }
        }
        return matrix;
    }

    private double nanosToMs(long nanos) {
        return nanos / 1_000_000.0;
    }

    private void saveRunSafely(int n, int minimumTotalCost, double greedyMs, double hungarianMs) {
        try {
            Integer gameId = jdbcTemplate.queryForObject(
                    "SELECT Game_Id FROM Game WHERE Game_Name = ?",
                    Integer.class,
                    "Minimum Cost"
            );

            if (gameId == null) {
                jdbcTemplate.update("INSERT INTO Game (Game_Name) VALUES (?)", "Minimum Cost");
                gameId = jdbcTemplate.queryForObject(
                        "SELECT Game_Id FROM Game WHERE Game_Name = ?",
                        Integer.class,
                        "Minimum Cost"
                );
            }

            jdbcTemplate.update("INSERT INTO Player (Player_Name) VALUES (?)", "System");
            Integer playerId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);

            jdbcTemplate.update("INSERT INTO Game_Session (Game_Id, Player_Id) VALUES (?, ?)", gameId, playerId);
            Integer sessionId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);

            jdbcTemplate.update(
                    "INSERT INTO Minimum_Cost_Game (Session_Id, N_Value, Minimum_Total_Cost) VALUES (?, ?, ?)",
                    sessionId, n, minimumTotalCost
            );

            Integer greedyAlgorithmId = ensureAlgorithm(gameId, "Minimum Cost - Greedy");
            Integer hungarianAlgorithmId = ensureAlgorithm(gameId, "Minimum Cost - Hungarian");

            jdbcTemplate.update(
                    "INSERT INTO Algorithm_Execute (Session_Id, Algorithm_Id, Execution_Time_MS, Output_Result) VALUES (?, ?, ?, 'SUCCESS')",
                    sessionId, greedyAlgorithmId, greedyMs
            );
            jdbcTemplate.update(
                    "INSERT INTO Algorithm_Execute (Session_Id, Algorithm_Id, Execution_Time_MS, Output_Result) VALUES (?, ?, ?, 'SUCCESS')",
                    sessionId, hungarianAlgorithmId, hungarianMs
            );
        } catch (Exception ignored) {
            // DB optional for local dev
        }
    }

    private Integer ensureAlgorithm(Integer gameId, String algorithmName) {
        Integer id = jdbcTemplate.query(
                "SELECT Algorithm_Id FROM Algorithm WHERE Algorithm_Name = ?",
                rs -> rs.next() ? rs.getInt("Algorithm_Id") : null,
                algorithmName
        );
        if (id != null) {
            return id;
        }
        jdbcTemplate.update("INSERT INTO Algorithm (Game_Id, Algorithm_Name) VALUES (?, ?)", gameId, algorithmName);
        return jdbcTemplate.queryForObject(
                "SELECT Algorithm_Id FROM Algorithm WHERE Algorithm_Name = ?",
                Integer.class,
                algorithmName
        );
    }

    public record MinCostStartRequest(Integer n, Integer minCost, Integer maxCost) {
    }

    public record MinCostHistoryItem(
            String date,
            int n,
            int cost,
            double greedyTime,
            double hungarianTime
    ) {
    }
}
