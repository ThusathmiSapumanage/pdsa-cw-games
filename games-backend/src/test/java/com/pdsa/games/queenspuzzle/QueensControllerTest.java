package com.pdsa.games.queenspuzzle;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QueensControllerTest {

    private static final String EMAIL = "rajitha@test.com";

    private QueensService service;
    private QueensController controller;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        service = mock(QueensService.class);
        controller = new QueensController(service);
        authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(EMAIL);
    }

    @Test
    void startRound_shouldDelegateToServiceAndReturn200() {
        QueensModel.StartRequest request = new QueensModel.StartRequest(16, 16);
        QueensModel.StartResponse expected = new QueensModel.StartResponse(
                100L, 16, 16, 14772512, 112000.0, 18800.0, "Sixteen Queens solutions generated and stored.");

        when(service.startRound(eq(request), eq(EMAIL))).thenReturn(expected);

        ResponseEntity<QueensModel.StartResponse> response = controller.startRound(request, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(100L, response.getBody().getSessionId());
        assertEquals(14772512, response.getBody().getTotalSolutions());
        verify(service).startRound(request, EMAIL);
    }

    @Test
    void submitAnswer_shouldDelegateToServiceAndReturn200() {
        QueensModel.SubmitRequest request = new QueensModel.SubmitRequest(
                100L, List.of(new QueensModel.Position(1, 1)));
        QueensModel.SubmitResponse expected = new QueensModel.SubmitResponse(
                100L, 1L, true, false, 1, 14772512, "Correct solution recorded.");

        when(service.submitAnswer(any(), eq(EMAIL))).thenReturn(expected);

        ResponseEntity<QueensModel.SubmitResponse> response = controller.submitAnswer(request, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(true, response.getBody().getCorrect());
        verify(service).submitAnswer(request, EMAIL);
    }

    @Test
    void getStatus_shouldDelegateToServiceAndReturn200() {
        QueensModel.StatusResponse expected = new QueensModel.StatusResponse(
                100L, 14772512, 42, "Sixteen Queens progress loaded.");

        when(service.getStatus(eq(100L), eq(EMAIL))).thenReturn(expected);

        ResponseEntity<QueensModel.StatusResponse> response = controller.getStatus(100L, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(42, response.getBody().getSolutionsDiscovered());
        verify(service).getStatus(100L, EMAIL);
    }

    @Test
    void startRound_shouldThrow401_whenNotAuthenticated() {
        Authentication unauth = mock(Authentication.class);
        when(unauth.isAuthenticated()).thenReturn(false);

        QueensModel.StartRequest request = new QueensModel.StartRequest(16, 16);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.startRound(request, unauth));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }
}
