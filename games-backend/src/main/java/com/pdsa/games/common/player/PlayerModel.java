package com.pdsa.games.common.player;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "Player")
public class PlayerModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Player_Id", nullable = false)
    @Schema(
            description = "Auto-generated unique player identifier",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "1")
    private Integer playerId;

    @Column(name = "Player_Name", nullable = false, length = 45)
    @Schema(description = "Display name of the player", minLength = 1, maxLength = 45, example = "Alice")
    private String playerName;
}

