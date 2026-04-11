package com.pdsa.games.mincost.service;

import com.pdsa.games.mincost.model.Assignment;
import com.pdsa.games.mincost.model.HungarianStepDto;

import java.util.List;

/**
 * Result of running one assignment algorithm on a cost matrix.
 */
public record AlgorithmSolveResult(
        int totalCost,
        List<Assignment> assignments,
        List<String> logs,
        List<HungarianStepDto> hungarianSteps
) {
}
