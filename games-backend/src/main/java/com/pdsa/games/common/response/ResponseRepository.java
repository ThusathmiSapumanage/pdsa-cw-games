package com.pdsa.games.common.response;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResponseRepository extends JpaRepository<ResponseModel, Integer> {
    List<ResponseModel> findBySessionId(Integer sessionId);
}
