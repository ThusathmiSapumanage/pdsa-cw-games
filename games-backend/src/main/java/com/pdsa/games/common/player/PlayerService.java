package com.pdsa.games.common.player;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PlayerService {
	private static final int PLAYER_NAME_MAX_LENGTH = 45;
	private static final int PLAYER_EMAIL_MIN_LENGTH = 3;
	private static final int PLAYER_EMAIL_MAX_LENGTH = 45;
	private static final int PLAYER_PASSWORD_MIN_LENGTH = 6;
	private static final int PLAYER_PASSWORD_MAX_LENGTH = 255;
	private static final Pattern PLAYER_EMAIL_REGEX = Pattern
			.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

	private final PlayerRepository playerRepository;
	private final PasswordEncoder passwordEncoder;

	public PlayerService(PlayerRepository playerRepository) {
		this.playerRepository = playerRepository;
		this.passwordEncoder = new BCryptPasswordEncoder();
	}

	public PlayerModel savePlayer(PlayerModel player) {
		if (player.getPlayerId() != null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "playerId must not be provided when creating a player");
		}

		validatePlayerName(player.getPlayerName(), false);
		validatePlayerEmail(player.getPlayerEmail(), false);
		validatePlayerPassword(player.getPlayerPassword(), false);

		player.setPlayerPassword(passwordEncoder.encode(player.getPlayerPassword()));

		return playerRepository.save(player);
	}

	public List<PlayerModel> getAllPlayers() {
		return playerRepository.findAll();
	}

	public Optional<PlayerModel> getPlayerById(Integer playerId) {
		return playerRepository.findById(playerId);
	}

	public Optional<PlayerModel> loginPlayer(String playerEmail, String playerPassword) {
		if (playerEmail == null || playerEmail.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "playerEmail is required");
		}

		if (playerPassword == null || playerPassword.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "playerPassword is required");
		}

		return playerRepository.findByPlayerEmail(playerEmail)
				.filter(player -> passwordEncoder.matches(playerPassword, player.getPlayerPassword()));
	}

	public Optional<PlayerModel> updatePlayer(Integer playerId, PlayerModel player) {
		if (player.getPlayerId() != null && !playerId.equals(player.getPlayerId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "playerId in path and body must match");
		}

		validatePlayerName(player.getPlayerName(), false);
		validatePlayerEmail(player.getPlayerEmail(), false);
		validatePlayerPassword(player.getPlayerPassword(), false);

		return playerRepository.findById(playerId)
				.map(existing -> {
					existing.setPlayerName(player.getPlayerName());
					existing.setPlayerEmail(player.getPlayerEmail());
					existing.setPlayerPassword(passwordEncoder.encode(player.getPlayerPassword()));
					return playerRepository.save(existing);
				});
	}

	public Optional<PlayerModel> patchPlayer(Integer playerId, PlayerModel player) {
		if (player.getPlayerId() != null && !playerId.equals(player.getPlayerId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "playerId in path and body must match");
		}

		validatePlayerName(player.getPlayerName(), true);
		validatePlayerEmail(player.getPlayerEmail(), true);
		validatePlayerPassword(player.getPlayerPassword(), true);

		return playerRepository.findById(playerId)
				.map(existing -> {
					if (player.getPlayerName() != null) {
						existing.setPlayerName(player.getPlayerName());
					}
					if (player.getPlayerEmail() != null) {
						existing.setPlayerEmail(player.getPlayerEmail());
					}
					if (player.getPlayerPassword() != null) {
						existing.setPlayerPassword(passwordEncoder.encode(player.getPlayerPassword()));
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

	private void validatePlayerEmail(String playerEmail, boolean allowNull) {
		if (playerEmail == null) {
			if (allowNull) {
				return;
			}
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "playerEmail is required");
		}

		if (playerEmail.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "playerEmail must not be blank");
		}

		if (playerEmail.length() < PLAYER_EMAIL_MIN_LENGTH) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"playerEmail must be at least " + PLAYER_EMAIL_MIN_LENGTH + " characters");
		}

		if (playerEmail.length() > PLAYER_EMAIL_MAX_LENGTH) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"playerEmail must be at most " + PLAYER_EMAIL_MAX_LENGTH + " characters");
		}

		if (!PLAYER_EMAIL_REGEX.matcher(playerEmail).matches()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "playerEmail format is invalid");
		}
	}

	private void validatePlayerPassword(String playerPassword, boolean allowNull) {
		if (playerPassword == null) {
			if (allowNull) {
				return;
			}
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "playerPassword is required");
		}

		if (playerPassword.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "playerPassword must not be blank");
		}

		if (playerPassword.length() < PLAYER_PASSWORD_MIN_LENGTH) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"playerPassword must be at least " + PLAYER_PASSWORD_MIN_LENGTH + " characters");
		}

		if (playerPassword.length() > PLAYER_PASSWORD_MAX_LENGTH) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"playerPassword must be at most " + PLAYER_PASSWORD_MAX_LENGTH + " characters");
		}
	}

	public void deletePlayer(Integer playerId) {
		playerRepository.deleteById(playerId);
	}
}
