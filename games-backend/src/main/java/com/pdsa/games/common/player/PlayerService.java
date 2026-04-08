package com.pdsa.games.common.player;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PlayerService {

	private final PlayerRepository playerRepository;

	public PlayerService(PlayerRepository playerRepository) {
		this.playerRepository = playerRepository;
	}

	public PlayerModel savePlayer(PlayerModel player) {
		if (player.getPlayerId() != null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "playerId must not be provided when creating a player");
		}

		return playerRepository.save(player);
	}

	public List<PlayerModel> getAllPlayers() {
		return playerRepository.findAll();
	}

	public Optional<PlayerModel> getPlayerById(Integer playerId) {
		return playerRepository.findById(playerId);
	}

	public void deletePlayer(Integer playerId) {
		playerRepository.deleteById(playerId);
	}
}
