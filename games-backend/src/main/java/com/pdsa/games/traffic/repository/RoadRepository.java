package com.pdsa.games.traffic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import com.pdsa.games.traffic.model.Road;
import com.pdsa.games.traffic.model.RoadId;

public interface RoadRepository extends JpaRepository<Road, RoadId> {
    List<Road> findByIdSessionId(Integer sessionId);
}
