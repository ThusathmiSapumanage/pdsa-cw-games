package com.pdsa.games.knightstour;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/knights-tour")
@CrossOrigin
public class KnightsTourController {

    private final KnightsTourService service;

    public KnightsTourController(KnightsTourService service) {
        this.service = service;
    }

    @PostMapping("/start")
    public ResponseEntity<KnightsTourModel.StartResponse> startRound(
            @RequestBody KnightsTourModel.StartRequest request
    ) {
        return ResponseEntity.ok(service.startRound(request));
    }

    @PostMapping("/submit")
    public ResponseEntity<KnightsTourModel.AnswerResponse> submitAnswer(
            @RequestBody KnightsTourModel.AnswerRequest request
    ) {
        return ResponseEntity.ok(service.submitAnswer(request));
    }

    @GetMapping("/{sessionId}/correct-answer")
    public ResponseEntity<KnightsTourModel.CorrectAnswerResponse> getCorrectAnswer(
            @PathVariable Long sessionId,
            @RequestParam Long playerId
    ) {
        return ResponseEntity.ok(service.getCorrectAnswer(sessionId, playerId));
    }
}