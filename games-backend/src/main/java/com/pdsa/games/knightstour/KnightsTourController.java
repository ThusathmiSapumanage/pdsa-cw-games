package com.pdsa.games.knightstour;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/knights-tour")
@CrossOrigin
@Tag(name = "Knights Tour", description = "Start a round, submit answers, request hints, and fetch the correct answer")
public class KnightsTourController {

    private final KnightsTourService service;

    public KnightsTourController(KnightsTourService service) {
        this.service = service;
    }

    @PostMapping("/start")
        @Operation(
            summary = "Start a Knights Tour round",
            description = "Creates a new Knights Tour session for the given player and board size.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                description = "Start request payload",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = KnightsTourModel.StartRequest.class),
                    examples = @ExampleObject(
                        name = "Start request",
                        value = "{\n  \"playerId\": 1,\n  \"boardSize\": 8\n}"
                    )
                )
            )
        )
        @ApiResponses({
            @ApiResponse(
                responseCode = "200",
                description = "Round started successfully",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = KnightsTourModel.StartResponse.class),
                    examples = @ExampleObject(
                        name = "Start response",
                        value = "{\n  \"sessionId\": 1001,\n  \"playerId\": 1,\n  \"boardSize\": 8,\n  \"startX\": 1,\n  \"startY\": 1,\n  \"expectedMoveCount\": 64,\n  \"algorithmName\": \"Warnsdorff\",\n  \"algorithmExecutionTimeMs\": 12.5,\n  \"status\": \"SUCCESS\",\n  \"message\": \"Round started successfully\"\n}"
                    )
                )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid start request"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
        })
    public ResponseEntity<KnightsTourModel.StartResponse> startRound(
            @RequestBody KnightsTourModel.StartRequest request
    ) {
        return ResponseEntity.ok(service.startRound(request));
    }

    @PostMapping("/submit")
        @Operation(
            summary = "Submit a Knights Tour answer",
            description = "Validates a submitted path for an active Knights Tour session.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                description = "Answer submission payload",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = KnightsTourModel.AnswerRequest.class)
                )
            )
        )
        @ApiResponses({
            @ApiResponse(
                responseCode = "200",
                description = "Answer processed successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = KnightsTourModel.AnswerResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid answer submission"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
        })
    public ResponseEntity<KnightsTourModel.AnswerResponse> submitAnswer(
            @RequestBody KnightsTourModel.AnswerRequest request
    ) {
        return ResponseEntity.ok(service.submitAnswer(request));
    }

    @PostMapping("/hint")
        @Operation(
            summary = "Get a Knights Tour hint",
            description = "Returns the next valid move options from the submitted path.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                description = "Hint request payload",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = KnightsTourModel.HintRequest.class)
                )
            )
        )
        @ApiResponses({
            @ApiResponse(
                responseCode = "200",
                description = "Hint generated successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = KnightsTourModel.HintResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid hint request"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
        })
    public ResponseEntity<KnightsTourModel.HintResponse> getHint(
            @RequestBody KnightsTourModel.HintRequest request
    ) {
        return ResponseEntity.ok(service.getHint(request));
    }

    @GetMapping("/{sessionId}/correct-answer")
        @Operation(
            summary = "Get the correct answer",
            description = "Fetches the correct Knights Tour solution for the specified session and player."
        )
        @ApiResponses({
            @ApiResponse(
                responseCode = "200",
                description = "Correct answer retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = KnightsTourModel.CorrectAnswerResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "404", description = "Session not found"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
        })
    public ResponseEntity<KnightsTourModel.CorrectAnswerResponse> getCorrectAnswer(
            @Parameter(description = "Unique session identifier", example = "1001")
            @PathVariable Long sessionId,
            @Parameter(description = "Unique player identifier", example = "1")
            @RequestParam Long playerId
    ) {
        return ResponseEntity.ok(service.getCorrectAnswer(sessionId, playerId));
    }
}