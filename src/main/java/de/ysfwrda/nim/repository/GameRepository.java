package de.ysfwrda.nim.repository;

import de.ysfwrda.nim.domain.Game;

import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

public interface GameRepository {

    Game save(Game game);

    Optional<Game> findById(UUID id);

    Optional<Game> update(UUID id, UnaryOperator<Game> mutation);
}
