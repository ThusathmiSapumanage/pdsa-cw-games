package com.pdsa.games.traffic.service;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.pdsa.games.common.algorithm.AlgorithmModel;
import com.pdsa.games.common.algorithm.AlgorithmRepository;
import com.pdsa.games.common.algorithmExecute.AlgorithmExecuteId;
import com.pdsa.games.common.algorithmExecute.AlgorithmExecuteModel;
import com.pdsa.games.common.algorithmExecute.AlgorithmExecuteOutputResult;
import com.pdsa.games.common.algorithmExecute.AlgorithmExecuteRepository;
import com.pdsa.games.common.game.GameModel;
import com.pdsa.games.common.game.GameRepository;
import com.pdsa.games.common.gameSession.GameSessionModel;
import com.pdsa.games.common.gameSession.GameSessionRepository;
import com.pdsa.games.common.player.PlayerModel;
import com.pdsa.games.common.player.PlayerRepository;
import com.pdsa.games.common.response.ResponseModel;
import com.pdsa.games.common.response.ResponseRepository;
import com.pdsa.games.traffic.model.Road;
import com.pdsa.games.traffic.model.RoadId;
import com.pdsa.games.traffic.model.TrafficSimGame;
import com.pdsa.games.traffic.repository.RoadRepository;
import com.pdsa.games.traffic.repository.TrafficSimGameRepository;

@Service
public class TrafficService {
    private static final String SOURCE = "A";
    private static final String SINK = "T";
    private static final String GAME_NAME = "Traffic Simulation";
    private static final String DINIC_NAME = "Traffic - Dinic";
    private static final String FORD_FULKERSON_NAME = "Traffic - Ford-Fulkerson";
    private static final String[][] EDGES = {
            {"A", "B"}, {"A", "C"}, {"A", "D"},
            {"B", "E"}, {"B", "F"},
            {"C", "E"}, {"C", "F"},
            {"D", "F"},
            {"E", "G"}, {"E", "H"},
            {"F", "H"},
            {"G", "T"}, {"H", "T"}
    };

    private final RoadRepository roadRepository;
    private final TrafficSimGameRepository trafficSimGameRepository;
    private final GameRepository gameRepository;
    private final AlgorithmRepository algorithmRepository;
    private final AlgorithmExecuteRepository algorithmExecuteRepository;
    private final GameSessionRepository gameSessionRepository;
    private final PlayerRepository playerRepository;
    private final ResponseRepository responseRepository;
    private final Random random = new Random();

    public TrafficService(
            RoadRepository roadRepository,
            TrafficSimGameRepository trafficSimGameRepository,
            GameRepository gameRepository,
            AlgorithmRepository algorithmRepository,
            AlgorithmExecuteRepository algorithmExecuteRepository,
            GameSessionRepository gameSessionRepository,
            PlayerRepository playerRepository,
            ResponseRepository responseRepository
    ) {
        this.roadRepository = roadRepository;
        this.trafficSimGameRepository = trafficSimGameRepository;
        this.gameRepository = gameRepository;
        this.algorithmRepository = algorithmRepository;
        this.algorithmExecuteRepository = algorithmExecuteRepository;
        this.gameSessionRepository = gameSessionRepository;
        this.playerRepository = playerRepository;
        this.responseRepository = responseRepository;
    }

    @Transactional
    public List<Road> generateTrafficNetwork(Integer sessionId) {
        validateSession(sessionId);

        // Ensure the parent traffic game row exists before writing child road rows.
        TrafficSimGame trafficGame = trafficSimGameRepository.findById(sessionId)
                .orElseGet(() -> trafficSimGameRepository.save(new TrafficSimGame(sessionId, 0)));

        List<Road> existing = roadRepository.findByIdSessionId(sessionId);
        if (!existing.isEmpty()) {
            roadRepository.deleteAll(existing);
        }

        List<Road> roads = new ArrayList<>();
        for (String[] edge : EDGES) {
            int capacity = random.nextInt(11) + 5;
            roads.add(new Road(new RoadId(sessionId, edge[0], edge[1]), capacity));
        }

        List<Road> saved = roadRepository.saveAll(roads);
        int maxFlow = runDinic(buildCapacityGraphFromRoads(saved));
        trafficGame.setMaxFlowValue(maxFlow);
        trafficSimGameRepository.save(trafficGame);
        return saved;
    }

    @Transactional
    public AlgorithmRunResult calculateDinicMaxFlow(Integer sessionId) {
        Map<String, Map<String, Integer>> graph = cloneGraph(buildCapacityGraph(sessionId));
        long start = System.nanoTime();
        int maxFlow = runDinic(graph);
        double timeMs = nanosToMs(System.nanoTime() - start);
        saveAlgorithmExecution(sessionId, DINIC_NAME, timeMs, maxFlow, AlgorithmExecuteOutputResult.SUCCESS);
        return new AlgorithmRunResult("Dinic", maxFlow, timeMs);
    }

    @Transactional
    public AlgorithmRunResult calculateFordFulkersonMaxFlow(Integer sessionId) {
        Map<String, Map<String, Integer>> graph = cloneGraph(buildCapacityGraph(sessionId));
        long start = System.nanoTime();
        int maxFlow = dfsFordFulkerson(graph, SOURCE, SINK);
        double timeMs = nanosToMs(System.nanoTime() - start);
        saveAlgorithmExecution(sessionId, FORD_FULKERSON_NAME, timeMs, maxFlow, AlgorithmExecuteOutputResult.SUCCESS);
        return new AlgorithmRunResult("Ford-Fulkerson", maxFlow, timeMs);
    }

    @Transactional
    public AnswerSubmissionResult submitAnswer(Integer sessionId, Integer answer) {
        if (answer == null || answer < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "answer must be a non-negative integer");
        }

        GameSessionModel session = validateSession(sessionId);
        Integer correct = getSavedMaxFlow(sessionId);

        boolean alreadyCorrect = responseRepository.findWithLockBySessionId(sessionId)
                .stream()
                .anyMatch(r -> Boolean.TRUE.equals(r.getIsCorrect()));

        boolean isCorrect = !alreadyCorrect && correct.equals(answer);

        ResponseModel response = new ResponseModel();
        response.setSessionId(sessionId);
        response.setResponse(String.valueOf(answer));
        response.setIsCorrect(isCorrect);
        responseRepository.save(response);

        return new AnswerSubmissionResult(
            isCorrect,
            correct,
            getPlayerName(session.getPlayerId())
        );
    }

    public Integer getSavedMaxFlow(Integer sessionId) {
        validateSession(sessionId);
        return trafficSimGameRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Traffic game result not found"
                ))
                .getMaxFlowValue();
    }

    private Map<String, Map<String, Integer>> buildCapacityGraph(Integer sessionId) {
        validateSession(sessionId);
        List<Road> roads = roadRepository.findByIdSessionId(sessionId);
        if (roads.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Traffic network not found for session " + sessionId
            );
        }

        Map<String, Map<String, Integer>> graph = new HashMap<>();
        for (Road road : roads) {
            String from = road.getId().getStartNode();
            String to = road.getId().getEndNode();
            int cap = road.getCapacity();

            graph.putIfAbsent(from, new HashMap<>());
            graph.putIfAbsent(to, new HashMap<>());
            graph.get(from).put(to, cap);
            graph.get(to).putIfAbsent(from, 0);
        }

        if (!graph.containsKey(SOURCE) || !graph.containsKey(SINK)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Traffic network is missing source or sink nodes");
        }

        return graph;
    }

    private Map<String, Map<String, Integer>> buildCapacityGraphFromRoads(List<Road> roads) {
        Map<String, Map<String, Integer>> graph = new HashMap<>();
        for (Road road : roads) {
            String from = road.getId().getStartNode();
            String to = road.getId().getEndNode();
            int cap = road.getCapacity();

            graph.putIfAbsent(from, new HashMap<>());
            graph.putIfAbsent(to, new HashMap<>());
            graph.get(from).put(to, cap);
            graph.get(to).putIfAbsent(from, 0);
        }

        if (!graph.containsKey(SOURCE) || !graph.containsKey(SINK)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Traffic network is missing source or sink nodes");
        }

        return graph;
    }

    private Map<String, Map<String, Integer>> cloneGraph(
            Map<String, Map<String, Integer>> original
        ) {
            Map<String, Map<String, Integer>> copy = new HashMap<>();

            for (Map.Entry<String, Map<String, Integer>> entry : original.entrySet()) {
                copy.put(entry.getKey(), new HashMap<>(entry.getValue()));
            }

            return copy;
        }

    private int runDinic(Map<String, Map<String, Integer>> graph) {
        List<String> nodes = sortedNodes(graph);
        int maxFlow = 0;

        while (true) {
            Map<String, Integer> level = buildLevelGraph(graph, nodes);
            if (!level.containsKey(SINK)) {
                return maxFlow;
            }

            Map<String, Integer> nextEdge = new HashMap<>();
            int flow;
            do {
                flow = sendFlow(graph, SOURCE, SINK, Integer.MAX_VALUE, level, nextEdge, nodes);
                maxFlow += flow;
            } while (flow > 0);
        }
    }

    private Map<String, Integer> buildLevelGraph(Map<String, Map<String, Integer>> graph, List<String> nodes) {
        Map<String, Integer> level = new HashMap<>();
        Queue<String> queue = new ArrayDeque<>();
        queue.add(SOURCE);
        level.put(SOURCE, 0);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (String neighbor : orderedNeighbors(graph, current, nodes)) {
                if (graph.get(current).getOrDefault(neighbor, 0) > 0 && !level.containsKey(neighbor)) {
                    level.put(neighbor, level.get(current) + 1);
                    queue.add(neighbor);
                }
            }
        }

        return level;
    }

    private int sendFlow(
            Map<String, Map<String, Integer>> graph,
            String current,
            String sink,
            int flow,
            Map<String, Integer> level,
            Map<String, Integer> nextEdge,
            List<String> nodes
    ) {
        if (current.equals(sink)) {
            return flow;
        }

        List<String> neighbors = orderedNeighbors(graph, current, nodes);
        int startIndex = nextEdge.getOrDefault(current, 0);

        for (int i = startIndex; i < neighbors.size(); i++) {
            nextEdge.put(current, i + 1);

            String neighbor = neighbors.get(i);
            int capacity = graph.get(current).getOrDefault(neighbor, 0);
            if (capacity <= 0 || level.getOrDefault(neighbor, -1) != level.get(current) + 1) {
                continue;
            }

            int bottleneck = sendFlow(
                    graph,
                    neighbor,
                    sink,
                    Math.min(flow, capacity),
                    level,
                    nextEdge,
                    nodes
            );

            if (bottleneck > 0) {
                graph.get(current).put(neighbor, capacity - bottleneck);
                graph.get(neighbor).put(current, graph.get(neighbor).getOrDefault(current, 0) + bottleneck);
                return bottleneck;
            }
        }

        return 0;
    }

    private int dfsFordFulkerson(Map<String, Map<String, Integer>> graph, String source, String sink) {
        int maxFlow = 0;

        while (true) {
            Set<String> visited = new HashSet<>();
            int flow = dfs(graph, source, sink, Integer.MAX_VALUE, visited);
            if (flow == 0) {
                break;
            }
            maxFlow += flow;
        }

        return maxFlow;
    }

    private int dfs(Map<String, Map<String, Integer>> graph, String current,
                    String sink, int flow, Set<String> visited) {
        if (current.equals(sink)) {
            return flow;
        }

        visited.add(current);

        for (String neighbor : orderedNeighbors(graph, current, sortedNodes(graph))) {
            int capacity = graph.get(current).getOrDefault(neighbor, 0);
            if (capacity > 0 && !visited.contains(neighbor)) {
                int bottleneck = dfs(graph, neighbor, sink, Math.min(flow, capacity), visited);

                if (bottleneck > 0) {
                    graph.get(current).put(neighbor, capacity - bottleneck);
                    graph.get(neighbor).put(current,
                            graph.get(neighbor).getOrDefault(current, 0) + bottleneck);
                    return bottleneck;
                }
            }
        }

        return 0;
    }

    private List<String> sortedNodes(Map<String, Map<String, Integer>> graph) {
        List<String> nodes = new ArrayList<>(graph.keySet());
        nodes.sort(Comparator.naturalOrder());
        return nodes;
    }

    private List<String> orderedNeighbors(Map<String, Map<String, Integer>> graph, String node, List<String> nodes) {
        List<String> neighbors = new ArrayList<>(graph.getOrDefault(node, Map.of()).keySet());
        neighbors.sort(Comparator.<String>comparingInt(nodes::indexOf).thenComparing(String::compareTo));
        return neighbors;
    }

    private void saveAlgorithmExecution(
            Integer sessionId,
            String algorithmName,
            double timeMs,
            int maxFlow,
            AlgorithmExecuteOutputResult result
    ) {
        Integer gameId = ensureTrafficGame().getGameId();
        Integer algorithmId = ensureAlgorithm(gameId, algorithmName).getAlgorithmId();

        AlgorithmExecuteId executionId = new AlgorithmExecuteId(sessionId, algorithmId);
        Optional<AlgorithmExecuteModel> existing = algorithmExecuteRepository.findById(executionId);
        if (existing.isPresent()) {
            return; // preserve first execution, prevent overwrite
        }
        AlgorithmExecuteModel execution = new AlgorithmExecuteModel();
        execution.setSessionId(sessionId);
        execution.setAlgorithmId(algorithmId);
        execution.setExecutionTimeMs(BigDecimal.valueOf(timeMs));
        execution.setOutputResult(result);
        algorithmExecuteRepository.save(execution);
        execution.setMaxFlowResult(maxFlow);
        algorithmExecuteRepository.save(execution);
    }

    private GameModel ensureTrafficGame() {
        Optional<GameModel> existing = gameRepository.findByGameName(GAME_NAME);
        if (existing.isPresent()) {
            return existing.get();
        }

        try {
            GameModel game = new GameModel();
            game.setGameName(GAME_NAME);
            return gameRepository.save(game);
        } catch (DataIntegrityViolationException ex) {
            return gameRepository.findByGameName(GAME_NAME)
                    .orElseThrow(() -> ex);
        }
    }

    private AlgorithmModel ensureAlgorithm(Integer gameId, String algorithmName) {
        Optional<AlgorithmModel> existing = algorithmRepository.findByAlgorithmName(algorithmName);
        if (existing.isPresent()) {
            return existing.get();
        }

        try {
            AlgorithmModel algorithm = new AlgorithmModel();
            algorithm.setGameId(gameId);
            algorithm.setAlgorithmName(algorithmName);
            return algorithmRepository.save(algorithm);
        } catch (DataIntegrityViolationException ex) {
            return algorithmRepository.findByAlgorithmName(algorithmName)
                    .orElseThrow(() -> ex);
        }
    }

    private GameSessionModel validateSession(Integer sessionId) {
        if (sessionId == null || sessionId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sessionId must be a positive integer");
        }

        return gameSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                "Game session not found for sessionId " + sessionId
                    + ". Create a Game_Session first, then generate traffic."
                ));
    }

    private String getPlayerName(Integer playerId) {
        PlayerModel player = playerRepository.findById(playerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Player not found for playerId " + playerId
                ));
        return player.getPlayerName();
    }

    private double nanosToMs(long nanos) {
        return Math.round((nanos / 1_000_000.0) * 1000.0) / 1000.0;
    }

    public static class AlgorithmRunResult {
        private final String algorithm;
        private final int maxFlow;
        private final double timeMs;

        public AlgorithmRunResult(String algorithm, int maxFlow, double timeMs) {
            this.algorithm = algorithm;
            this.maxFlow = maxFlow;
            this.timeMs = timeMs;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public int getMaxFlow() {
            return maxFlow;
        }

        public double getTimeMs() {
            return timeMs;
        }
    }

    public static class AnswerSubmissionResult {
        private final boolean correct;
        private final Integer correctAnswer;
        private final String playerName;

        public AnswerSubmissionResult(boolean correct, Integer correctAnswer, String playerName) {
            this.correct = correct;
            this.correctAnswer = correctAnswer;
            this.playerName = playerName;
        }

        public boolean isCorrect() {
            return correct;
        }

        public Integer getCorrectAnswer() {
            return correctAnswer;
        }

        public String getPlayerName() {
            return playerName;
        }
    }
}
