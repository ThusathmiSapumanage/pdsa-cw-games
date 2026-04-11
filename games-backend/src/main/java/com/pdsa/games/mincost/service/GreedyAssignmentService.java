package com.pdsa.games.mincost.service;

import com.pdsa.games.mincost.model.Assignment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Greedy minimum-cost assignment: repeatedly pick the smallest available cell and block its row/column.
 */
@Service
public class GreedyAssignmentService {

    public AlgorithmSolveResult solve(int[][] matrix) {
        int n = matrix.length;
        boolean[] usedRows = new boolean[n];
        boolean[] usedCols = new boolean[n];
        List<Assignment> assignments = new ArrayList<>();
        List<String> logs = new ArrayList<>();
        int total = 0;

        for (int step = 0; step < n; step++) {
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

            usedRows[bestRow] = true;
            usedCols[bestCol] = true;
            assignments.add(new Assignment(bestRow, bestCol, bestValue));
            total += bestValue;
            logs.add("Step " + (step + 1) + ": picked T" + (bestRow + 1)
                    + " -> E" + (bestCol + 1) + " (cost " + bestValue + ")");
        }

        return new AlgorithmSolveResult(total, assignments, logs, List.of());
    }
}
