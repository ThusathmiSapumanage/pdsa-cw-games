package com.pdsa.games.knightstour;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class KnightsTourServiceTest {

    private KnightsTourRepository repository;
    private KnightsTourService service;

    @BeforeEach
    void setUp() {
        repository = mock(KnightsTourRepository.class);
        service = new KnightsTourService(repository);
    }

    // ================= START ROUND =================

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
        assertEquals(64, response.getExpectedMoveCount());

        verify(repository).saveKnightTourGame(eq(100L), eq(8), anyInt(), anyInt(), eq(64));
        verify(repository).saveAlgorithmExecution(eq(100L), eq(1), anyDouble(), eq("SUCCESS"));
        verify(repository).saveKnightMoves(eq(100L), eq(1), anyList());
    }

    @Test
    void startRound_shouldThrow_whenBoardSizeInvalid() {
        KnightsTourModel.StartRequest request = new KnightsTourModel.StartRequest();
        request.setPlayerId(1L);
        request.setBoardSize(10);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.startRound(request)
        );

        assertEquals("Board size must be 8 or 16.", ex.getMessage());
    }

    @Test
    void startRound_shouldThrow_whenPlayerDoesNotExist() {
        when(repository.playerExists(1L)).thenReturn(false);

        KnightsTourModel.StartRequest request = new KnightsTourModel.StartRequest();
        request.setPlayerId(1L);
        request.setBoardSize(8);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.startRound(request)
        );

        assertEquals("Player does not exist.", ex.getMessage());
    }

    // ================= SUBMIT ANSWER =================

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

        assertTrue(response.getCorrect());
        assertEquals("WIN", response.getOutcome());

        verify(repository).saveResponseMoves(500L, correctMoves);
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

        assertFalse(response.getCorrect());
        assertEquals("LOSE", response.getOutcome());

        verify(repository).saveResponseMoves(501L, submittedMoves);
    }

    @Test
    void submitAnswer_shouldThrow_whenMovesEmpty() {
        KnightsTourModel.AnswerRequest request = new KnightsTourModel.AnswerRequest();
        request.setSessionId(100L);
        request.setPlayerId(1L);
        request.setMoves(List.of());

        assertThrows(IllegalArgumentException.class,
                () -> service.submitAnswer(request));
    }

    @Test
    void submitAnswer_shouldThrow_whenSessionInvalid() {
        when(repository.playerExists(1L)).thenReturn(true);
        when(repository.sessionBelongsToPlayer(100L, 1L)).thenReturn(false);

        KnightsTourModel.AnswerRequest request = new KnightsTourModel.AnswerRequest();
        request.setSessionId(100L);
        request.setPlayerId(1L);
        request.setMoves(List.of(new KnightsTourModel.Move(1, 1, 1)));

        assertThrows(IllegalArgumentException.class,
                () -> service.submitAnswer(request));
    }

    @Test
    void submitAnswer_shouldThrow_whenCorrectMovesMissing() {
        when(repository.playerExists(1L)).thenReturn(true);
        when(repository.sessionBelongsToPlayer(100L, 1L)).thenReturn(true);
        when(repository.findAlgorithmId("Warnsdorff")).thenReturn(1);
        when(repository.findCorrectMoves(100L, 1)).thenReturn(List.of());

        KnightsTourModel.AnswerRequest request = new KnightsTourModel.AnswerRequest();
        request.setSessionId(100L);
        request.setPlayerId(1L);
        request.setMoves(List.of(new KnightsTourModel.Move(1, 1, 1)));

        assertThrows(IllegalStateException.class,
                () -> service.submitAnswer(request));
    }

    // ================= CORRECT ANSWER =================

    @Test
    void getCorrectAnswer_shouldReturnMoves() {
        when(repository.playerExists(1L)).thenReturn(true);
        when(repository.sessionExists(100L)).thenReturn(true);
        when(repository.sessionBelongsToPlayer(100L, 1L)).thenReturn(true);
        when(repository.findAlgorithmId("Warnsdorff")).thenReturn(1);

        List<KnightsTourModel.Move> moves = Arrays.asList(
                new KnightsTourModel.Move(1, 1, 1),
                new KnightsTourModel.Move(2, 2, 3)
        );

        when(repository.findCorrectMoves(100L, 1)).thenReturn(moves);

        KnightsTourModel.CorrectAnswerResponse response =
                service.getCorrectAnswer(100L, 1L);

        assertEquals(2, response.getMoveCount());
        assertEquals(moves, response.getMoves());
    }

    @Test
    void getCorrectAnswer_shouldThrow_whenNotOwner() {
        when(repository.playerExists(1L)).thenReturn(true);
        when(repository.sessionExists(100L)).thenReturn(true);
        when(repository.sessionBelongsToPlayer(100L, 1L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> service.getCorrectAnswer(100L, 1L));
    }
}