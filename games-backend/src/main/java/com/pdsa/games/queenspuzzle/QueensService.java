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
	private static final int DEFAULT_QUEEN_COUNT = 8;
	private static final int MAX_BOARD_SIZE = 16;

	private static final long MIN_TIME_BUDGET_MS = 500L;
	private static final long MAX_TIME_BUDGET_MS = 60_000L;

	private static final String GAME_NAME = "Sixteen Queens Puzzle";
	private static final String SEQUENTIAL_NAME = "Sixteen Queens - Sequential";
	private static final String THREADED_NAME = "Sixteen Queens - Threaded";

	private final QueensRepository repository;
	private final QueensSolver solver = new QueensSolver();
	private final ConcurrentMap<Long, Integer> sessionQueenCount = new ConcurrentHashMap<>();

	public QueensService(QueensRepository repository) {
		this.repository = repository;
	}

	@Transactional
	public QueensModel.StartResponse startRound(QueensModel.StartRequest request, String authenticatedEmail) {
		Long playerId = resolveAuthenticatedPlayerId(authenticatedEmail);

		QueensModel.StartRequest payload = request == null
				? new QueensModel.StartRequest(null, null, null, null)
				: request;

		Integer boardSizeValue = payload.getBoardSize();
		Integer queenCountValue = payload.getQueenCount();
		int boardSize = boardSizeValue != null ? boardSizeValue : DEFAULT_BOARD_SIZE;
		int queenCount = queenCountValue != null ? queenCountValue : DEFAULT_QUEEN_COUNT;
		Long timeBudgetMs = normalizeBudget(payload.getTimeBudgetMs());
		boolean skipCache = Boolean.TRUE.equals(payload.getSkipCache());

		validateBoardConfig(boardSize, queenCount);

		Integer gameId = repository.ensureGameId(GAME_NAME);
		Integer sequentialAlgoId = repository.ensureAlgorithmId(gameId, SEQUENTIAL_NAME);
		Integer threadedAlgoId = repository.ensureAlgorithmId(gameId, THREADED_NAME);

		Long sessionId = repository.createGameSession(gameId, playerId);
		sessionQueenCount.put(sessionId, queenCount);

		return timeBudgetMs == null
				? runFullSolve(sessionId, boardSize, queenCount, sequentialAlgoId, threadedAlgoId, skipCache)
				: runTimedSolve(sessionId, boardSize, queenCount, timeBudgetMs,
						sequentialAlgoId, threadedAlgoId);
	}

	private QueensModel.StartResponse runFullSolve(Long sessionId, int boardSize, int queenCount,
												   Integer sequentialAlgoId, Integer threadedAlgoId,
												   boolean skipCache) {
		QueensRepository.CachedCount cached = skipCache
				? null
				: repository.findCachedCount(boardSize, queenCount);

		int totalSolutions;
		double sequentialMs;
		double threadedMs;
		boolean fromCache = cached != null;

		if (fromCache) {
			totalSolutions = cached.totalSolutions();
			sequentialMs = cached.sequentialMs();
			threadedMs = cached.threadedMs();
		} else {
			long seqStart = System.nanoTime();
			int seqCount = solver.solveSequentialCount(boardSize, queenCount);
			sequentialMs = (System.nanoTime() - seqStart) / 1_000_000.0;

			long thStart = System.nanoTime();
			int thCount = solver.solveThreadedCount(boardSize, queenCount);
			threadedMs = (System.nanoTime() - thStart) / 1_000_000.0;

			if (seqCount != thCount) {
				throw new IllegalStateException(
						"Sequential and threaded solvers disagree: " + seqCount + " vs " + thCount);
			}
			totalSolutions = seqCount;
			repository.insertCachedCount(boardSize, queenCount, totalSolutions, sequentialMs, threadedMs);
		}

		repository.saveSixteenQueensGame(sessionId, totalSolutions);
		repository.saveAlgorithmExecution(sessionId, sequentialAlgoId, sequentialMs, "SUCCESS");
		repository.saveAlgorithmExecution(sessionId, threadedAlgoId, threadedMs, "SUCCESS");

		QueensModel.StartResponse response = new QueensModel.StartResponse();
		response.setSessionId(sessionId);
		response.setBoardSize(boardSize);
		response.setQueenCount(queenCount);
		response.setTotalSolutions(totalSolutions);
		response.setSequentialTimeMs(sequentialMs);
		response.setThreadedTimeMs(threadedMs);
		response.setSequentialFound(totalSolutions);
		response.setThreadedFound(totalSolutions);
		response.setSequentialHitLimit(false);
		response.setThreadedHitLimit(false);
		response.setTimeBudgetMs(null);
		response.setMessage(fromCache
				? "Cached full-solve result: " + totalSolutions + " solutions. Sequential "
						+ formatMs(sequentialMs) + " ms, Threaded " + formatMs(threadedMs) + " ms."
				: "Full solve complete: " + totalSolutions + " solutions. Sequential "
						+ formatMs(sequentialMs) + " ms, Threaded " + formatMs(threadedMs) + " ms.");
		return response;
	}

	private QueensModel.StartResponse runTimedSolve(Long sessionId, int boardSize, int queenCount,
													long timeBudgetMs,
													Integer sequentialAlgoId, Integer threadedAlgoId) {
		QueensSolver.TimedResult sequentialResult = solver.solveSequentialTimed(
				boardSize, queenCount, timeBudgetMs, QueensSolver.DEFAULT_MAX_STORED);
		QueensSolver.TimedResult threadedResult = solver.solveThreadedTimed(
				boardSize, queenCount, timeBudgetMs, QueensSolver.DEFAULT_MAX_STORED);

		QueensRepository.CachedCount cached = repository.findCachedCount(boardSize, queenCount);
		int totalSolutions = cached != null
				? cached.totalSolutions()
				: Math.max(sequentialResult.totalFound, threadedResult.totalFound);

		repository.saveSixteenQueensGame(sessionId, totalSolutions);
		repository.saveAlgorithmExecution(sessionId, sequentialAlgoId, sequentialResult.elapsedMs,
				sequentialResult.hitTimeLimit ? "TIMEOUT" : "SUCCESS");
		repository.saveAlgorithmExecution(sessionId, threadedAlgoId, threadedResult.elapsedMs,
				threadedResult.hitTimeLimit ? "TIMEOUT" : "SUCCESS");

		QueensModel.StartResponse response = new QueensModel.StartResponse();
		response.setSessionId(sessionId);
		response.setBoardSize(boardSize);
		response.setQueenCount(queenCount);
		response.setTotalSolutions(totalSolutions);
		response.setSequentialTimeMs((double) sequentialResult.elapsedMs);
		response.setThreadedTimeMs((double) threadedResult.elapsedMs);
		response.setSequentialFound(sequentialResult.totalFound);
		response.setThreadedFound(threadedResult.totalFound);
		response.setSequentialHitLimit(sequentialResult.hitTimeLimit);
		response.setThreadedHitLimit(threadedResult.hitTimeLimit);
		response.setTimeBudgetMs(timeBudgetMs);
		response.setMessage(buildTimedMessage(sequentialResult, threadedResult, timeBudgetMs));
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

		Integer storedQueenCount = sessionQueenCount.get(request.getSessionId());
		int expectedQueenCount = storedQueenCount != null ? storedQueenCount : DEFAULT_QUEEN_COUNT;

		try {
			validatePositions(positions, DEFAULT_BOARD_SIZE, expectedQueenCount);
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
		int updatedDiscovered;
		String message;
		String responseSummary;

		if (existingSolutionId != null) {
			if (repository.isSolutionDiscovered(request.getSessionId(), existingSolutionId)) {
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
			repository.markSolutionDiscovered(request.getSessionId(), existingSolutionId);
			updatedDiscovered = repository.getDiscoveredCount(request.getSessionId());
			responseSummary = buildResponseSummary(playerId, "SOLUTION_" + existingSolutionId);
			message = "Correct solution recorded.";
		} else {
			int newSolutionId = repository.insertDiscoveredSolution(request.getSessionId(), positions);
			updatedDiscovered = repository.getDiscoveredCount(request.getSessionId());
			responseSummary = buildResponseSummary(playerId, "SOLUTION_" + newSolutionId);
			message = "Correct solution recorded.";
		}

		repository.saveResponse(request.getSessionId(), responseSummary, true);

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

	private Long normalizeBudget(Long requestedBudget) {
		if (requestedBudget == null) {
			return null;
		}
		long value = requestedBudget;
		if (value < MIN_TIME_BUDGET_MS) {
			return MIN_TIME_BUDGET_MS;
		}
		if (value > MAX_TIME_BUDGET_MS) {
			return MAX_TIME_BUDGET_MS;
		}
		return value;
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

	private String buildTimedMessage(QueensSolver.TimedResult sequential,
									 QueensSolver.TimedResult threaded,
									 long timeBudgetMs) {
		String suffix = (sequential.hitTimeLimit || threaded.hitTimeLimit)
				? " (time-bounded — search hit the budget; partial counts shown)"
				: " (full search completed within budget)";
		return "Sequential found " + sequential.totalFound
				+ "; Threaded found " + threaded.totalFound
				+ " in " + timeBudgetMs + " ms each" + suffix;
	}

	private String formatMs(double ms) {
		return String.format("%.2f", ms);
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
