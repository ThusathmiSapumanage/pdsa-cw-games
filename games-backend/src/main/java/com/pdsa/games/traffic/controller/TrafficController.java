package com.pdsa.games.traffic.controller;

import com.pdsa.games.traffic.model.Road;
import com.pdsa.games.traffic.service.TrafficService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
