package com.pdsa.games.mincost;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Service
public class MinCostService {

    // Used to generate random N and random matrix costs.
    private final Random random = new Random();
    // Used for simple DB insert/select queries.
    private final JdbcTemplate jdbcTemplate;

    public MinCostService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Main method called by controller.
    // It validates input, runs both algorithms, measures time, and returns all data.
    public MinCostStartResponse startGame(MinCostStartRequest request) {
        // If user sends N, use it. If not, auto generate N.
        int n = pickN(request.n());
        // Keep default CW range if values are not sent.
        int minCost = request.minCost() == null ? 20 : request.minCost();
        int maxCost = request.maxCost() == null ? 200 : request.maxCost();
        // Stop early if any input is wrong.
        validateInputs(n, minCost, maxCost);

        // Build N x N matrix with random values.
        int[][] matrix = buildRandomMatrix(n, minCost, maxCost);

        // Run greedy and capture time.
        long greedyStart = System.nanoTime();
        AssignmentResult greedyResult = runGreedy(matrix);
        long greedyEnd = System.nanoTime();

        // Run Hungarian and capture time.
        long hungarianStart = System.nanoTime();
        AssignmentResult hungarianResult = runHungarian(matrix);
        long hungarianEnd = System.nanoTime();

        // Convert raw nano time into readable milliseconds.
        double greedyMs = nanosToMs(greedyEnd - greedyStart);
        double hungarianMs = nanosToMs(hungarianEnd - hungarianStart);

        // Save one game run into DB (if DB is available).
        saveRunSafely(n, hungarianResult.totalCost(), greedyMs, hungarianMs);

        // Send full response for frontend UI.
        return new MinCostStartResponse(
                matrix,
                greedyResult.totalCost(),
                hungarianResult.totalCost(),
                greedyResult.assignments(),
                hungarianResult.assignments(),
                greedyResult.logs(),
                List.of(
                        "Hungarian complete: total = " + hungarianResult.totalCost(),
                        "Greedy complete: total = " + greedyResult.totalCost()
                ),
                greedyMs,
                hungarianMs
        );
    }

    // Pick N from request, or generate random N in CW range 50..100.
    private int pickN(Integer requestedN) {
        if (requestedN != null) {
            return requestedN;
        }
        return 50 + random.nextInt(51);
    }

    // Simple validation for user inputs.
    private void validateInputs(int n, int minCost, int maxCost) {
        if (n < 5 || n > 100) {
            throw new IllegalArgumentException("N must be between 5 and 100");
        }
        if (minCost < 1 || maxCost < 1) {
            throw new IllegalArgumentException("Cost range must be positive");
        }
        if (minCost > maxCost) {
            throw new IllegalArgumentException("Min cost cannot be greater than max cost");
        }
    }

    // Creates random cost matrix.
    // Every cell is random between minCost and maxCost.
    private int[][] buildRandomMatrix(int n, int minCost, int maxCost) {
        int[][] matrix = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                matrix[i][j] = minCost + random.nextInt(maxCost - minCost + 1);
            }
        }
        return matrix;
    }

    // Greedy approach:
    // Each step picks current smallest free cell.
    // Then blocks that row and column from future picks.
    private AssignmentResult runGreedy(int[][] matrix) {
        int n = matrix.length;
        // Track which rows/cols are already used.
        boolean[] usedRows = new boolean[n];
        boolean[] usedCols = new boolean[n];
        // Store chosen pairs and user-friendly step logs.
        List<AssignmentPair> pairs = new ArrayList<>();
        List<String> logs = new ArrayList<>();
        int total = 0;

        for (int step = 0; step < n; step++) {
            // Find best currently available cell.
            int bestValue = Integer.MAX_VALUE;
            int bestRow = -1;
            int bestCol = -1;

            for (int row = 0; row < n; row++) {
                if (usedRows[row]) {
                    continue;
                }
                for (int col = 0; col < n; col++) {
                    if (usedCols[col]) {
                        continue;
                    }
                    if (matrix[row][col] < bestValue) {
                        bestValue = matrix[row][col];
                        bestRow = row;
                        bestCol = col;
                    }
                }
            }

            // Mark picked row/column as used.
            usedRows[bestRow] = true;
            usedCols[bestCol] = true;
            // Save assignment and update total cost.
            pairs.add(new AssignmentPair(bestRow, bestCol, bestValue));
            total += bestValue;
            // Add simple log line for UI.
            logs.add("Step " + (step + 1) + ": picked T" + (bestRow + 1)
                    + " -> E" + (bestCol + 1) + " (cost " + bestValue + ")");
        }

        return new AssignmentResult(total, pairs, logs);
    }

    // Hungarian algorithm (O(N^3)).
    // This gives the optimal assignment for minimum total cost.
    private AssignmentResult runHungarian(int[][] costs) {
        int n = costs.length;

        // Helper arrays used in Hungarian method.
        int[] u = new int[n + 1];
        int[] v = new int[n + 1];
        int[] p = new int[n + 1];
        int[] way = new int[n + 1];

        // Build matching one row at a time.
        for (int i = 1; i <= n; i++) {
            p[0] = i;
            int j0 = 0;
            int[] minv = new int[n + 1];
            boolean[] used = new boolean[n + 1];
            Arrays.fill(minv, Integer.MAX_VALUE);

            do {
                // Mark this column as visited in current search.
                used[j0] = true;
                int i0 = p[j0];
                int delta = Integer.MAX_VALUE;
                int j1 = 0;

                for (int j = 1; j <= n; j++) {
                    if (used[j]) {
                        continue;
                    }
                    // Reduced cost value for current edge.
                    int cur = costs[i0 - 1][j - 1] - u[i0] - v[j];
                    if (cur < minv[j]) {
                        minv[j] = cur;
                        way[j] = j0;
                    }
                    if (minv[j] < delta) {
                        delta = minv[j];
                        j1 = j;
                    }
                }

                // Update potentials.
                for (int j = 0; j <= n; j++) {
                    if (used[j]) {
                        u[p[j]] += delta;
                        v[j] -= delta;
                    } else {
                        minv[j] -= delta;
                    }
                }
                j0 = j1;
            } while (p[j0] != 0);

            // Rebuild matching path.
            do {
                int j1 = way[j0];
                p[j0] = p[j1];
                j0 = j1;
            } while (j0 != 0);
        }

        // Convert algorithm result into row -> col mapping.
        int[] assignmentByRow = new int[n];
        for (int j = 1; j <= n; j++) {
            assignmentByRow[p[j] - 1] = j - 1;
        }

        // Build final pairs and total.
        List<AssignmentPair> pairs = new ArrayList<>();
        int total = 0;
        for (int row = 0; row < n; row++) {
            int col = assignmentByRow[row];
            int value = costs[row][col];
            total += value;
            pairs.add(new AssignmentPair(row, col, value));
        }

        return new AssignmentResult(total, pairs, List.of());
    }

    // Convert nanoseconds to milliseconds and round to 3 decimals.
    private double nanosToMs(long nanos) {
        return Math.round((nanos / 1_000_000.0) * 1000.0) / 1000.0;
    }

    // Save game run details in database.
    // If DB is not configured yet, we silently skip save.
    private void saveRunSafely(int n, int minimumTotalCost, double greedyMs, double hungarianMs) {
        try {
            // Find game row first.
            Integer gameId = jdbcTemplate.queryForObject(
                    "SELECT Game_Id FROM Game WHERE Game_Name = ?",
                    Integer.class,
                    "Minimum Cost"
            );

            if (gameId == null) {
                // First run case: insert game row.
                jdbcTemplate.update("INSERT INTO Game (Game_Name) VALUES (?)", "Minimum Cost");
                gameId = jdbcTemplate.queryForObject(
                        "SELECT Game_Id FROM Game WHERE Game_Name = ?",
                        Integer.class,
                        "Minimum Cost"
                );
            }

            // Insert a simple system player for auto runs.
            jdbcTemplate.update("INSERT INTO Player (Player_Name) VALUES (?)", "System");
            Integer playerId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);

            // Create one game session.
            jdbcTemplate.update("INSERT INTO Game_Session (Game_Id, Player_Id) VALUES (?, ?)", gameId, playerId);
            Integer sessionId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);

            // Save minimum cost output.
            jdbcTemplate.update(
                    "INSERT INTO Minimum_Cost_Game (Session_Id, N_Value, Minimum_Total_Cost) VALUES (?, ?, ?)",
                    sessionId, n, minimumTotalCost
            );

            // Ensure both algorithm names exist.
            Integer greedyAlgorithmId = ensureAlgorithm(gameId, "Minimum Cost - Greedy");
            Integer hungarianAlgorithmId = ensureAlgorithm(gameId, "Minimum Cost - Hungarian");

            // Save greedy execution time.
            jdbcTemplate.update(
                    "INSERT INTO Algorithm_Execute (Session_Id, Algorithm_Id, Execution_Time_MS, Output_Result) VALUES (?, ?, ?, 'SUCCESS')",
                    sessionId, greedyAlgorithmId, greedyMs
            );
            // Save Hungarian execution time.
            jdbcTemplate.update(
                    "INSERT INTO Algorithm_Execute (Session_Id, Algorithm_Id, Execution_Time_MS, Output_Result) VALUES (?, ?, ?, 'SUCCESS')",
                    sessionId, hungarianAlgorithmId, hungarianMs
            );
        } catch (Exception ignored) {
            // If DB is not ready yet, game still works.
        }
    }

    // Gets algorithm id if already there.
    // Else inserts and returns the new id.
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

    // Input payload from frontend.
    public record MinCostStartRequest(Integer n, Integer minCost, Integer maxCost) {}

    // One assignment pair: task row, employee col, and selected cost.
    public record AssignmentPair(int row, int col, int cost) {}

    // Full response payload for frontend UI.
    public record MinCostStartResponse(
            int[][] matrix,
            int greedyTotalCost,
            int hungarianTotalCost,
            List<AssignmentPair> greedyAssignments,
            List<AssignmentPair> hungarianAssignments,
            List<String> greedyLogs,
            List<String> hungarianLogs,
            double greedyTimeMs,
            double hungarianTimeMs
    ) {}

    // Internal helper record for algorithm method returns.
    private record AssignmentResult(int totalCost, List<AssignmentPair> assignments, List<String> logs) {}
}
