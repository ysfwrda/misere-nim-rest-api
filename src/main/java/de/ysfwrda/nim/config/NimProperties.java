package de.ysfwrda.nim.config;

import de.ysfwrda.nim.domain.strategy.StrategyType;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

// @Validated so a missing/misspelled nim.default-strategy fails fast at startup,
// instead of binding to null and only surfacing as an NPE on the first move.
@ConfigurationProperties(prefix = "nim")
@Validated
public record NimProperties(@NotNull StrategyType defaultStrategy) {
}
