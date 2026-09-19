package de.ysfwrda.nim.domain.strategy;

import org.springframework.stereotype.Component;

@Component
public class OptimalStrategy implements GameStrategy {

    @Override
    public int chooseMove(int heapSize) {
        // Leaving a heap of size (4k + 1) is a loss for the opponent under correct play.
        // When the heap is already at that residue, no winning move exists; take one.
        int move = (heapSize - 1) % 4;
        return move == 0 ? 1 : move;
    }

    @Override
    public StrategyType type() {
        return StrategyType.OPTIMAL;
    }
}
