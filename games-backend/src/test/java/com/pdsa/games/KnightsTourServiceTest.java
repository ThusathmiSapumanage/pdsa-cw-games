package com.pdsa.games;

import com.pdsa.games.knightstour.KnightsTourModel;
import com.pdsa.games.knightstour.KnightsTourRepository;
import com.pdsa.games.knightstour.KnightsTourService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KnightsTourServiceTest {

    private KnightsTourRepository repository;
    private KnightsTourService service;

    @BeforeEach
    void setUp() {
        repository = mock(KnightsTourRepository.class);
        service = new KnightsTourService(repository);
    }

    @Test
    void startRound_shouldCreateRoundSuccessfully() {
        when(repository.playerExists(1L)).thenReturn(true);
        when(repository.findAlgorithmId("Warnsdorff")).thenReturn(1);
        when(repository.createGameSession(1L)).thenReturn(100L);

        KnightsTourModel.StartRequest request = new KnightsTourModel.StartRequest();
        request.setPlayerId(1L);
        request.setBoardSize(8);

        KnightsTourModel.StartResponse response = service.startRound(request);

        assertNotNull(response);
        assertEquals(100L, response.getSessionId());
        assertEquals(1L, response.getPlayerId());
        assertEquals(8, response.getBoardSize());
        assertEquals("READY", response.getStatus());

        verify(repository).saveKnightTourGame(eq(100L), eq(8), anyInt(), anyInt(), eq(64));
        verify(repository).saveAlgorithmExecution(eq(100L), eq(1), anyDouble(), eq("SUCCESS"));
        verify(repository).saveKnightMoves(eq(100L), eq(1), anyList());
    }

    @Test
    void submitAnswer_shouldReturnWin_whenMovesMatch() {
        when(repository.playerExists(1L)).thenReturn(true);
        when(repository.sessionBelongsToPlayer(100L, 1L)).thenReturn(true);
        when(repository.findAlgorithmId("Warnsdorff")).thenReturn(1);

        List<KnightsTourModel.Move> correctMoves = Arrays.asList(
                new KnightsTourModel.Move(1, 1, 1),
                new KnightsTourModel.Move(2, 2, 3)
        );

        when(repository.findCorrectMoves(100L, 1)).thenReturn(correctMoves);
        when(repository.saveResponse(100L, "CORRECT_2_MOVES", true)).thenReturn(500L);

        KnightsTourModel.AnswerRequest request = new KnightsTourModel.AnswerRequest();
        request.setSessionId(100L);
        request.setPlayerId(1L);
        request.setMoves(correctMoves);

        KnightsTourModel.AnswerResponse response = service.submitAnswer(request);

        assertNotNull(response);
        assertEquals(500L, response.getResponseId());
        assertTrue(response.getCorrect());
        assertEquals("WIN", response.getOutcome());
    }

    @Test
    void submitAnswer_shouldReturnLose_whenMovesDoNotMatch() {
        when(repository.playerExists(1L)).thenReturn(true);
        when(repository.sessionBelongsToPlayer(100L, 1L)).thenReturn(true);
        when(repository.findAlgorithmId("Warnsdorff")).thenReturn(1);

        List<KnightsTourModel.Move> correctMoves = Arrays.asList(
                new KnightsTourModel.Move(1, 1, 1),
                new KnightsTourModel.Move(2, 2, 3)
        );

        List<KnightsTourModel.Move> submittedMoves = Arrays.asList(
                new KnightsTourModel.Move(1, 1, 1),
                new KnightsTourModel.Move(2, 3, 2)
        );

        when(repository.findCorrectMoves(100L, 1)).thenReturn(correctMoves);
        when(repository.saveResponse(100L, "WRONG_2_MOVES", false)).thenReturn(501L);

        KnightsTourModel.AnswerRequest request = new KnightsTourModel.AnswerRequest();
        request.setSessionId(100L);
        request.setPlayerId(1L);
        request.setMoves(submittedMoves);

        KnightsTourModel.AnswerResponse response = service.submitAnswer(request);

        assertNotNull(response);
        assertEquals(501L, response.getResponseId());
        assertFalse(response.getCorrect());
        assertEquals("LOSE", response.getOutcome());
    }
}