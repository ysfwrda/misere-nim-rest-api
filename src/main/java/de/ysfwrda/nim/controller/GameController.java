package de.ysfwrda.nim.controller;

import de.ysfwrda.nim.dto.CreateGameRequest;
import de.ysfwrda.nim.dto.GameResponse;
import de.ysfwrda.nim.dto.MoveRequest;
import de.ysfwrda.nim.dto.MoveResponse;
import de.ysfwrda.nim.service.GameService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GameResponse createGame(@Valid @RequestBody CreateGameRequest request) {
        return gameService.createGame(request);
    }

    @PostMapping("/{id}/moves")
    public MoveResponse move(@PathVariable UUID id, @Valid @RequestBody MoveRequest request) {
        return gameService.move(id, request.count());
    }

    @GetMapping("/{id}")
    public GameResponse getGame(@PathVariable UUID id) {
        return gameService.getGame(id);
    }
}
