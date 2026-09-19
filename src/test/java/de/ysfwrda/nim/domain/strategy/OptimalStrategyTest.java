package de.ysfwrda.nim.domain.strategy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class OptimalStrategyTest {

    private final OptimalStrategy strategy = new OptimalStrategy();

    // Heaps 6, 7, 8 cover the three residue classes (mod 4) other than 1, where a
    // winning move exists: each should leave the opponent a heap of 4k + 1.
    @ParameterizedTest
    @ValueSource(ints = {6, 7, 8})
    void returnsTheMoveReachingHeapModFourEqualsOne(int heapSize) {
        int move = strategy.chooseMove(heapSize);

        assertThat((heapSize - move) % 4).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 9, 13})
    void takesOneWhenNoWinningMoveExists(int heapSize) {
        assertThat(strategy.chooseMove(heapSize)).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13})
    void neverReturnsAValueOutsideOneToMinThreeHeap(int heapSize) {
        int move = strategy.chooseMove(heapSize);

        assertThat(move).isBetween(1, Math.min(3, heapSize));
    }
}
