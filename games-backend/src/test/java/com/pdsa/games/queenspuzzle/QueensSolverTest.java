package com.pdsa.games.queenspuzzle;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class QueensSolverTest {

    @Test
    void solveSequential_shouldFindTwoSolutionsFor4x4() {
        QueensSolver solver = new QueensSolver();
        List<int[]> solutions = solver.solveSequential(4, 4);
        assertEquals(2, solutions.size());
    }

    @Test
    void solveThreaded_shouldMatchSequentialFor4x4() {
        QueensSolver solver = new QueensSolver();
        int threadedCount = solver.solveThreadedCount(4, 4);
        assertEquals(2, threadedCount);
    }
}
