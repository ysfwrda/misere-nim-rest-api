package de.ysfwrda.nim.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record MoveRequest(@Min(1) @Max(3) int count) {
}
