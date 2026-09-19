package de.ysfwrda.nim.dto;

import de.ysfwrda.nim.domain.Player;
import de.ysfwrda.nim.domain.strategy.StrategyType;

import java.util.UUID;

public record GameResponse(
        UUID id,
        int heapSize,
        boolean gameOver,
        Player nextPlayer,
        Player winner,
        StrategyType strategy) {
}
