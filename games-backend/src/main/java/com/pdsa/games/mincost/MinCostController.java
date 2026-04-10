package com.pdsa.games.mincost;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mincost")
@CrossOrigin(origins = "*")
public class MinCostController {
    // Service has all business logic for this game.
    private final MinCostService minCostService;

    // Constructor injection.
    public MinCostController(MinCostService minCostService) {
        this.minCostService = minCostService;
    }

    // Starts one min-cost game round.
    // Accepts input JSON and returns matrix + algorithm results.
    @PostMapping("/start")
    public ResponseEntity<?> start(@RequestBody(required = false) MinCostService.MinCostStartRequest request) {
        try {
            // If body is empty, use defaults in service.
            MinCostService.MinCostStartRequest payload = request == null
                    ? new MinCostService.MinCostStartRequest(null, null, null)
                    : request;
            return ResponseEntity.ok(minCostService.startGame(payload));
        } catch (IllegalArgumentException ex) {
            // Validation errors come here.
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            // Any unknown server errors come here.
            return ResponseEntity.internalServerError().body(Map.of("error", "Unexpected server error"));
        }
    }
}
