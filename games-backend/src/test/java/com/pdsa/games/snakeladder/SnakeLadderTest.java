package com.pdsa.games.snakeladder;

import com.pdsa.games.snakeladder.model.AlgorithmResult;
import com.pdsa.games.snakeladder.model.SnakeLadderItem;
import com.pdsa.games.snakeladder.model.SnakeLadderItem.PathType;
import com.pdsa.games.snakeladder.service.BFSSnakeLadderService;
import com.pdsa.games.snakeladder.service.DPSnakeLadderService;
import com.pdsa.games.snakeladder.service.SnakeLadderService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Unit tests for the Snake and Ladder game backend.
 *
 * Tests are grouped into four sections:
 * 1. BFS algorithm tests
 * 2. DP algorithm tests
 * 3. SnakeLadderService tests (board generation, choices, validation)
 * 4. Both algorithms agree on the same answer
 *
 * No database is needed — the algorithm services are tested independently.
 */
class SnakeLadderTest {

    private BFSSnakeLadderService bfsService;
    private DPSnakeLadderService dpService;
    private SnakeLadderService snakeLadderService;

    // A simple 6×6 board (36 cells) with no snakes or ladders
    private static final int TOTAL_CELLS_6x6 = 36;
    private static final Map<Integer, Integer> NO_PORTALS = new HashMap<>();

    @BeforeEach
    void setUp() {
        bfsService = new BFSSnakeLadderService();
        dpService = new DPSnakeLadderService();
        // Pass null for repository — we only test methods that don't need DB
        snakeLadderService = new SnakeLadderService(null, bfsService, dpService);
    }

    private List<SnakeLadderItem> dummyItems(int n) {
        return snakeLadderService.generateBoard(n, 1);
    }

    // =========================================================================
    // 1. BFS Algorithm Tests
    // =========================================================================

    @Test
    void bfs_noSnakesOrLadders_returnsValidAnswer() {
        // With no portals, minimum throws on a 36-cell board must be between 1 and 7
        AlgorithmResult result = bfsService.solve(TOTAL_CELLS_6x6, NO_PORTALS);
        assertTrue(result.isSuccess(), "BFS should find a path");
        assertTrue(result.getMinimumThrows() >= 1, "Must take at least 1 throw");
        assertTrue(result.getMinimumThrows() <= 7, "Should finish within 7 throws on a 36-cell board");
    }

    @Test
    void bfs_ladderNearStart_reducesThrows() {
        // A ladder from cell 2 to cell 32 should dramatically reduce minimum throws
        Map<Integer, Integer> portalMap = new HashMap<>();
        portalMap.put(2, 32); // big ladder near start

        AlgorithmResult withLadder = bfsService.solve(TOTAL_CELLS_6x6, portalMap);
        AlgorithmResult withoutLadder = bfsService.solve(TOTAL_CELLS_6x6, NO_PORTALS);

        assertTrue(withLadder.getMinimumThrows() <= withoutLadder.getMinimumThrows(),
                "A big ladder should not increase minimum throws");
    }

    @Test
    void bfs_snakeNearEnd_increasesOrSameThrows() {
        // A snake just before the end should force more throws
        Map<Integer, Integer> portalMap = new HashMap<>();
        portalMap.put(35, 5); // snake from cell 35 down to cell 5

        AlgorithmResult withSnake = bfsService.solve(TOTAL_CELLS_6x6, portalMap);
        AlgorithmResult withoutSnake = bfsService.solve(TOTAL_CELLS_6x6, NO_PORTALS);

        assertTrue(withSnake.getMinimumThrows() >= withoutSnake.getMinimumThrows(),
                "A snake near the end should not decrease minimum throws");
    }

    @Test
    void bfs_recordsExecutionTime() {
        AlgorithmResult result = bfsService.solve(TOTAL_CELLS_6x6, NO_PORTALS);
        assertTrue(result.getExecutionTimeMs() >= 0, "Execution time must be non-negative");
    }

    @Test
    void bfs_algorithmNameIsBFS() {
        AlgorithmResult result = bfsService.solve(TOTAL_CELLS_6x6, NO_PORTALS);
        assertEquals("BFS", result.getAlgorithmName());
    }

    // =========================================================================
    // 2. DP Algorithm Tests
    // =========================================================================

    @Test
    void dp_noSnakesOrLadders_returnsValidAnswer() {
        AlgorithmResult result = dpService.solve(TOTAL_CELLS_6x6, NO_PORTALS);
        assertTrue(result.isSuccess(), "DP should find a path");
        assertTrue(result.getMinimumThrows() >= 1);
        assertTrue(result.getMinimumThrows() <= 7);
    }

    @Test
    void dp_ladderNearStart_reducesThrows() {
        Map<Integer, Integer> portalMap = new HashMap<>();
        portalMap.put(2, 32);

        AlgorithmResult withLadder = dpService.solve(TOTAL_CELLS_6x6, portalMap);
        AlgorithmResult withoutLadder = dpService.solve(TOTAL_CELLS_6x6, NO_PORTALS);

        assertTrue(withLadder.getMinimumThrows() <= withoutLadder.getMinimumThrows());
    }

    @Test
    void dp_recordsExecutionTime() {
        AlgorithmResult result = dpService.solve(TOTAL_CELLS_6x6, NO_PORTALS);
        assertTrue(result.getExecutionTimeMs() >= 0);
    }

    @Test
    void dp_algorithmNameIsDynamicProgramming() {
        AlgorithmResult result = dpService.solve(TOTAL_CELLS_6x6, NO_PORTALS);
        assertEquals("Dynamic Programming", result.getAlgorithmName());
    }

    // =========================================================================
    // 3. SnakeLadderService Tests
    // =========================================================================

    @Test
    void validateN_validValues_noException() {
        // Should not throw for any value from 6 to 12
        for (int n = 6; n <= 12; n++) {
            final int testN = n;
            assertDoesNotThrow(() -> snakeLadderService.validateN(testN));
        }
    }

    @Test
    void validateN_tooSmall_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> snakeLadderService.validateN(5));
    }

    @Test
    void validateN_tooLarge_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> snakeLadderService.validateN(13));
    }

    @Test
    void generateBoard_correctNumberOfSnakesAndLadders() {
        int n = 8;
        List<SnakeLadderItem> items = snakeLadderService.generateBoard(n, 1);

        long snakeCount = items.stream().filter(i -> i.getPathType() == PathType.SNAKE).count();
        long ladderCount = items.stream().filter(i -> i.getPathType() == PathType.LADDER).count();

        assertEquals(n - 2, snakeCount, "Should have N-2 snakes");
        assertEquals(n - 2, ladderCount, "Should have N-2 ladders");
    }

    @Test
    void generateBoard_laddersGoUpward() {
        List<SnakeLadderItem> items = snakeLadderService.generateBoard(6, 1);
        for (SnakeLadderItem item : items) {
            if (item.getPathType() == PathType.LADDER) {
                assertTrue(item.getStartCell() < item.getEndCell(),
                        "Ladder start must be less than end: " + item);
            }
        }
    }

    @Test
    void generateBoard_snakesGoDownward() {
        List<SnakeLadderItem> items = snakeLadderService.generateBoard(6, 1);
        for (SnakeLadderItem item : items) {
            if (item.getPathType() == PathType.SNAKE) {
                assertTrue(item.getStartCell() > item.getEndCell(),
                        "Snake start must be greater than end: " + item);
            }
        }
    }

    @Test
    void generateBoard_noDuplicateStartCells() {
        List<SnakeLadderItem> items = snakeLadderService.generateBoard(10, 1);
        Set<Integer> starts = new HashSet<>();
        for (SnakeLadderItem item : items) {
            assertTrue(starts.add(item.getStartCell()),
                    "Duplicate start cell found: " + item.getStartCell());
        }
    }

    @Test
    void generateChoices_alwaysContainsCorrectAnswer() {
        for (int correct = 1; correct <= 20; correct++) {
            List<Integer> choices = snakeLadderService.generateChoices(correct, dummyItems(8), 8);
            assertTrue(choices.contains(correct),
                    "Choices must include the correct answer: " + correct);
        }
    }

    @Test
    void generateChoices_alwaysReturnsThreeOptions() {
        List<Integer> choices = snakeLadderService.generateChoices(7, dummyItems(8), 8);
        assertEquals(3, choices.size(), "There must always be exactly 3 choices");
    }

    @Test
    void generateChoices_allChoicesArePositive() {
        // Even when correct answer is 1, wrong answers must be > 0
        List<Integer> choices = snakeLadderService.generateChoices(1, dummyItems(8), 8);
        for (int choice : choices) {
            assertTrue(choice > 0, "All choices must be positive: " + choice);
        }
    }

    @Test
    void generateChoices_allChoicesAreDistinct() {
        List<Integer> choices = snakeLadderService.generateChoices(5, dummyItems(8), 8);
        assertEquals(3, new HashSet<>(choices).size());
    }

    @Test
    void generateChoices_alwaysIncludesDrawOption() {
        int correctAnswer = 5;
        List<Integer> choices = snakeLadderService.generateChoices(correctAnswer, dummyItems(8), 8);
        assertTrue(
                choices.contains(correctAnswer - 1) || choices.contains(correctAnswer + 1),
                "Choices must include a draw option off by one from the correct answer.");
    }

    @Test
    void determineOutcome_exactMatch_returnsWin() {
        assertEquals("WIN", snakeLadderService.determineOutcome(5, 5));
    }

    @Test
    void determineOutcome_offByOne_above_returnsDraw() {
        assertEquals("DRAW", snakeLadderService.determineOutcome(6, 5));
    }

    @Test
    void determineOutcome_offByOne_below_returnsDraw() {
        assertEquals("DRAW", snakeLadderService.determineOutcome(4, 5));
    }

    @Test
    void determineOutcome_offByTwo_returnsLose() {
        assertEquals("LOSE", snakeLadderService.determineOutcome(3, 5));
    }

    @Test
    void determineOutcome_offByMoreThanTwo_returnsLose() {
        assertEquals("LOSE", snakeLadderService.determineOutcome(9, 5));
    }

    // =========================================================================
    // 4. BFS and DP Must Agree
    // =========================================================================

    @Test
    void bfsAndDP_agreeOnSameBoard_noPortals() {
        AlgorithmResult bfs = bfsService.solve(TOTAL_CELLS_6x6, NO_PORTALS);
        AlgorithmResult dp = dpService.solve(TOTAL_CELLS_6x6, NO_PORTALS);
        assertEquals(bfs.getMinimumThrows(), dp.getMinimumThrows(),
                "BFS and DP must produce the same answer on the same board");
    }

    @Test
    void bfsAndDP_agreeOnSameBoard_withPortals() {
        Map<Integer, Integer> portalMap = new HashMap<>();
        portalMap.put(4, 14); // ladder
        portalMap.put(9, 3); // snake
        portalMap.put(20, 30); // ladder
        portalMap.put(33, 12); // snake

        AlgorithmResult bfs = bfsService.solve(TOTAL_CELLS_6x6, portalMap);
        AlgorithmResult dp = dpService.solve(TOTAL_CELLS_6x6, portalMap);

        assertEquals(bfs.getMinimumThrows(), dp.getMinimumThrows(),
                "BFS and DP must agree: BFS=" + bfs.getMinimumThrows() + " DP=" + dp.getMinimumThrows());
    }

    @Test
    void bfsAndDP_agreeOnLargerBoard_12x12() {
        int total = 144; // 12 × 12
        Map<Integer, Integer> portalMap = new HashMap<>();
        portalMap.put(5, 50);
        portalMap.put(22, 100);
        portalMap.put(110, 30);
        portalMap.put(130, 60);

        AlgorithmResult bfs = bfsService.solve(total, portalMap);
        AlgorithmResult dp = dpService.solve(total, portalMap);

        assertEquals(bfs.getMinimumThrows(), dp.getMinimumThrows(),
                "Algorithms must agree on a 12×12 board");
    }
}