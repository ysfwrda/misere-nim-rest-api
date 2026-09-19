package de.ysfwrda.nim.service;

import de.ysfwrda.nim.config.NimProperties;
import de.ysfwrda.nim.domain.strategy.GameStrategy;
import de.ysfwrda.nim.domain.strategy.StrategyType;
import de.ysfwrda.nim.dto.CreateGameRequest;
import de.ysfwrda.nim.dto.GameResponse;
import de.ysfwrda.nim.dto.MoveResponse;
import de.ysfwrda.nim.exception.GameNotFoundException;
import de.ysfwrda.nim.repository.GameRepository;
import de.ysfwrda.nim.repository.InMemoryGameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GameServiceTest {

    private final GameStrategy strategy = mock(GameStrategy.class);
    private final GameRepository repository = new InMemoryGameRepository();
    private GameService service;

    @BeforeEach
    void setUp() {
        when(strategy.type()).thenReturn(StrategyType.OPTIMAL);
        service = new GameService(List.of(strategy), repository, new NimProperties(StrategyType.OPTIMAL));
    }

    @Test
    void humanMoveIsAppliedBeforeComputerMove() {
        GameResponse created = service.createGame(new CreateGameRequest(5, StrategyType.OPTIMAL));
        when(strategy.chooseMove(4)).thenReturn(2);

        MoveResponse response = service.move(created.id(), 1);

        // chooseMove must see the heap after the human's move (5 - 1 = 4), not the original heap.
        verify(strategy).chooseMove(4);
        assertThat(response.heapSize()).isEqualTo(2);
        assertThat(response.computerMove()).isEqualTo(2);
    }

    @Test
    void computerDoesNotMoveWhenHumanMoveEndsGame() {
        GameResponse created = service.createGame(new CreateGameRequest(1, StrategyType.OPTIMAL));

        MoveResponse response = service.move(created.id(), 1);

        assertThat(response.gameOver()).isTrue();
        assertThat(response.computerMove()).isNull();
        verify(strategy, never()).chooseMove(anyInt());
    }

    @Test
    void unknownGameIdThrowsGameNotFoundException() {
        assertThatThrownBy(() -> service.move(UUID.randomUUID(), 1))
                .isInstanceOf(GameNotFoundException.class);
    }

    @Test
    void createGameUsesConfiguredDefaultStrategyWhenOmitted() {
        GameResponse response = service.createGame(new CreateGameRequest(5, null));

        assertThat(response.strategy()).isEqualTo(StrategyType.OPTIMAL);
    }
}
