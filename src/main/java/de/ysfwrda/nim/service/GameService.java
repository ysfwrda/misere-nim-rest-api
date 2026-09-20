package de.ysfwrda.nim.service;

import de.ysfwrda.nim.config.NimProperties;
import de.ysfwrda.nim.domain.Game;
import de.ysfwrda.nim.domain.Player;
import de.ysfwrda.nim.domain.strategy.GameStrategy;
import de.ysfwrda.nim.domain.strategy.StrategyType;
import de.ysfwrda.nim.dto.CreateGameRequest;
import de.ysfwrda.nim.dto.GameResponse;
import de.ysfwrda.nim.dto.MoveResponse;
import de.ysfwrda.nim.exception.GameNotFoundException;
import de.ysfwrda.nim.repository.GameRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GameService {

    private final GameRepository repository;
    private final NimProperties properties;
    private final Map<StrategyType, GameStrategy> strategies;

    public GameService(List<GameStrategy> strategies, GameRepository repository, NimProperties properties) {
        this.repository = repository;
        this.properties = properties;
        this.strategies = strategies.stream()
                .collect(Collectors.toMap(GameStrategy::type, Function.identity()));
    }

    public GameResponse createGame(CreateGameRequest request) {
        StrategyType strategyType = request.strategy() != null ? request.strategy() : properties.defaultStrategy();
        Game game = new Game(UUID.randomUUID(), request.heapSize(), strategyType);
        repository.save(game);
        return toGameResponse(game);
    }

    public MoveResponse move(UUID id, int count) {
        // Human move and computer reply must apply as one atomic turn, so concurrent
        // requests for the same game can't interleave. Hence the whole sequence runs
        // inside repository.update rather than find-then-save.
        AtomicReference<Integer> computerMove = new AtomicReference<>();
        Game game = repository.update(id, current -> {
            current.applyMove(count, Player.USER);
            if (!current.isGameOver()) {
                GameStrategy strategy = strategies.get(current.strategy());
                int move = strategy.chooseMove(current.heapSize());
                current.applyMove(move, Player.COMPUTER);
                computerMove.set(move);
            }
            return current;
        }).orElseThrow(() -> new GameNotFoundException("No game found with id " + id));
        return toMoveResponse(game, computerMove.get());
    }

    public GameResponse getGame(UUID id) {
        Game game = repository.findById(id)
                .orElseThrow(() -> new GameNotFoundException("No game found with id " + id));
        return toGameResponse(game);
    }

    private GameResponse toGameResponse(Game game) {
        return new GameResponse(
                game.id(), game.heapSize(), game.isGameOver(), game.nextPlayer(), game.winner(), game.strategy());
    }

    private MoveResponse toMoveResponse(Game game, Integer computerMove) {
        return new MoveResponse(
                game.id(), game.heapSize(), game.isGameOver(), game.nextPlayer(), game.winner(), game.strategy(),
                computerMove);
    }
}
