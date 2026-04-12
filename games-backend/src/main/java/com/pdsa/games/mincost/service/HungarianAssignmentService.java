package com.pdsa.games.mincost.service;

import com.pdsa.games.mincost.model.Assignment;
import com.pdsa.games.mincost.model.HungarianStepDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * HUNGARIAN ALGORITHM:
 * This is the smart way to get the absolute lowest total cost.
 * It uses matrix reduction (like we learned in PDSA) to find the best pairs.
 * It's way better than the Greedy way!
 */
@Service
public class HungarianAssignmentService {

    public AlgorithmSolveResult solve(int[][] originalMatrix) {
        // Safety check first!
        if (originalMatrix == null || originalMatrix.length == 0) {
            throw new IllegalArgumentException("Matrix can't be null or empty");
        }

        int n = originalMatrix.length;
        // We make a copy so we don't mess up the original matrix data
        int[][] matrix = new int[n][n];
        for (int i = 0; i < n; i++) {
            matrix[i] = Arrays.copyOf(originalMatrix[i], n);
        }

        List<String> logs = new ArrayList<>();
        List<HungarianStepDto> steps = new ArrayList<>();

        // 1. Initial State
        steps.add(new HungarianStepDto(
                "INITIAL",
                copyMatrix(originalMatrix),
                "Starting Matrix",
                null,
                null,
                null,
                null,
                null));
        logs.add("Start Hungarian: Goal is to find the perfect assignment for the lowest total.");

        // 2. ROW REDUCTION: For each row, subtract the smallest number from all numbers in that row.
        int[] rowMins = new int[n];
        for (int i = 0; i < n; i++) {
            int rowMin = Integer.MAX_VALUE;
            // Find min in this row
            for (int j = 0; j < n; j++) {
                if (matrix[i][j] < rowMin) {
                    rowMin = matrix[i][j];
                }
            }
            rowMins[i] = rowMin;
            // Subtract it
            for (int j = 0; j < n; j++) {
                matrix[i][j] -= rowMin;
            }
        }
        steps.add(new HungarianStepDto(
                "ROW_REDUCTION",
                copyMatrix(matrix),
                "Subtracted row minimums",
                rowMins,
                null,
                null,
                null,
                null));
        if (n <= 10) {
            logs.add("Completed Row Reduction phase.");
        }

        // 3. COL REDUCTION: For each column, subtract the smallest number from all numbers in that column.
        int[] colMins = new int[n];
        for (int j = 0; j < n; j++) {
            int colMin = Integer.MAX_VALUE;
            // Find min in this column
            for (int i = 0; i < n; i++) {
                if (matrix[i][j] < colMin) {
                    colMin = matrix[i][j];
                }
            }
            colMins[j] = colMin;
            // Subtract it
            for (int i = 0; i < n; i++) {
                matrix[i][j] -= colMin;
            }
        }
        steps.add(new HungarianStepDto(
                "COL_REDUCTION",
                copyMatrix(matrix),
                "Subtracted column minimums",
                null,
                colMins,
                null,
                null,
                null));
        if (n > 10) {
            logs.add("Completed Column Reduction phase.");
        }

        logs.add("Starting assignment search on reduced zeros...");

        int[] matchRow = new int[n];
        int[] matchCol = new int[n];
        Arrays.fill(matchRow, -1);
        Arrays.fill(matchCol, -1);

        // 4. MAIN LOOP: Keep finding zeros and adjusting until we have N assignments.
        while (true) {
            Arrays.fill(matchRow, -1);
            Arrays.fill(matchCol, -1);
            int matchedCount = 0;

            // Try to pick one zero for each row/column
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

            // If we have enough zeros (MatchedCount == N), we are done!
            if (matchedCount == n) {
                steps.add(new HungarianStepDto(
                        "FINAL_MATCH",
                        copyMatrix(matrix),
                        "Optimal zero-assignments found",
                        null,
                        null,
                        null,
                        null,
                        null));
                break;
            }

            // 5. IF NOT DONE: We need to cover all zeros with minimum lines (Marking logic)
            boolean[] markedRows = new boolean[n];
            boolean[] markedCols = new boolean[n];

            // Mark rows that don't have an assignment
            for (int i = 0; i < n; i++) {
                if (matchRow[i] == -1) {
                    markedRows[i] = true;
                }
            }

            // Spread the marks to find the paths
            boolean changed = true;
            while (changed) {
                changed = false;
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

            boolean[] coverRows = new boolean[n];
            for (int i = 0; i < n; i++) {
                coverRows[i] = !markedRows[i];
            }

            // 6. DELTA ADJUSTMENT: Find the smallest uncovered number and update the matrix.
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

            // If we can't find a delta, something is wrong with the zero logic
            if (delta == Integer.MAX_VALUE) break;

            steps.add(new HungarianStepDto(
                    "COVERING",
                    copyMatrix(matrix),
                    "Covered zeros with minimum lines (delta=" + delta + ")",
                    null,
                    null,
                    coverRows,
                    markedCols,
                    delta));

            // Subtract delta from uncovered rows, Add it to covered columns
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (markedRows[i]) {
                        matrix[i][j] -= delta;
                    }
                    if (markedCols[j]) {
                        matrix[i][j] += delta;
                    }
                }
            }
            steps.add(new HungarianStepDto(
                    "ADJUSTMENT",
                    copyMatrix(matrix),
                    "Adjusted matrix by delta",
                    null,
                    null,
                    null,
                    null,
                    delta));
        }

        // 7. FINAL CALCULATION: Sum up the costs based on the final matches
        List<Assignment> assignments = new ArrayList<>();
        int total = 0;
        for (int i = 0; i < n; i++) {
            int j = matchRow[i];
            if (j != -1) {
               int cost = originalMatrix[i][j];
               total += cost;
               assignments.add(new Assignment(i, j, cost));
            }
        }

        logs.add("Optimal assignments found successfully.");
        logs.add("Final Total Cost = " + total);

        return new AlgorithmSolveResult(total, assignments, logs, steps);
    }

    // Helper to duplicate the grid
    private int[][] copyMatrix(int[][] original) {
        int n = original.length;
        int[][] copy = new int[n][n];
        for (int i = 0; i < n; i++) {
            copy[i] = Arrays.copyOf(original[i], n);
        }
        return copy;
    }
}

