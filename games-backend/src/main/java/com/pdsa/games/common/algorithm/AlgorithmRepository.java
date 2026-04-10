package com.pdsa.games.common.algorithm;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AlgorithmRepository extends JpaRepository<AlgorithmModel, Integer> {

    @Query(value = "SELECT * FROM Algorithm WHERE Game_Id = :gameId", nativeQuery = true)
    List<AlgorithmModel> findAllByGameId(@Param("gameId") Integer gameId);
}