package com.pdsa.games.traffic.service;

import com.pdsa.games.traffic.model.Road;
import com.pdsa.games.traffic.model.RoadId;
import com.pdsa.games.traffic.repository.RoadRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class TrafficService {

    private final RoadRepository roadRepository;
    private final Random random;

    public TrafficService(RoadRepository roadRepository) {
        this.roadRepository = roadRepository;
        this.random = new Random();
    }

    public List<Road> generateTrafficNetwork(Integer sessionId) {
        List<Road> roads = new ArrayList<>();

        // Define the hardcoded edges
        String[][] edges = {
                { "A", "B" }, { "A", "C" },
                { "B", "D" }, { "B", "C" },
                { "C", "D" }, { "C", "E" },
                { "D", "F" }, { "D", "G" },
                { "E", "G" }, { "E", "H" },
                { "F", "T" },
                { "G", "F" }, { "G", "T" },
                { "H", "T" }
        };

        for (String[] edge : edges) {
            String startNode = edge[0];
            String endNode = edge[1];

            // Get random capacity between 5 and 15
            int capacity = random.nextInt(11) + 5;

            RoadId id = new RoadId(sessionId, startNode, endNode);
            Road road = new Road(id, capacity);
            roads.add(road);
        }

        // Delete any existing roads for this session (to allow "Play Again")
        // ! check if every try is a seperate session, if so this is not needed
        List<Road> existingRoads = roadRepository.findByIdSessionId(sessionId);
        if (!existingRoads.isEmpty()) {
            roadRepository.deleteAll(existingRoads);
        }

        // Save and return array to controller
        return roadRepository.saveAll(roads);
    }
}
