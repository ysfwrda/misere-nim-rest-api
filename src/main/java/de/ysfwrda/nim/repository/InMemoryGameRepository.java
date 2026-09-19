package de.ysfwrda.nim.repository;

import de.ysfwrda.nim.domain.Game;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

@Repository
public class InMemoryGameRepository implements GameRepository {

    private final Map<UUID, Game> games = new ConcurrentHashMap<>();

    @Override
    public Game save(Game game) {
        games.put(game.id(), game);
        return game;
    }

    @Override
    public Optional<Game> findById(UUID id) {
        return Optional.ofNullable(games.get(id));
    }

    @Override
    public Optional<Game> update(UUID id, UnaryOperator<Game> mutation) {
        return Optional.ofNullable(games.computeIfPresent(id, (key, game) -> mutation.apply(game)));
    }
}
