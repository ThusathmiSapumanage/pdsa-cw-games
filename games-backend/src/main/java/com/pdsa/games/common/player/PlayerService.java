package com.pdsa.games.common.player;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PlayerService {
	private static final int PLAYER_NAME_MAX_LENGTH = 45;

	private final PlayerRepository playerRepository;

	public PlayerService(PlayerRepository playerRepository) {
		this.playerRepository = playerRepository;
	}

	public PlayerModel savePlayer(PlayerModel player) {
		if (player.getPlayerId() != null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "playerId must not be provided when creating a player");
		}

		validatePlayerName(player.getPlayerName(), false);

		return playerRepository.save(player);
	}

	public List<PlayerModel> getAllPlayers() {
		return playerRepository.findAll();
	}

	public Optional<PlayerModel> getPlayerById(Integer playerId) {
		return playerRepository.findById(playerId);
	}

	public Optional<PlayerModel> updatePlayer(Integer playerId, PlayerModel player) {
		if (player.getPlayerId() != null && !playerId.equals(player.getPlayerId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "playerId in path and body must match");
		}

		validatePlayerName(player.getPlayerName(), false);

		return playerRepository.findById(playerId)
				.map(existing -> {
					existing.setPlayerName(player.getPlayerName());
					return playerRepository.save(existing);
				});
	}

	public Optional<PlayerModel> patchPlayer(Integer playerId, PlayerModel player) {
		if (player.getPlayerId() != null && !playerId.equals(player.getPlayerId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "playerId in path and body must match");
		}

		validatePlayerName(player.getPlayerName(), true);

		return playerRepository.findById(playerId)
				.map(existing -> {
					if (player.getPlayerName() != null) {
						existing.setPlayerName(player.getPlayerName());
					}
					return playerRepository.save(existing);
				});
	}

	private void validatePlayerName(String playerName, boolean allowNull) {
		if (playerName == null) {
			if (allowNull) {
				return;
			}
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "playerName is required");
		}

		if (playerName.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "playerName must not be blank");
		}

		if (playerName.length() > PLAYER_NAME_MAX_LENGTH) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"playerName must be at most " + PLAYER_NAME_MAX_LENGTH + " characters");
		}
	}

	public void deletePlayer(Integer playerId) {
		playerRepository.deleteById(playerId);
	}
}
