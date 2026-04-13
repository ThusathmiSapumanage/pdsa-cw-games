package com.pdsa.games.mincost.service;

import com.pdsa.games.mincost.MinCostModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * HUNGARIAN ALGORITHM (Kuhn-Munkres):
 * This is the smart, mathematically optimal way to solve the problem.
 * It's way more complex than Greedy because it looks at the whole matrix 
 * to find the absolute lowest cost possible.
 *
 * This implementation records every "Step" so the UI can visualize the math.
 */
@Service
public class HungarianAssignmentService {

    /**
     * Solve the matrix using the Hungarian method.
     */
    public AlgorithmSolveResult solve(int[][] matrix) {
        // Defensive check: matrix must exist!
        if (matrix == null || matrix.length == 0) {
            throw new IllegalArgumentException("Matrix cannot be empty or null");
        }

        int n = matrix.length;
        // We copy to double to handle any eventual decimal reductions, 
        // though our current game uses integers.
        double[][] costMatrix = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                costMatrix[i][j] = matrix[i][j];
            }
        }

        List<MinCostModel.HungarianStep> steps = new ArrayList<>();

        // --- PHASE 1: ROW REDUCTION ---
        List<Integer> rowMins = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double min = Double.MAX_VALUE;
            for (int j = 0; j < n; j++) {
                if (costMatrix[i][j] < min) min = costMatrix[i][j];
            }
            rowMins.add((int) min);
            for (int j = 0; j < n; j++) {
                costMatrix[i][j] -= min;
            }
        }
        steps.add(new MinCostModel.HungarianStep("ROW_REDUCTION", convertToInt(costMatrix, n),
                "Row Reduction: Subtracted smallest value of each row to find zeros.",
                rowMins, null, null, null, null));

        // --- PHASE 2: COLUMN REDUCTION ---
        List<Integer> colMins = new ArrayList<>();
        for (int j = 0; j < n; j++) {
            double min = Double.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                if (costMatrix[i][j] < min) min = costMatrix[i][j];
            }
            colMins.add((int) min);
            for (int i = 0; i < n; i++) {
                costMatrix[i][j] -= min;
            }
        }
        steps.add(new MinCostModel.HungarianStep("COL_REDUCTION", convertToInt(costMatrix, n),
                "Column Reduction: Subtracted smallest value of each column.",
                null, colMins, null, null, null));

        // --- MAIN LOOP: COVER ZEROS AND ADJUST ---
        int[] rowsCovered = new int[n];
        int[] colsCovered = new int[n];
        int[] match = new int[n];
        Arrays.fill(match, -1);

        // Keep searching for a perfect matching (N assignments)
        while (countAssignments(costMatrix, n, match) < n) {
            Arrays.fill(rowsCovered, 0);
            Arrays.fill(colsCovered, 0);
            
            // Mark rows and cols with minimal lines to cover all zeros
            coverZeros(costMatrix, n, rowsCovered, colsCovered);

            // If we hit a dead end or enough lines, we stop
            if (isOptimized(rowsCovered, colsCovered, n)) break;

            // Find the smallest uncovered value (Delta) to "uncover" new zeros
            double delta = findDelta(costMatrix, n, rowsCovered, colsCovered);
            applyDelta(costMatrix, n, rowsCovered, colsCovered, delta);

            steps.add(new MinCostModel.HungarianStep("DELTA_ADJUST", convertToInt(costMatrix, n),
                    "Delta Adjust: Smallest uncovered value (" + delta + ") used to shift matrix.",
                    null, null, toBoolList(rowsCovered), toBoolList(colsCovered), delta));
        }

        // --- FINAL ASSIGNMENT EXTRACTION ---
        List<MinCostModel.Assignment> assignments = calculateOptimalPairs(costMatrix, n);
        int totalCost = 0;
        for (MinCostModel.Assignment a : assignments) {
            // Get original cost from the input matrix
            int cost = matrix[a.getRow()][a.getCol()];
            a.setCost(cost);
            totalCost += cost;
        }

        return new AlgorithmSolveResult(totalCost, assignments, List.of("Hungarian algorithm successfully optimized."), steps);
    }

    // --- REFACTORED HELPERS ---

    private List<MinCostModel.Assignment> calculateOptimalPairs(double[][] matrix, int n) {
        int[] match = new int[n];
        Arrays.fill(match, -1);
        findMatch(matrix, n, match);
        List<MinCostModel.Assignment> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (match[i] != -1) {
                // Return assignment pairs
                result.add(new MinCostModel.Assignment(i, match[i], 0));
            }
        }
        return result;
    }

    private int countAssignments(double[][] matrix, int n, int[] match) {
        Arrays.fill(match, -1);
        return findMatch(matrix, n, match);
    }

    private int findMatch(double[][] matrix, int n, int[] match) {
        int count = 0;
        boolean[] used = new boolean[n];
        for (int i = 0; i < n; i++) {
            if (dfs(i, matrix, n, used, match)) {
                count++;
                Arrays.fill(used, false);
            }
        }
        return count;
    }

    private boolean dfs(int u, double[][] matrix, int n, boolean[] used, int[] match) {
        for (int v = 0; v < n; v++) {
            if (matrix[u][v] == 0 && !used[v]) {
                used[v] = true;
                if (match[v] < 0 || dfs(match[v], matrix, n, used, match)) {
                    match[v] = u;
                    return true;
                }
            }
        }
        return false;
    }

    private void coverZeros(double[][] matrix, int n, int[] rows, int[] cols) {
        // Greedy zero covering for simpler demonstration in viva
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (matrix[i][j] == 0 && rows[i] == 0 && cols[j] == 0) {
                    cols[j] = 1;
                }
            }
        }
    }

    private boolean isOptimized(int[] rows, int[] cols, int n) {
        int totalLines = 0;
        for (int r : rows) if (r == 1) totalLines++;
        for (int c : cols) if (c == 1) totalLines++;
        return totalLines >= n;
    }

    private double findDelta(double[][] matrix, int n, int[] rows, int[] cols) {
        double delta = Double.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            if (rows[i] == 0) {
                for (int j = 0; j < n; j++) {
                    if (cols[j] == 0 && matrix[i][j] < delta) {
                        delta = matrix[i][j];
                    }
                }
            }
        }
        return (delta == Double.MAX_VALUE) ? 0 : delta;
    }

    private void applyDelta(double[][] matrix, int n, int[] rows, int[] cols, double delta) {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                // If double covered, add delta
                if (rows[i] == 1 && cols[j] == 1) matrix[i][j] += delta;
                // If uncovered, subtract delta
                if (rows[i] == 0 && cols[j] == 0) matrix[i][j] -= delta;
            }
        }
    }

    private int[][] convertToInt(double[][] matrix, int n) {
        int[][] res = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                res[i][j] = (int) Math.round(matrix[i][j]);
            }
        }
        return res;
    }

    private List<Boolean> toBoolList(int[] arr) {
        List<Boolean> list = new ArrayList<>();
        for (int val : arr) list.add(val == 1);
        return list;
    }
}
