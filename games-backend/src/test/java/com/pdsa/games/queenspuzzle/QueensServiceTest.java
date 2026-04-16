package com.pdsa.games.queenspuzzle;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    private QueensRepository repository;
    private QueensService service;

    @BeforeEach
    void setUp() {
        repository = mock(QueensRepository.class);
        service = new QueensService(repository);
    }

    // ================= START ROUND =================

    @Test
    void startRound_shouldCreateRoundSuccessfully() {
        when(repository.playerExists(1L)).thenReturn(true);
        when(repository.ensureGameId(anyString())).thenReturn(10);
        when(repository.ensureAlgorithmId(eq(10), anyString())).thenReturn(20, 21);
        when(repository.createGameSession(10, 1L)).thenReturn(100L);

        QueensModel.StartRequest request = new QueensModel.StartRequest(1L, 4, 4);
        QueensModel.StartResponse response = service.startRound(request);

        assertNotNull(response);
        assertEquals(100L, response.getSessionId());
        assertEquals(4, response.getBoardSize());
        assertEquals(4, response.getQueenCount());
        assertEquals(2, response.getTotalSolutions());

        verify(repository).saveSixteenQueensGame(100L, 2);
        verify(repository, times(2)).saveAlgorithmExecution(eq(100L), anyInt(), anyDouble(), eq("SUCCESS"));
    }

    @Test
    void startRound_shouldThrow_whenPlayerIdMissing() {
        QueensModel.StartRequest request = new QueensModel.StartRequest(null, 4, 4);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.startRound(request));
        assertEquals("Valid playerId is required.", ex.getMessage());
    }

    @Test
    void startRound_shouldThrow_whenPlayerDoesNotExist() {
        when(repository.playerExists(1L)).thenReturn(false);

        QueensModel.StartRequest request = new QueensModel.StartRequest(1L, 4, 4);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.startRound(request));
        assertEquals("Player does not exist.", ex.getMessage());
    }

    @Test
    void startRound_shouldThrow_whenBoardSizeBelowMinimum() {
        when(repository.playerExists(1L)).thenReturn(true);

        QueensModel.StartRequest request = new QueensModel.StartRequest(1L, 3, 3);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.startRound(request));
        assertTrue(ex.getMessage().contains("Board size"));
    }

    @Test
    void startRound_shouldThrow_whenQueenCountExceedsBoardSize() {
        when(repository.playerExists(1L)).thenReturn(true);

        QueensModel.StartRequest request = new QueensModel.StartRequest(1L, 4, 5);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.startRound(request));
        assertTrue(ex.getMessage().contains("Queen count"));
    }

    // ================= SUBMIT ANSWER =================

    @Test
    void submitAnswer_shouldRecordCorrectSolution() {
        when(repository.playerExists(1L)).thenReturn(true);
        when(repository.sessionBelongsToPlayer(100L, 1L)).thenReturn(true);
        when(repository.getTotalSolutions(100L)).thenReturn(14772512);
        when(repository.getDiscoveredCount(100L)).thenReturn(0, 1);
        when(repository.findMatchingSolutionId(eq(100L), any())).thenReturn(null);
        when(repository.insertDiscoveredSolution(eq(100L), any())).thenReturn(1);
        when(repository.findPlayerName(1L)).thenReturn("Rajitha");

        QueensModel.SubmitRequest request = new QueensModel.SubmitRequest(100L, 1L, validSolution());
        QueensModel.SubmitResponse response = service.submitAnswer(request);

        assertTrue(response.getCorrect());
        assertFalse(response.getAlreadyDiscovered());
        assertEquals(1, response.getSolutionsDiscovered());
        verify(repository).insertDiscoveredSolution(eq(100L), any());
        verify(repository).saveResponse(eq(100L), anyString(), eq(true));
    }

    @Test
    void submitAnswer_shouldFlagAlreadyDiscovered() {
        when(repository.playerExists(1L)).thenReturn(true);
        when(repository.sessionBelongsToPlayer(100L, 1L)).thenReturn(true);
        when(repository.getTotalSolutions(100L)).thenReturn(14772512);
        when(repository.getDiscoveredCount(100L)).thenReturn(1);
        when(repository.findMatchingSolutionId(eq(100L), any())).thenReturn(7);

        QueensModel.SubmitRequest request = new QueensModel.SubmitRequest(100L, 1L, validSolution());
        QueensModel.SubmitResponse response = service.submitAnswer(request);

        assertTrue(response.getCorrect());
        assertTrue(response.getAlreadyDiscovered());
        verify(repository, never()).insertDiscoveredSolution(anyLong(), any());
    }

    @Test
    void submitAnswer_shouldReturnIncorrect_whenStructureInvalid() {
        when(repository.playerExists(1L)).thenReturn(true);
        when(repository.sessionBelongsToPlayer(100L, 1L)).thenReturn(true);
        when(repository.getTotalSolutions(100L)).thenReturn(14772512);
        when(repository.getDiscoveredCount(100L)).thenReturn(0);
        when(repository.findPlayerName(1L)).thenReturn("Rajitha");

        List<QueensModel.Position> conflicting = new ArrayList<>(validSolution());
        conflicting.set(0, new QueensModel.Position(1, 2));

        QueensModel.SubmitRequest request = new QueensModel.SubmitRequest(100L, 1L, conflicting);
        QueensModel.SubmitResponse response = service.submitAnswer(request);

        assertFalse(response.getCorrect());
        assertFalse(response.getAlreadyDiscovered());
        assertTrue(response.getMessage().startsWith("Incorrect solution"));
        verify(repository).saveResponse(eq(100L), anyString(), eq(false));
        verify(repository, never()).insertDiscoveredSolution(anyLong(), any());
    }

    @Test
    void submitAnswer_shouldResetDiscovered_whenAllSolutionsFound() {
        when(repository.playerExists(1L)).thenReturn(true);
        when(repository.sessionBelongsToPlayer(100L, 1L)).thenReturn(true);
        when(repository.getTotalSolutions(100L)).thenReturn(1);
        when(repository.getDiscoveredCount(100L)).thenReturn(0, 1);
        when(repository.findMatchingSolutionId(eq(100L), any())).thenReturn(null);
        when(repository.insertDiscoveredSolution(eq(100L), any())).thenReturn(1);
        when(repository.findPlayerName(1L)).thenReturn("Rajitha");

        QueensModel.SubmitRequest request = new QueensModel.SubmitRequest(100L, 1L, validSolution());
        QueensModel.SubmitResponse response = service.submitAnswer(request);

        assertTrue(response.getCorrect());
        assertEquals(0, response.getSolutionsDiscovered());
        assertTrue(response.getMessage().contains("All solutions discovered"));
        verify(repository).resetDiscovered(100L);
    }

    @Test
    void submitAnswer_shouldThrow_whenSessionDoesNotBelongToPlayer() {
        when(repository.playerExists(1L)).thenReturn(true);
        when(repository.sessionBelongsToPlayer(100L, 1L)).thenReturn(false);

        QueensModel.SubmitRequest request = new QueensModel.SubmitRequest(100L, 1L, validSolution());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.submitAnswer(request));
        assertTrue(ex.getMessage().contains("session does not belong"));
    }

    @Test
    void submitAnswer_shouldThrow_whenGameConfigMissing() {
        when(repository.playerExists(1L)).thenReturn(true);
        when(repository.sessionBelongsToPlayer(100L, 1L)).thenReturn(true);
        when(repository.getTotalSolutions(100L)).thenReturn(null);

        QueensModel.SubmitRequest request = new QueensModel.SubmitRequest(100L, 1L, validSolution());
        assertThrows(IllegalStateException.class, () -> service.submitAnswer(request));
    }

    // ================= STATUS =================

    @Test
    void getStatus_shouldReturnProgress() {
        when(repository.playerExists(1L)).thenReturn(true);
        when(repository.sessionBelongsToPlayer(100L, 1L)).thenReturn(true);
        when(repository.getTotalSolutions(100L)).thenReturn(14772512);
        when(repository.getDiscoveredCount(100L)).thenReturn(42);

        QueensModel.StatusResponse response = service.getStatus(100L, 1L);

        assertEquals(100L, response.getSessionId());
        assertEquals(14772512, response.getTotalSolutions());
        assertEquals(42, response.getSolutionsDiscovered());
    }

    @Test
    void getStatus_shouldThrow_whenSessionIdInvalid() {
        assertThrows(IllegalArgumentException.class, () -> service.getStatus(0L, 1L));
    }

    // 16x16 valid solution (one of the 14M+) — placed by row, columns:
    // row 1->col 1, 2->3, 3->5, 4->2, 5->13, 6->9, 7->14, 8->12,
    // 9->15, 10->6, 11->16, 12->7, 13->4, 14->11, 15->8, 16->10
    private List<QueensModel.Position> validSolution() {
        int[] cols = {1, 3, 5, 2, 13, 9, 14, 12, 15, 6, 16, 7, 4, 11, 8, 10};
        List<QueensModel.Position> positions = new ArrayList<>();
        for (int row = 0; row < cols.length; row++) {
            positions.add(new QueensModel.Position(cols[row], row + 1));
        }
        return positions;
    }
}
