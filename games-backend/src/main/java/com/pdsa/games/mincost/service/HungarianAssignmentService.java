package com.pdsa.games.mincost.service;

import com.pdsa.games.mincost.model.Assignment;
import com.pdsa.games.mincost.model.HungarianStepDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Hungarian (Kuhn–Munkres style) minimum assignment on a square cost matrix.
 */
@Service
public class HungarianAssignmentService {

    public AlgorithmSolveResult solve(int[][] originalMatrix) {
        int n = originalMatrix.length;
        int[][] matrix = new int[n][n];
        for (int i = 0; i < n; i++) {
            matrix[i] = Arrays.copyOf(originalMatrix[i], n);
        }

        List<String> logs = new ArrayList<>();
        List<HungarianStepDto> steps = new ArrayList<>();

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

        int[] rowMins = new int[n];
        for (int i = 0; i < n; i++) {
            int rowMin = Integer.MAX_VALUE;
            for (int j = 0; j < n; j++) {
                if (matrix[i][j] < rowMin) {
                    rowMin = matrix[i][j];
                }
            }
            rowMins[i] = rowMin;
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

        int[] colMins = new int[n];
        for (int j = 0; j < n; j++) {
            int colMin = Integer.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                if (matrix[i][j] < colMin) {
                    colMin = matrix[i][j];
                }
            }
            colMins[j] = colMin;
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

        while (true) {
            Arrays.fill(matchRow, -1);
            Arrays.fill(matchCol, -1);
            int matchedCount = 0;

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

            boolean[] markedRows = new boolean[n];
            boolean[] markedCols = new boolean[n];

            for (int i = 0; i < n; i++) {
                if (matchRow[i] == -1) {
                    markedRows[i] = true;
                }
            }

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

            steps.add(new HungarianStepDto(
                    "COVERING",
                    copyMatrix(matrix),
                    "Covered zeros with minimum lines (delta=" + delta + ")",
                    null,
                    null,
                    coverRows,
                    markedCols,
                    delta));

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

        List<Assignment> assignments = new ArrayList<>();
        int total = 0;
        for (int i = 0; i < n; i++) {
            int j = matchRow[i];
            int cost = originalMatrix[i][j];
            total += cost;
            assignments.add(new Assignment(i, j, cost));
        }

        logs.add("Optimal assignments found successfully.");
        logs.add("Final Total Cost = " + total);

        return new AlgorithmSolveResult(total, assignments, logs, steps);
    }

    private int[][] copyMatrix(int[][] original) {
        int n = original.length;
        int[][] copy = new int[n][n];
        for (int i = 0; i < n; i++) {
            copy[i] = Arrays.copyOf(original[i], n);
        }
        return copy;
    }
}
