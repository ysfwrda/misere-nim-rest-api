package de.ysfwrda.nim.domain.strategy;

import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class RandomStrategy implements GameStrategy {

    private final Random random;

    public RandomStrategy(Random random) {
        this.random = random;
    }

    @Override
    public int chooseMove(int heapSize) {
        return random.nextInt(1, Math.min(3, heapSize) + 1);
    }

    @Override
    public StrategyType type() {
        return StrategyType.RANDOM;
    }
}
