package com.pdsa.games.traffic.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pdsa.games.traffic.model.Road;
import com.pdsa.games.traffic.service.TrafficService;

@RestController
@RequestMapping("/api/traffic-sim")
@CrossOrigin(origins = "*") // Allow localhost frontend
public class TrafficController {

    private final TrafficService trafficService;

    public TrafficController(TrafficService trafficService) {
        this.trafficService = trafficService;
    }

    @PostMapping("/generate/{sessionId}")
    public ResponseEntity<List<Road>> generateNetwork(@PathVariable Integer sessionId) {
        List<Road> generatedRoads = trafficService.generateTrafficNetwork(sessionId);
        return ResponseEntity.ok(generatedRoads);
    }

    @GetMapping("/dinic/{sessionId}")
    public ResponseEntity<Map<String, Object>> calculateDinic(@PathVariable Integer sessionId) {
        long start = System.nanoTime();
        int maxFlow = trafficService.calculateDinicMaxFlow(sessionId);
        long end = System.nanoTime();

        Map<String, Object> response = new HashMap<>();
        response.put("maxFlow", maxFlow);
        response.put("timeMs", (end - start) / 1_000_000.0);

        return ResponseEntity.ok(response);
    }
}
