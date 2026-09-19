package de.ysfwrda.nim.config;

import de.ysfwrda.nim.domain.strategy.StrategyType;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nim")
public record NimProperties(StrategyType defaultStrategy) {
}
