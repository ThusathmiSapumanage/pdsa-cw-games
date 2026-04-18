package com.pdsa.games.traffic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.pdsa.games.traffic.model.TrafficSimGame;

public interface TrafficSimGameRepository extends JpaRepository<TrafficSimGame, Integer> {
}
