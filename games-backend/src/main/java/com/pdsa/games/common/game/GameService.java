package com.pdsa.games.common.game;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;


@Service
public class GameService {

	private final GameRepository gameRepository;

	public GameService(GameRepository gameRepository) {
		this.gameRepository = gameRepository;
	}

	public GameModel saveGame(GameModel game) {
		if (game.getGameId() != null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "gameId must not be provided when creating a game");
		}

		return gameRepository.save(game);
	}

	public List<GameModel> getAllGames() {
		return gameRepository.findAll();
	}

	public Optional<GameModel> getGameById(Integer gameId) {
		return gameRepository.findById(gameId);
	}

	public void deleteGame(Integer gameId) {
		gameRepository.deleteById(gameId);
	}
}
