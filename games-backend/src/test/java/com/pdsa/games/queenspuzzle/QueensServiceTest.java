package com.pdsa.games.queenspuzzle;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QueensServiceTest {

    private static final String EMAIL = "rajitha@test.com";

    private QueensRepository repository;
    private QueensService service;

    @BeforeEach
    void setUp() {
        repository = mock(QueensRepository.class);
        service = new QueensService(repository);
    }

    private void stubAuthenticatedPlayer() {
        when(repository.findPlayerIdByEmail(EMAIL)).thenReturn(1L);
        when(repository.playerExists(1L)).thenReturn(true);
    }

    // ================= START ROUND =================

    @Test
    void startRound_shouldCreateRoundSuccessfully() {
        stubAuthenticatedPlayer();
        when(repository.ensureGameId(anyString())).thenReturn(10);
        when(repository.ensureAlgorithmId(eq(10), anyString())).thenReturn(20, 21);
        when(repository.createGameSession(10, 1L)).thenReturn(100L);

        QueensModel.StartRequest request = new QueensModel.StartRequest(4, 4);
        QueensModel.StartResponse response = service.startRound(request, EMAIL);

        assertNotNull(response);
        assertEquals(100L, response.getSessionId());
        assertEquals(4, response.getBoardSize());
        assertEquals(4, response.getQueenCount());
        assertEquals(2, response.getTotalSolutions());

        verify(repository).saveSixteenQueensGame(100L, 2);
        verify(repository, times(2)).saveAlgorithmExecution(eq(100L), anyInt(), anyDouble(), eq("SUCCESS"));
    }

    @Test
    void startRound_shouldThrow_whenEmailIsBlank() {
        QueensModel.StartRequest request = new QueensModel.StartRequest(4, 4);
        assertThrows(ResponseStatusException.class,
                () -> service.startRound(request, "   "));
    }

    @Test
    void startRound_shouldThrow_whenPlayerDoesNotExist() {
        when(repository.findPlayerIdByEmail(EMAIL)).thenReturn(null);

        QueensModel.StartRequest request = new QueensModel.StartRequest(4, 4);
        assertThrows(ResponseStatusException.class,
                () -> service.startRound(request, EMAIL));
    }

    @Test
    void startRound_shouldThrow_whenBoardSizeBelowMinimum() {
        stubAuthenticatedPlayer();

        QueensModel.StartRequest request = new QueensModel.StartRequest(3, 3);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.startRound(request, EMAIL));
        assertTrue(ex.getMessage().contains("Board size"));
    }

    @Test
    void startRound_shouldThrow_whenQueenCountExceedsBoardSize() {
        stubAuthenticatedPlayer();

        QueensModel.StartRequest request = new QueensModel.StartRequest(4, 5);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.startRound(request, EMAIL));
        assertTrue(ex.getMessage().contains("Queen count"));
    }

    // ================= SUBMIT ANSWER =================

    @Test
    void submitAnswer_shouldRecordCorrectSolution() {
        stubAuthenticatedPlayer();
        when(repository.sessionBelongsToPlayer(100L, 1L)).thenReturn(true);
        when(repository.getTotalSolutions(100L)).thenReturn(14772512);
        when(repository.getDiscoveredCount(100L)).thenReturn(0, 1);
        when(repository.findMatchingSolutionId(eq(100L), any())).thenReturn(null);
        when(repository.insertDiscoveredSolution(eq(100L), any())).thenReturn(1);
        when(repository.findPlayerName(1L)).thenReturn("Rajitha");

        QueensModel.SubmitRequest request = new QueensModel.SubmitRequest(100L, validSolution());
        QueensModel.SubmitResponse response = service.submitAnswer(request, EMAIL);

        assertTrue(response.getCorrect());
        assertFalse(response.getAlreadyDiscovered());
        assertEquals(1, response.getSolutionsDiscovered());
        verify(repository).insertDiscoveredSolution(eq(100L), any());
        verify(repository).saveResponse(eq(100L), anyString(), eq(true));
    }

    @Test
    void submitAnswer_shouldFlagAlreadyDiscovered() {
        stubAuthenticatedPlayer();
        when(repository.sessionBelongsToPlayer(100L, 1L)).thenReturn(true);
        when(repository.getTotalSolutions(100L)).thenReturn(14772512);
        when(repository.getDiscoveredCount(100L)).thenReturn(1);
        when(repository.findMatchingSolutionId(eq(100L), any())).thenReturn(7);

        QueensModel.SubmitRequest request = new QueensModel.SubmitRequest(100L, validSolution());
        QueensModel.SubmitResponse response = service.submitAnswer(request, EMAIL);

        assertTrue(response.getCorrect());
        assertTrue(response.getAlreadyDiscovered());
        verify(repository, never()).insertDiscoveredSolution(anyLong(), any());
    }

    @Test
    void submitAnswer_shouldReturnIncorrect_whenStructureInvalid() {
        stubAuthenticatedPlayer();
        when(repository.sessionBelongsToPlayer(100L, 1L)).thenReturn(true);
        when(repository.getTotalSolutions(100L)).thenReturn(14772512);
        when(repository.getDiscoveredCount(100L)).thenReturn(0);
        when(repository.findPlayerName(1L)).thenReturn("Rajitha");

        List<QueensModel.Position> conflicting = new ArrayList<>(validSolution());
        conflicting.set(0, new QueensModel.Position(1, 2));

        QueensModel.SubmitRequest request = new QueensModel.SubmitRequest(100L, conflicting);
        QueensModel.SubmitResponse response = service.submitAnswer(request, EMAIL);

        assertFalse(response.getCorrect());
        assertFalse(response.getAlreadyDiscovered());
        assertTrue(response.getMessage().startsWith("Incorrect solution"));
        verify(repository).saveResponse(eq(100L), anyString(), eq(false));
        verify(repository, never()).insertDiscoveredSolution(anyLong(), any());
    }

    @Test
    void submitAnswer_shouldResetDiscovered_whenAllSolutionsFound() {
        stubAuthenticatedPlayer();
        when(repository.sessionBelongsToPlayer(100L, 1L)).thenReturn(true);
        when(repository.getTotalSolutions(100L)).thenReturn(1);
        when(repository.getDiscoveredCount(100L)).thenReturn(0, 1);
        when(repository.findMatchingSolutionId(eq(100L), any())).thenReturn(null);
        when(repository.insertDiscoveredSolution(eq(100L), any())).thenReturn(1);
        when(repository.findPlayerName(1L)).thenReturn("Rajitha");

        QueensModel.SubmitRequest request = new QueensModel.SubmitRequest(100L, validSolution());
        QueensModel.SubmitResponse response = service.submitAnswer(request, EMAIL);

        assertTrue(response.getCorrect());
        assertEquals(0, response.getSolutionsDiscovered());
        assertTrue(response.getMessage().contains("All solutions discovered"));
        verify(repository).resetDiscovered(100L);
    }

    @Test
    void submitAnswer_shouldThrow_whenSessionDoesNotBelongToPlayer() {
        stubAuthenticatedPlayer();
        when(repository.sessionBelongsToPlayer(100L, 1L)).thenReturn(false);

        QueensModel.SubmitRequest request = new QueensModel.SubmitRequest(100L, validSolution());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.submitAnswer(request, EMAIL));
        assertTrue(ex.getMessage().contains("session does not belong"));
    }

    @Test
    void submitAnswer_shouldThrow_whenGameConfigMissing() {
        stubAuthenticatedPlayer();
        when(repository.sessionBelongsToPlayer(100L, 1L)).thenReturn(true);
        when(repository.getTotalSolutions(100L)).thenReturn(null);

        QueensModel.SubmitRequest request = new QueensModel.SubmitRequest(100L, validSolution());
        assertThrows(IllegalStateException.class, () -> service.submitAnswer(request, EMAIL));
    }

    // ================= STATUS =================

    @Test
    void getStatus_shouldReturnProgress() {
        stubAuthenticatedPlayer();
        when(repository.sessionBelongsToPlayer(100L, 1L)).thenReturn(true);
        when(repository.getTotalSolutions(100L)).thenReturn(14772512);
        when(repository.getDiscoveredCount(100L)).thenReturn(42);

        QueensModel.StatusResponse response = service.getStatus(100L, EMAIL);

        assertEquals(100L, response.getSessionId());
        assertEquals(14772512, response.getTotalSolutions());
        assertEquals(42, response.getSolutionsDiscovered());
    }

    @Test
    void getStatus_shouldThrow_whenSessionIdInvalid() {
        assertThrows(IllegalArgumentException.class, () -> service.getStatus(0L, EMAIL));
    }

    // 8 queens on 16x16, no-threat placement (spec default).
    // Row 1..8, cols 1,3,5,7,9,11,13,15 — all rows/cols/diagonals unique.
    private List<QueensModel.Position> validSolution() {
        int[] cols = {1, 3, 5, 7, 9, 11, 13, 15};
        List<QueensModel.Position> positions = new ArrayList<>();
        for (int row = 0; row < cols.length; row++) {
            positions.add(new QueensModel.Position(cols[row], row + 1));
        }
        return positions;
    }
}
