package com.pdsa.games.queenspuzzle;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/sixteen-queens")
@CrossOrigin
@Tag(name = "Sixteen Queens", description = "Generate solutions and submit answers for the 16x16 queens puzzle")
public class QueensController {

    private final QueensService service;

    public QueensController(QueensService service) {
	this.service = service;
    }

    @PostMapping("/start")
    @Operation(
	    summary = "Start a Sixteen Queens round",
	    description = "Generates all valid solutions using sequential and threaded backtracking and stores them."
    )
    @ApiResponses({
	    @ApiResponse(
		    responseCode = "200",
		    description = "Round started successfully",
		    content = @Content(mediaType = "application/json", schema = @Schema(implementation = QueensModel.StartResponse.class))
	    ),
	    @ApiResponse(responseCode = "400", description = "Invalid start request"),
	    @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<QueensModel.StartResponse> startRound(
	    @RequestBody QueensModel.StartRequest request) {
	return ResponseEntity.ok(service.startRound(request));
    }

    @PostMapping("/submit")
    @Operation(
	    summary = "Submit a Sixteen Queens answer",
	    description = "Validates a submitted board and records correct answers."
    )
    @ApiResponses({
	    @ApiResponse(
		    responseCode = "200",
		    description = "Submission processed successfully",
		    content = @Content(mediaType = "application/json", schema = @Schema(implementation = QueensModel.SubmitResponse.class))
	    ),
	    @ApiResponse(responseCode = "400", description = "Invalid submission"),
	    @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<QueensModel.SubmitResponse> submitAnswer(
	    @RequestBody QueensModel.SubmitRequest request) {
	return ResponseEntity.ok(service.submitAnswer(request));
    }

    @GetMapping("/{sessionId}/status")
    @Operation(
	    summary = "Get Sixteen Queens status",
	    description = "Returns the number of discovered solutions for the current session."
    )
    @ApiResponses({
	    @ApiResponse(
		    responseCode = "200",
		    description = "Status retrieved successfully",
		    content = @Content(mediaType = "application/json", schema = @Schema(implementation = QueensModel.StatusResponse.class))
	    ),
	    @ApiResponse(responseCode = "400", description = "Invalid request"),
	    @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<QueensModel.StatusResponse> getStatus(
	    @PathVariable Long sessionId,
	    @RequestParam Long playerId) {
	return ResponseEntity.ok(service.getStatus(sessionId, playerId));
    }
}
