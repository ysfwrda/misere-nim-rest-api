package de.ysfwrda.nim.domain;

import de.ysfwrda.nim.domain.strategy.StrategyType;
import de.ysfwrda.nim.exception.GameOverException;
import de.ysfwrda.nim.exception.IllegalMoveException;
import de.ysfwrda.nim.exception.InvalidHeapSizeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameTest {

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsGameConstructionWithHeapSizeBelowOne(int heapSize) {
        assertThatThrownBy(() -> new Game(UUID.randomUUID(), heapSize, StrategyType.OPTIMAL))
                .isInstanceOf(InvalidHeapSizeException.class);
    }

    @Test
    void rejectsGameConstructionWithNullStrategy() {
        assertThatThrownBy(() -> new Game(UUID.randomUUID(), 5, null))
                .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 4})
    void rejectsMoveCountOutsideOneToThree(int count) {
        Game game = new Game(UUID.randomUUID(), 5, StrategyType.OPTIMAL);

        assertThatThrownBy(() -> game.applyMove(count, Player.USER))
                .isInstanceOf(IllegalMoveException.class);
    }

    @Test
    void rejectsMoveCountLargerThanRemainingHeap() {
        Game game = new Game(UUID.randomUUID(), 2, StrategyType.OPTIMAL);

        assertThatThrownBy(() -> game.applyMove(3, Player.USER))
                .isInstanceOf(IllegalMoveException.class);
    }

    @Test
    void rejectsAnyMoveOnceGameIsOver() {
        Game game = new Game(UUID.randomUUID(), 1, StrategyType.OPTIMAL);
        game.applyMove(1, Player.USER);

        assertThatThrownBy(() -> game.applyMove(1, Player.COMPUTER))
                .isInstanceOf(GameOverException.class);
    }

    @Test
    void takingLastMatchSetsWinnerToOpponentOfMover() {
        Game game = new Game(UUID.randomUUID(), 1, StrategyType.OPTIMAL);

        game.applyMove(1, Player.USER);

        assertThat(game.winner()).isEqualTo(Player.COMPUTER);
    }



    @Test
    void heapDecreasesByExactlyTheCountTaken() {
        Game game = new Game(UUID.randomUUID(), 5, StrategyType.OPTIMAL);

        game.applyMove(2, Player.USER);

        assertThat(game.heapSize()).isEqualTo(3);
    }
}
