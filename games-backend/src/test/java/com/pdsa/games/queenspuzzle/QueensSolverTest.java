package com.pdsa.games.queenspuzzle;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueensSolverTest {

    private final QueensSolver solver = new QueensSolver();

    // ================= SEQUENTIAL =================

    @Test
    void solveSequential_shouldFindTwoSolutionsFor4x4() {
        List<int[]> solutions = solver.solveSequential(4, 4);
        assertEquals(2, solutions.size());
    }

    @Test
    void solveSequentialCount_shouldMatchSequentialListFor4x4() {
        int count = solver.solveSequentialCount(4, 4);
        assertEquals(solver.solveSequential(4, 4).size(), count);
    }

    @Test
    void solveSequentialCount_shouldFindTenSolutionsFor5x5() {
        assertEquals(10, solver.solveSequentialCount(5, 5));
    }

    @Test
    void solveSequentialCount_shouldFindFourSolutionsFor6x6() {
        assertEquals(4, solver.solveSequentialCount(6, 6));
    }

    @Test
    void solveSequentialCount_shouldFindFortySolutionsFor7x7() {
        assertEquals(40, solver.solveSequentialCount(7, 7));
    }

    @Test
    void solveSequentialCount_shouldHandlePartialPlacement() {
        int count = solver.solveSequentialCount(4, 2);
        assertTrue(count > 0, "placing 2 queens on 4x4 should yield at least one solution");
    }

    // ================= THREADED =================

    @Test
    void solveThreadedCount_shouldMatchSequentialFor4x4() {
        assertEquals(solver.solveSequentialCount(4, 4), solver.solveThreadedCount(4, 4));
    }

    @Test
    void solveThreadedCount_shouldMatchSequentialFor5x5() {
        assertEquals(solver.solveSequentialCount(5, 5), solver.solveThreadedCount(5, 5));
    }

    @Test
    void solveThreadedCount_shouldMatchSequentialFor6x6() {
        assertEquals(solver.solveSequentialCount(6, 6), solver.solveThreadedCount(6, 6));
    }

    @Test
    void solveThreadedCount_shouldMatchSequentialFor8x8() {
        assertEquals(solver.solveSequentialCount(8, 8), solver.solveThreadedCount(8, 8));
    }

    // ================= STRUCTURE =================

    @Test
    void solveSequential_shouldReturnNonNullList() {
        List<int[]> solutions = solver.solveSequential(4, 4);
        assertNotNull(solutions);
        for (int[] s : solutions) {
            assertEquals(4, s.length);
        }
    }
}
