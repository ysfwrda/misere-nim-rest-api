package de.ysfwrda.nim.domain.strategy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class RandomStrategyTest {

    @DisplayName("chooseMove always returns a value within 1..min(3, heap) for heap sizes 1 through 20")
    @Test
    void chooseMove_heapSizesOneToTwenty_staysWithinLegalRange() {
        RandomStrategy strategy = new RandomStrategy(new Random(42));

        for (int heapSize = 1; heapSize <= 20; heapSize++) {
            int move = strategy.chooseMove(heapSize);
            assertThat(move).isBetween(1, Math.min(3, heapSize));
        }
    }

    @Test
    void chooseMove_heapIsOne_returnsOne() {
        RandomStrategy strategy = new RandomStrategy(new Random(42));

        assertThat(strategy.chooseMove(1)).isEqualTo(1);
    }
}
