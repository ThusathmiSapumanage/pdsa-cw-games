package com.pdsa.games.queenspuzzle;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class QueensService {

	private static final int DEFAULT_BOARD_SIZE = 16;
	private static final int DEFAULT_QUEEN_COUNT = 16;
	private static final int MAX_BOARD_SIZE = 16;

	private static final String GAME_NAME = "Sixteen Queens Puzzle";
	private static final String SEQUENTIAL_NAME = "Sixteen Queens - Sequential";
	private static final String THREADED_NAME = "Sixteen Queens - Threaded";

	private final QueensRepository repository;
	private final QueensSolver solver = new QueensSolver();

	private static final ConcurrentMap<String, CachedBenchmark> BENCHMARK_CACHE = new ConcurrentHashMap<>();

	private record CachedBenchmark(int totalSolutions, double sequentialMs, double threadedMs) {
	}

	public QueensService(QueensRepository repository) {
		this.repository = repository;
	}

	@Transactional
	public QueensModel.StartResponse startRound(QueensModel.StartRequest request, String authenticatedEmail) {
		Long playerId = resolveAuthenticatedPlayerId(authenticatedEmail);

		QueensModel.StartRequest payload = request == null
				? new QueensModel.StartRequest(null, null)
				: request;

		Integer boardSizeValue = payload.getBoardSize();
		Integer queenCountValue = payload.getQueenCount();
		int boardSize = boardSizeValue != null ? boardSizeValue : DEFAULT_BOARD_SIZE;
		int queenCount = queenCountValue != null ? queenCountValue : DEFAULT_QUEEN_COUNT;

		validateBoardConfig(boardSize, queenCount);

		Integer gameId = repository.ensureGameId(GAME_NAME);
		Integer sequentialId = repository.ensureAlgorithmId(gameId, SEQUENTIAL_NAME);
		Integer threadedId = repository.ensureAlgorithmId(gameId, THREADED_NAME);

		Long sessionId = repository.createGameSession(gameId, playerId);

		String cacheKey = boardSize + "x" + queenCount;
		CachedBenchmark benchmark = BENCHMARK_CACHE.get(cacheKey);
		String threadedStatus = "SUCCESS";

		if (benchmark == null) {
			long sequentialStart = System.nanoTime();
			int sequentialCount = solver.solveSequentialCount(boardSize, queenCount);
			long sequentialEnd = System.nanoTime();

			long threadedStart = System.nanoTime();
			int threadedCount = solver.solveThreadedCount(boardSize, queenCount);
			long threadedEnd = System.nanoTime();

			double sequentialMs = nanosToMs(sequentialEnd - sequentialStart);
			double threadedMs = nanosToMs(threadedEnd - threadedStart);

			threadedStatus = threadedCount == sequentialCount ? "SUCCESS" : "ERROR";
			benchmark = new CachedBenchmark(sequentialCount, sequentialMs, threadedMs);
			BENCHMARK_CACHE.put(cacheKey, benchmark);
		}

		int totalSolutions = benchmark.totalSolutions();
		double sequentialMs = benchmark.sequentialMs();
		double threadedMs = benchmark.threadedMs();

		repository.saveSixteenQueensGame(sessionId, totalSolutions);
		repository.saveAlgorithmExecution(sessionId, sequentialId, sequentialMs, "SUCCESS");
		repository.saveAlgorithmExecution(sessionId, threadedId, threadedMs, threadedStatus);

		QueensModel.StartResponse response = new QueensModel.StartResponse();
		response.setSessionId(sessionId);
		response.setBoardSize(boardSize);
		response.setQueenCount(queenCount);
		response.setTotalSolutions(totalSolutions);
		response.setSequentialTimeMs(sequentialMs);
		response.setThreadedTimeMs(threadedMs);
		response.setMessage("Sixteen Queens solutions generated and stored.");

		return response;
	}

	@Transactional
	public QueensModel.SubmitResponse submitAnswer(QueensModel.SubmitRequest request, String authenticatedEmail) {
		if (request == null) {
			throw new IllegalArgumentException("Request payload is required.");
		}

		if (request.getSessionId() == null || request.getSessionId() <= 0) {
			throw new IllegalArgumentException("Valid sessionId is required.");
		}

		Long playerId = resolveAuthenticatedPlayerId(authenticatedEmail);

		if (!repository.sessionBelongsToPlayer(request.getSessionId(), playerId)) {
			throw new IllegalArgumentException("This session does not belong to the authenticated player.");
		}

		Integer totalSolutions = repository.getTotalSolutions(request.getSessionId());
		Integer discovered = repository.getDiscoveredCount(request.getSessionId());
		if (totalSolutions == null || discovered == null) {
			throw new IllegalStateException("Sixteen Queens game configuration not found for this session.");
		}

		List<QueensModel.Position> positions = normalizePositions(request.getQueens());

		try {
			validatePositions(positions, DEFAULT_BOARD_SIZE, DEFAULT_QUEEN_COUNT);
		} catch (IllegalArgumentException ex) {
			String responseSummary = buildResponseSummary(playerId, "INCORRECT");
			repository.saveResponse(request.getSessionId(), responseSummary, false);

			return new QueensModel.SubmitResponse(
					request.getSessionId(),
					playerId,
					false,
					false,
					discovered,
					totalSolutions,
					"Incorrect solution: " + ex.getMessage()
			);
		}

		Integer existingSolutionId = repository.findMatchingSolutionId(request.getSessionId(), positions);

		if (existingSolutionId != null) {
			return new QueensModel.SubmitResponse(
					request.getSessionId(),
					playerId,
					true,
					true,
					discovered,
					totalSolutions,
					"This solution was already discovered. Try a different one."
			);
		}

		int newSolutionId = repository.insertDiscoveredSolution(request.getSessionId(), positions);
		int updatedDiscovered = repository.getDiscoveredCount(request.getSessionId());

		String responseSummary = buildResponseSummary(playerId, "SOLUTION_" + newSolutionId);
		repository.saveResponse(request.getSessionId(), responseSummary, true);

		String message = "Correct solution recorded.";
		if (updatedDiscovered >= totalSolutions) {
			repository.resetDiscovered(request.getSessionId());
			updatedDiscovered = 0;
			message = "All solutions discovered. Progress has been reset for future players.";
		}

		return new QueensModel.SubmitResponse(
				request.getSessionId(),
				playerId,
				true,
				false,
				updatedDiscovered,
				totalSolutions,
				message
		);
	}

	@Transactional(readOnly = true)
	public QueensModel.StatusResponse getStatus(Long sessionId, String authenticatedEmail) {
		if (sessionId == null || sessionId <= 0) {
			throw new IllegalArgumentException("Valid sessionId is required.");
		}

		Long playerId = resolveAuthenticatedPlayerId(authenticatedEmail);

		if (!repository.sessionBelongsToPlayer(sessionId, playerId)) {
			throw new IllegalArgumentException("This session does not belong to the authenticated player.");
		}

		Integer totalSolutions = repository.getTotalSolutions(sessionId);
		Integer discovered = repository.getDiscoveredCount(sessionId);
		if (totalSolutions == null || discovered == null) {
			throw new IllegalStateException("Sixteen Queens game configuration not found for this session.");
		}

		QueensModel.StatusResponse response = new QueensModel.StatusResponse();
		response.setSessionId(sessionId);
		response.setTotalSolutions(totalSolutions);
		response.setSolutionsDiscovered(discovered);
		response.setMessage("Sixteen Queens progress loaded.");
		return response;
	}

	private Long resolveAuthenticatedPlayerId(String authenticatedEmail) {
		if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user email is missing.");
		}

		Long playerId = repository.findPlayerIdByEmail(authenticatedEmail);
		if (playerId == null || !repository.playerExists(playerId)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
					"Authenticated player does not exist. Make sure the logged-in user's email exists in Player.Player_Email.");
		}

		return playerId;
	}

	private void validateBoardConfig(int boardSize, int queenCount) {
		if (boardSize < 4 || boardSize > MAX_BOARD_SIZE) {
			throw new IllegalArgumentException("Board size must be between 4 and " + MAX_BOARD_SIZE + ".");
		}
		if (queenCount < 1 || queenCount > boardSize) {
			throw new IllegalArgumentException("Queen count must be between 1 and the board size.");
		}
	}

	private List<QueensModel.Position> normalizePositions(List<QueensModel.Position> positions) {
		if (positions == null) {
			throw new IllegalArgumentException("Queens positions are required.");
		}
		return positions;
	}

	private void validatePositions(List<QueensModel.Position> positions, int boardSize, int queenCount) {
		if (positions.size() != queenCount) {
			throw new IllegalArgumentException("Exactly " + queenCount + " queens are required.");
		}

		Set<String> occupied = new HashSet<>();
		Set<Integer> rows = new HashSet<>();
		Set<Integer> cols = new HashSet<>();
		Set<Integer> diag1 = new HashSet<>();
		Set<Integer> diag2 = new HashSet<>();

		for (QueensModel.Position position : positions) {
			if (position == null) {
				throw new IllegalArgumentException("Queen position entries must not be null.");
			}
			int x = position.getX();
			int y = position.getY();
			if (x < 1 || x > boardSize || y < 1 || y > boardSize) {
				throw new IllegalArgumentException("Queen positions must be within the board range.");
			}
			String key = x + ":" + y;
			if (!occupied.add(key)) {
				throw new IllegalArgumentException("Duplicate queen positions are not allowed.");
			}
			if (!rows.add(y) || !cols.add(x)) {
				throw new IllegalArgumentException("Queens must not share a row or column.");
			}
			int d1 = y - x;
			int d2 = y + x;
			if (!diag1.add(d1) || !diag2.add(d2)) {
				throw new IllegalArgumentException("Queens must not share a diagonal.");
			}
		}
	}

	private double nanosToMs(long nanos) {
		return nanos / 1_000_000.0;
	}

	private String buildResponseSummary(Long playerId, String responseKey) {
		String playerName = repository.findPlayerName(playerId);
		String safeName = playerName == null ? "Player" : playerName;
		String summary = safeName + ":" + responseKey;
		if (summary.length() > 50) {
			return summary.substring(0, 50);
		}
		return summary;
	}
}
