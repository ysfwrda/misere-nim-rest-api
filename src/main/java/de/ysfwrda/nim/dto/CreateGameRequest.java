package de.ysfwrda.nim.dto;

import de.ysfwrda.nim.domain.strategy.StrategyType;
import jakarta.validation.constraints.Min;

public record CreateGameRequest(@Min(1) int heapSize, StrategyType strategy) {
}
