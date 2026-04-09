package com.pdsa.games.traffic.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.pdsa.games.traffic.model.Road;
import com.pdsa.games.traffic.model.RoadId;
import com.pdsa.games.traffic.repository.RoadRepository;

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

    private static class Edge {
        String to;
        int capacity;
        int flow;
        Edge reverse;

        // Creates a connection between two nodes and stores how much traffic it can carry.
        Edge(String to, int capacity) {
            this.to = to;
            this.capacity = capacity;
        }

        // Returns how much unused space is still left on this road.
        int remainingCapacity() {
            return capacity - flow;
        }

        // Sends traffic through this road and updates the reverse road so the flow 
        //can be adjusted later.
        void augment(int bottleneck) {
            flow += bottleneck;
            reverse.flow -= bottleneck;
        }
    }

    // Loads the saved road network for this session and calculates the maximum 
    //flow from A to T using Dinic's algorithm.
    public int calculateDinicMaxFlow(Integer sessionId) {
        List<Road> roads = getRoadsBySession(sessionId);

        Map<String, List<Edge>> graph = buildGraph(roads);
        int maxFlow = 0;

        while (bfs(graph, "A", "T")) {
            Map<String, Integer> next = new HashMap<>();
            for (String node : graph.keySet()) {
                next.put(node, 0);
            }

            int flow;
            while ((flow = dfs(graph, "A", "T", Integer.MAX_VALUE, next)) > 0) {
                maxFlow += flow;
            }
        }

        return maxFlow;
    }

    private final Map<String, Integer> level = new HashMap<>();

    // Turns the list of roads into a graph that the max-flow algorithm can work with.
    private Map<String, List<Edge>> buildGraph(List<Road> roads) {
        Map<String, List<Edge>> graph = new HashMap<>();

        for (Road road : roads) {
            String from = road.getId().getStartNode();
            String to = road.getId().getEndNode();
            int capacity = road.getCapacity();

            graph.putIfAbsent(from, new ArrayList<>());
            graph.putIfAbsent(to, new ArrayList<>());

            Edge forward = new Edge(to, capacity);
            Edge backward = new Edge(from, 0);

            forward.reverse = backward;
            backward.reverse = forward;

            graph.get(from).add(forward);
            graph.get(to).add(backward);
        }

        return graph;
    }

    // Uses breadth-first search to group nodes by level, so the algorithm only 
    //follows useful forward paths.
    private boolean bfs(Map<String, List<Edge>> graph, String source, String sink) {
        level.clear();
        Queue<String> queue = new LinkedList<>();
        queue.offer(source);
        level.put(source, 0);

        while (!queue.isEmpty()) {
            String current = queue.poll();

            for (Edge edge : graph.get(current)) {
                if (edge.remainingCapacity() > 0 && !level.containsKey(edge.to)) {
                    level.put(edge.to, level.get(current) + 1);
                    queue.offer(edge.to);
                }
            }
        }

        return level.containsKey(sink);
    }

    // Uses depth-first search to push as much flow as possible through the current layered graph.
    private int dfs(Map<String, List<Edge>> graph, String current, String sink,
                    int flow, Map<String, Integer> next) {

        if (current.equals(sink)) return flow;

        List<Edge> edges = graph.get(current);

        for (int i = next.get(current); i < edges.size(); i++, next.put(current, i + 1)) {
            Edge edge = edges.get(i);

            if (edge.remainingCapacity() > 0 &&
                    level.get(edge.to) == level.get(current) + 1) {

                int bottleneck = dfs(
                        graph,
                        edge.to,
                        sink,
                        Math.min(flow, edge.remainingCapacity()),
                        next
                );

                if (bottleneck > 0) {
                    edge.augment(bottleneck);
                    return bottleneck;
                }
            }
        }

        return 0;
    }

    // Gets all the roads that belong to the given game session so the traffic map can be reused 
    //later.
    public List<Road> getRoadsBySession(Integer sessionId) {
        return roadRepository.findByIdSessionId(sessionId);
    }
}
