package com.pdsa.games.queenspuzzle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class QueensSolver {

    List<int[]> solveSequential(int boardSize, int queenCount) {
        List<int[]> solutions = new ArrayList<>();
        int[] rowToCol = initRowToCol(boardSize);
        boolean[] columns = new boolean[boardSize];
        boolean[] diag1 = new boolean[2 * boardSize - 1];
        boolean[] diag2 = new boolean[2 * boardSize - 1];
        backtrack(0, 0, boardSize, queenCount, rowToCol, columns, diag1, diag2, solutions);
        return solutions;
    }

    int solveSequentialCount(int boardSize, int queenCount) {
        int[] rowToCol = initRowToCol(boardSize);
        boolean[] columns = new boolean[boardSize];
        boolean[] diag1 = new boolean[2 * boardSize - 1];
        boolean[] diag2 = new boolean[2 * boardSize - 1];
        return countBacktrack(0, 0, boardSize, queenCount, rowToCol, columns, diag1, diag2);
    }

    private int countBacktrack(
            int row,
            int placed,
            int boardSize,
            int queenCount,
            int[] rowToCol,
            boolean[] columns,
            boolean[] diag1,
            boolean[] diag2) {
        if (placed == queenCount) {
            return 1;
        }
        if (row == boardSize) {
            return 0;
        }
        if ((boardSize - row) < (queenCount - placed)) {
            return 0;
        }

        int total = 0;
        for (int col = 0; col < boardSize; col++) {
            int d1 = diag1Index(row, col, boardSize);
            int d2 = row + col;
            if (!columns[col] && !diag1[d1] && !diag2[d2]) {
                columns[col] = true;
                diag1[d1] = true;
                diag2[d2] = true;
                rowToCol[row] = col;

                total += countBacktrack(row + 1, placed + 1, boardSize, queenCount, rowToCol, columns, diag1, diag2);

                rowToCol[row] = -1;
                columns[col] = false;
                diag1[d1] = false;
                diag2[d2] = false;
            }
        }

        if ((boardSize - (row + 1)) >= (queenCount - placed)) {
            rowToCol[row] = -1;
            total += countBacktrack(row + 1, placed, boardSize, queenCount, rowToCol, columns, diag1, diag2);
        }

        return total;
    }

    int solveThreadedCount(int boardSize, int queenCount) {
        List<State> seeds = new ArrayList<>();
        int[] initial = initRowToCol(boardSize);
        boolean[] columns = new boolean[boardSize];
        boolean[] diag1 = new boolean[2 * boardSize - 1];
        boolean[] diag2 = new boolean[2 * boardSize - 1];

        buildSeeds(0, 0, boardSize, queenCount, 2, initial, columns, diag1, diag2, seeds);

        ExecutorService executor = Executors.newFixedThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors())
        );
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (State seed : seeds) {
                futures.add(executor.submit(new SolverTask(seed, boardSize, queenCount)));
            }
            int total = 0;
            for (Future<Integer> future : futures) {
                total += future.get();
            }
            return total;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Threaded solver interrupted", ex);
        } catch (ExecutionException ex) {
            throw new IllegalStateException("Threaded solver failed", ex.getCause());
        } finally {
            executor.shutdownNow();
        }
    }

    private void backtrack(
            int row,
            int placed,
            int boardSize,
            int queenCount,
            int[] rowToCol,
            boolean[] columns,
            boolean[] diag1,
            boolean[] diag2,
            List<int[]> solutions) {
        if (placed == queenCount) {
            solutions.add(copyBoard(rowToCol));
            return;
        }
        if (row == boardSize) {
            return;
        }
        if ((boardSize - row) < (queenCount - placed)) {
            return;
        }

        for (int col = 0; col < boardSize; col++) {
            int d1 = diag1Index(row, col, boardSize);
            int d2 = row + col;
            if (!columns[col] && !diag1[d1] && !diag2[d2]) {
                columns[col] = true;
                diag1[d1] = true;
                diag2[d2] = true;
                rowToCol[row] = col;

                backtrack(row + 1, placed + 1, boardSize, queenCount, rowToCol, columns, diag1, diag2, solutions);

                rowToCol[row] = -1;
                columns[col] = false;
                diag1[d1] = false;
                diag2[d2] = false;
            }
        }

        if ((boardSize - (row + 1)) >= (queenCount - placed)) {
            rowToCol[row] = -1;
            backtrack(row + 1, placed, boardSize, queenCount, rowToCol, columns, diag1, diag2, solutions);
        }
    }

    private void buildSeeds(
            int row,
            int placed,
            int boardSize,
            int queenCount,
            int depth,
            int[] rowToCol,
            boolean[] columns,
            boolean[] diag1,
            boolean[] diag2,
            List<State> seeds) {
        if (placed == queenCount || row == boardSize || depth == 0) {
            seeds.add(new State(row, placed, copyBoard(rowToCol), columns.clone(), diag1.clone(), diag2.clone()));
            return;
        }
        if ((boardSize - row) < (queenCount - placed)) {
            return;
        }

        for (int col = 0; col < boardSize; col++) {
            int d1 = diag1Index(row, col, boardSize);
            int d2 = row + col;
            if (!columns[col] && !diag1[d1] && !diag2[d2]) {
                columns[col] = true;
                diag1[d1] = true;
                diag2[d2] = true;
                rowToCol[row] = col;

                buildSeeds(row + 1, placed + 1, boardSize, queenCount, depth - 1, rowToCol, columns, diag1, diag2, seeds);

                rowToCol[row] = -1;
                columns[col] = false;
                diag1[d1] = false;
                diag2[d2] = false;
            }
        }

        if ((boardSize - (row + 1)) >= (queenCount - placed)) {
            rowToCol[row] = -1;
            buildSeeds(row + 1, placed, boardSize, queenCount, depth - 1, rowToCol, columns, diag1, diag2, seeds);
        }
    }

    private int[] initRowToCol(int boardSize) {
        int[] rowToCol = new int[boardSize];
        for (int i = 0; i < boardSize; i++) {
            rowToCol[i] = -1;
        }
        return rowToCol;
    }

    private int[] copyBoard(int[] rowToCol) {
        int[] copy = new int[rowToCol.length];
        System.arraycopy(rowToCol, 0, copy, 0, rowToCol.length);
        return copy;
    }

    private int diag1Index(int row, int col, int boardSize) {
        return row - col + boardSize - 1;
    }

    private static class State {
        private final int row;
        private final int placed;
        private final int[] rowToCol;
        private final boolean[] columns;
        private final boolean[] diag1;
        private final boolean[] diag2;

        private State(int row, int placed, int[] rowToCol, boolean[] columns, boolean[] diag1, boolean[] diag2) {
            this.row = row;
            this.placed = placed;
            this.rowToCol = rowToCol;
            this.columns = columns;
            this.diag1 = diag1;
            this.diag2 = diag2;
        }
    }

    private class SolverTask implements Callable<Integer> {
        private final State seed;
        private final int boardSize;
        private final int queenCount;

        private SolverTask(State seed, int boardSize, int queenCount) {
            this.seed = seed;
            this.boardSize = boardSize;
            this.queenCount = queenCount;
        }

        @Override
        public Integer call() {
            return countSolutions(seed.row, seed.placed, boardSize, queenCount,
                    seed.rowToCol, seed.columns, seed.diag1, seed.diag2);
        }

        private int countSolutions(
                int row,
                int placed,
                int boardSize,
                int queenCount,
                int[] rowToCol,
                boolean[] columns,
                boolean[] diag1,
                boolean[] diag2) {
            if (placed == queenCount) {
                return 1;
            }
            if (row == boardSize) {
                return 0;
            }
            if ((boardSize - row) < (queenCount - placed)) {
                return 0;
            }

            int total = 0;
            for (int col = 0; col < boardSize; col++) {
                int d1 = diag1Index(row, col, boardSize);
                int d2 = row + col;
                if (!columns[col] && !diag1[d1] && !diag2[d2]) {
                    columns[col] = true;
                    diag1[d1] = true;
                    diag2[d2] = true;
                    rowToCol[row] = col;

                    total += countSolutions(row + 1, placed + 1, boardSize, queenCount,
                            rowToCol, columns, diag1, diag2);

                    rowToCol[row] = -1;
                    columns[col] = false;
                    diag1[d1] = false;
                    diag2[d2] = false;
                }
            }

            if ((boardSize - (row + 1)) >= (queenCount - placed)) {
                rowToCol[row] = -1;
                total += countSolutions(row + 1, placed, boardSize, queenCount,
                        rowToCol, columns, diag1, diag2);
            }

            return total;
        }
    }
}
