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
    // validates input, runs both algos, saves to db, and returns data for frontend
    public MinCostStartResponse startGame(MinCostStartRequest request) {
        // If user sends N, use it. If not, auto generate N (between 50 and 100).
        int n = pickN(request.n());
        // default costs are $20 to $200 as per pdf
        int minCost = request.minCost() == null ? 20 : request.minCost();
        int maxCost = request.maxCost() == null ? 200 : request.maxCost();
        
        // Basic check to make sure values are correct
        validateInputs(n, minCost, maxCost);

        // create the random N x N matrix
        int[][] matrix = buildRandomMatrix(n, minCost, maxCost);

        // run greedy algo and track time taken
        long greedyStart = System.nanoTime();
        AssignmentResult greedyResult = runGreedy(matrix);
        long greedyEnd = System.nanoTime();

        // run hungarian algo and track time taken
        long hungarianStart = System.nanoTime();
        AssignmentResult hungarianResult = runHungarian(matrix);
        long hungarianEnd = System.nanoTime();

        // convert nano time to milliseconds (easier to read)
        double greedyMs = nanosToMs(greedyEnd - greedyStart);
        double hungarianMs = nanosToMs(hungarianEnd - hungarianStart);

        // save this round into the database
        saveRunSafely(n, hungarianResult.totalCost(), greedyMs, hungarianMs);

        // build response to show on the screen
        return new MinCostStartResponse(
                matrix,
                greedyResult.totalCost(),
                hungarianResult.totalCost(),
                greedyResult.assignments(),
                hungarianResult.assignments(),
                greedyResult.logs(),
                List.of(
                        "Hungarian done: best cost = " + hungarianResult.totalCost(),
                        "Greedy done: cost = " + greedyResult.totalCost()
                ),
                greedyMs,
                hungarianMs
        );
    }

    // if user didnt give N, we pick random between 50 and 100
    private int pickN(Integer requestedN) {
        if (requestedN != null) {
            return requestedN;
        }
        // random.nextInt(51) gives 0-50, so +50 gives 50-100
        return 50 + random.nextInt(51);
    }

    // simple check for correct inputs based on pdf rules
    private void validateInputs(int n, int minCost, int maxCost) {
        // N must be 50-100 usually, but we allow 5+ for smaller tests
        if (n < 5 || n > 100) {
            throw new IllegalArgumentException("N should be between 5 and 100");
        }
        // costs cant be negative or zero
        if (minCost < 1 || maxCost < 1) {
            throw new IllegalArgumentException("costs must be positive values");
        }
        // min must be smaller than max
        if (minCost > maxCost) {
            throw new IllegalArgumentException("min cost cannot be more than max cost");
        }
    }

    // fills a n x n matrix with random cost values
    private int[][] buildRandomMatrix(int n, int minCost, int maxCost) {
        int[][] matrix = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                // random value between minCost and maxCost
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

    // HUNGARIAN ALGORITHM (The "Matrix Reduction" way)
    // This finds the perfect assignments for the lowest total cost.
    // I used the step-by-step method taught in PDSA class.
    private AssignmentResult runHungarian(int[][] originalMatrix) {
        int n = originalMatrix.length;
        // make a copy of matrix so we dont mess up the original one
        int[][] matrix = new int[n][n];
        for (int i = 0; i < n; i++) {
            matrix[i] = Arrays.copyOf(originalMatrix[i], n);
        }

        // STEP 1: Row Reduction
        // subtract the smallest value in each row from all others in that row
        for (int i = 0; i < n; i++) {
            int rowMin = Integer.MAX_VALUE;
            for (int j = 0; j < n; j++) {
                if (matrix[i][j] < rowMin) rowMin = matrix[i][j];
            }
            for (int j = 0; j < n; j++) {
                matrix[i][j] -= rowMin;
            }
        }

        // STEP 2: Column Reduction
        // subtract the smallest value in each column from all others in that column
        for (int j = 0; j < n; j++) {
            int colMin = Integer.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                if (matrix[i][j] < colMin) colMin = matrix[i][j];
            }
            for (int i = 0; i < n; i++) {
                matrix[i][j] -= colMin;
            }
        }

        // STEP 3 & 4: Covering zeros and adjusting matrix
        // we keep doing this until we can assign N zeros
        int[] matchRow = new int[n]; // stores which col is matched to row
        int[] matchCol = new int[n]; // stores which row is matched to col
        Arrays.fill(matchRow, -1);
        Arrays.fill(matchCol, -1);

        while (true) {
            // reset matches for current matrix state
            Arrays.fill(matchRow, -1);
            Arrays.fill(matchCol, -1);
            int matchedCount = 0;

            // Simple greedy matching to find baseline assignments on zeros
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (matrix[i][j] == 0 && matchCol[j] == -1) {
                        matchRow[i] = j;
                        matchCol[j] = i;
                        matchedCount++;
                        break;
                    }
                }
            }

            // if we matched all rows, we are done!
            if (matchedCount == n) break;

            // if not enough matches, we find min lines to cover all zeros
            // based on the "Marking" logic from class
            boolean[] markedRows = new boolean[n];
            boolean[] markedCols = new boolean[n];

            // 1. Mark rows that have no assignment
            for (int i = 0; i < n; i++) {
                if (matchRow[i] == -1) markedRows[i] = true;
            }

            boolean changed = true;
            while (changed) {
                changed = false;
                // 2. Mark cols that have zero in a marked row
                for (int i = 0; i < n; i++) {
                    if (markedRows[i]) {
                        for (int j = 0; j < n; j++) {
                            if (matrix[i][j] == 0 && !markedCols[j]) {
                                markedCols[j] = true;
                                changed = true;
                            }
                        }
                    }
                }
                // 3. Mark rows that have assignment in a marked column
                for (int j = 0; j < n; j++) {
                    if (markedCols[j]) {
                        for (int i = 0; i < n; i++) {
                            if (matchRow[i] == j && !markedRows[i]) {
                                markedRows[i] = true;
                                changed = true;
                            }
                        }
                    }
                }
            }

            // Smallest uncovered value (delta)
            int delta = Integer.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                if (markedRows[i]) {
                    for (int j = 0; j < n; j++) {
                        if (!markedCols[j] && matrix[i][j] < delta) {
                            delta = matrix[i][j];
                        }
                    }
                }
            }

            // Adjust the matrix values
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (markedRows[i]) matrix[i][j] -= delta;
                    if (markedCols[j]) matrix[i][j] += delta;
                }
            }
        }

        // Final result: collect assignments and total cost
        List<AssignmentPair> pairs = new ArrayList<>();
        int total = 0;
        for (int i = 0; i < n; i++) {
            int j = matchRow[i];
            int cost = originalMatrix[i][j];
            total += cost;
            pairs.add(new AssignmentPair(i, j, cost));
        }

        return new AssignmentResult(total, pairs, List.of("Optimal Hungarian Result Calculated"));
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
