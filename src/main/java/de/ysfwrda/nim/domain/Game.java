package de.ysfwrda.nim.domain;

import de.ysfwrda.nim.domain.strategy.StrategyType;
import de.ysfwrda.nim.exception.GameOverException;
import de.ysfwrda.nim.exception.IllegalMoveException;
import de.ysfwrda.nim.exception.InvalidHeapSizeException;

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
        this.id = id;
        this.heapSize = heapSize;
        this.strategy = strategy;
        this.winner = null;
    }

    // Game-over must be checked before the count bounds: once the heap is empty,
    // "count > heapSize" would always be true and would mask the real reason (game over).
    public void applyMove(int count, Player player) {
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

    // The computer always replies within the same request, so a running game
    // is always waiting on the human.
    public Player nextPlayer() {
        return isGameOver() ? null : Player.USER;
    }
}
