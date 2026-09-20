package de.ysfwrda.nim.domain;

import de.ysfwrda.nim.domain.strategy.StrategyType;
import de.ysfwrda.nim.exception.GameOverException;
import de.ysfwrda.nim.exception.IllegalMoveException;
import de.ysfwrda.nim.exception.InvalidHeapSizeException;

import java.util.Objects;
import java.util.UUID;

public class Game {

    private final UUID id;
    private final StrategyType strategy;
    private int heapSize;
    private Player winner;

    public Game(UUID id, int heapSize, StrategyType strategy) {
        if (heapSize < 1) {
            throw new InvalidHeapSizeException("Heap size must be at least 1, was " + heapSize);
        }
        Objects.requireNonNull(strategy, "strategy must not be null");
        this.id = id;
        this.heapSize = heapSize;
        this.strategy = strategy;
        this.winner = null;
    }

    public void applyMove(int count, Player player) {
        // Game over check as otherwise count is always > heapSize
        if (isGameOver()) {
            throw new GameOverException("Game is already over");
        }

        if (count < 1 || count > 3 || count > heapSize) {
            throw new IllegalMoveException("Count must be between 1 and 3 and not exceed the heap size");
        }

        heapSize -= count;
        if (heapSize == 0) {
            // Misère rule: taking the last match loses, so the mover's opponent wins.
            winner = player == Player.USER ? Player.COMPUTER : Player.USER;
        }
    }

    public UUID id() {
        return id;
    }

    public StrategyType strategy() {
        return strategy;
    }

    public int heapSize() {
        return heapSize;
    }

    public Player winner() {
        return winner;
    }

    public boolean isGameOver() {
        return winner != null;
    }

    public Player nextPlayer() {
        return isGameOver() ? null : Player.USER;
    }
}
