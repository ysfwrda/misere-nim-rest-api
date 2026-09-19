package de.ysfwrda.nim.domain.strategy;

public interface GameStrategy {

    int chooseMove(int heapSize);

    StrategyType type();
}
