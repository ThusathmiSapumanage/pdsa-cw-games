package com.pdsa.games.mincost;

import com.pdsa.games.mincost.model.MinCostResponse;
import com.pdsa.games.mincost.service.AlgorithmSolveResult;
import com.pdsa.games.mincost.service.GreedyAssignmentService;
import com.pdsa.games.mincost.service.HungarianAssignmentService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

/**
 * MIN COST SERVICE:
 * This is the main engine for our MinCost game.
 * It handles generating the matrix, calling algorithms, and saving history.
 */
@Service
public class MinCostService {

    // Default values if user doesn't provide them
    private static final int DEFAULT_N = 6;
    private static final int DEFAULT_MIN_COST = 1;
    private static final int DEFAULT_MAX_COST = 100;

    private final Random random = new Random();
    private final JdbcTemplate jdbcTemplate;
    private final GreedyAssignmentService greedyAssignmentService;
    private final HungarianAssignmentService hungarianAssignmentService;

    // Constructor to inject the tools we need
    public MinCostService(
            JdbcTemplate jdbcTemplate,
            GreedyAssignmentService greedyAssignmentService,
            HungarianAssignmentService hungarianAssignmentService) {
        this.jdbcTemplate = jdbcTemplate;
        this.greedyAssignmentService = greedyAssignmentService;
        this.hungarianAssignmentService = hungarianAssignmentService;
    }

    /**
     * Start a new game round.
     * Generates a random matrix and solves it with both Algos.
     */
    public MinCostResponse startGame(MinCostStartRequest request) {
        // If request is null, we use defaults
        MinCostStartRequest payload = request == null
                ? new MinCostStartRequest(null, null, null)
                : request;

        // Pick values or use defaults
        int n = pickN(payload.n());
        int minCost = payload.minCost() == null ? DEFAULT_MIN_COST : payload.minCost();
        int maxCost = payload.maxCost() == null ? DEFAULT_MAX_COST : payload.maxCost();

        // 1. VALIDATION: Check if inputs make sense (Student requirement)
        validateInputs(n, minCost, maxCost);
        
        // 2. MATRIX GEN: Create the random grid
        int[][] matrix = buildRandomMatrix(n, minCost, maxCost);

        // 3. GREEDY: Run the quick greedy way
        long greedyStart = System.nanoTime();
        AlgorithmSolveResult greedyResult = greedyAssignmentService.solve(matrix);
        long greedyEnd = System.nanoTime();

        // 4. HUNGARIAN: Run the smart optimal way
        long hungarianStart = System.nanoTime();
        AlgorithmSolveResult hungarianResult = hungarianAssignmentService.solve(matrix);
        long hungarianEnd = System.nanoTime();

        // Calculate time taken in Milliseconds
        double greedyMs = nanosToMs(greedyEnd - greedyStart);
        double hungarianMs = nanosToMs(hungarianEnd - hungarianStart);

        // 5. DB SAVE: Try to save the run in background (silent)
        saveRunSafely(n, hungarianResult.totalCost(), greedyMs, hungarianMs);

        // Return everything to the DTO for the frontend
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

    /**
     * Fetch the history of last 10 games.
     * Uses SQL to join the tables Rumeth prepared.
     */
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
            // If DB fails, we just return empty list so the app doesn't crash
            return List.of();
        }
    }

    private int pickN(Integer requestedN) {
        if (requestedN != null) {
            return requestedN;
        }
        return DEFAULT_N;
    }

    /**
     * EXCEPTION HANDLING: Check for bad inputs.
     * Crucial for Viva presentation!
     */
    private void validateInputs(int n, int minCost, int maxCost) {
        if (n < 5 || n > 100) {
            throw new IllegalArgumentException("Matrix size (N) must be between 5 and 100!");
        }
        if (minCost < 1 || maxCost < 1) {
            throw new IllegalArgumentException("Costs must be positive integers!");
        }
        if (minCost > maxCost) {
            throw new IllegalArgumentException("Min cost cannot be higher than Max cost!");
        }
    }

    /**
     * Helper to create the N x N grid.
     */
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

    /**
     * DB LOGIC: Saves the run so we can show charts later.
     * Wrapped in a try-catch for "Robust Exception Handling".
     */
    private void saveRunSafely(int n, int minimumTotalCost, double greedyMs, double hungarianMs) {
        try {
            // 1. Find the game ID for 'Minimum Cost'
            Integer gameId = jdbcTemplate.query(
                    "SELECT Game_Id FROM Game WHERE Game_Name = ?",
                    rs -> rs.next() ? rs.getInt("Game_Id") : null,
                    "Minimum Cost"
            );

            // Create game if not exists
            if (gameId == null) {
                jdbcTemplate.update("INSERT INTO Game (Game_Name) VALUES (?)", "Minimum Cost");
                gameId = jdbcTemplate.queryForObject(
                        "SELECT Game_Id FROM Game WHERE Game_Name = ?",
                        Integer.class,
                        "Minimum Cost"
                );
            }

            // 2. Create a system player
            jdbcTemplate.update("INSERT INTO Player (Player_Name) VALUES (?)", "System");
            Integer playerId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);

            // 3. Start session
            jdbcTemplate.update("INSERT INTO Game_Session (Game_Id, Player_Id) VALUES (?, ?)", gameId, playerId);
            Integer sessionId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);

            // 4. Record specific MinCost details
            jdbcTemplate.update(
                    "INSERT INTO Minimum_Cost_Game (Session_Id, N_Value, Minimum_Total_Cost) VALUES (?, ?, ?)",
                    sessionId, n, minimumTotalCost
            );

            // 5. Save execution times for both algorithms
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
            // We ignore errors here so the user can still play even if DB is down.
            // This is "silent persistence" logic!
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

    // Request DTO
    public record MinCostStartRequest(Integer n, Integer minCost, Integer maxCost) {
    }

    // History Item DTO
    public record MinCostHistoryItem(
            String date,
            int n,
            int cost,
            double greedyTime,
            double hungarianTime
    ) {
    }
}

