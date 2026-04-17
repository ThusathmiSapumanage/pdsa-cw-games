package com.pdsa.games.queenspuzzle;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QueensControllerTest {

    private QueensService service;
    private QueensController controller;

    @BeforeEach
    void setUp() {
        service = mock(QueensService.class);
        controller = new QueensController(service);
    }

    @Test
    void startRound_shouldDelegateToServiceAndReturn200() {
        QueensModel.StartRequest request = new QueensModel.StartRequest(1L, 16, 16);
        QueensModel.StartResponse expected = new QueensModel.StartResponse(
                100L, 16, 16, 14772512, 112000.0, 18800.0, "Sixteen Queens solutions generated and stored.");

        when(service.startRound(request)).thenReturn(expected);

        ResponseEntity<QueensModel.StartResponse> response = controller.startRound(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(100L, response.getBody().getSessionId());
        assertEquals(14772512, response.getBody().getTotalSolutions());
        verify(service).startRound(request);
    }

    @Test
    void submitAnswer_shouldDelegateToServiceAndReturn200() {
        QueensModel.SubmitRequest request = new QueensModel.SubmitRequest(
                100L, 1L, List.of(new QueensModel.Position(1, 1)));
        QueensModel.SubmitResponse expected = new QueensModel.SubmitResponse(
                100L, 1L, true, false, 1, 14772512, "Correct solution recorded.");

        when(service.submitAnswer(any())).thenReturn(expected);

        ResponseEntity<QueensModel.SubmitResponse> response = controller.submitAnswer(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(true, response.getBody().getCorrect());
        verify(service).submitAnswer(request);
    }

    @Test
    void getStatus_shouldDelegateToServiceAndReturn200() {
        QueensModel.StatusResponse expected = new QueensModel.StatusResponse(
                100L, 14772512, 42, "Sixteen Queens progress loaded.");

        when(service.getStatus(100L, 1L)).thenReturn(expected);

        ResponseEntity<QueensModel.StatusResponse> response = controller.getStatus(100L, 1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(42, response.getBody().getSolutionsDiscovered());
        verify(service).getStatus(100L, 1L);
    }
}
