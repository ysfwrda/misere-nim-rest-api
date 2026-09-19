package de.ysfwrda.nim.controller;

import de.ysfwrda.nim.domain.Player;
import de.ysfwrda.nim.domain.strategy.StrategyType;
import de.ysfwrda.nim.dto.GameResponse;
import de.ysfwrda.nim.dto.MoveResponse;
import de.ysfwrda.nim.exception.GameNotFoundException;
import de.ysfwrda.nim.exception.GameOverException;
import de.ysfwrda.nim.service.GameService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GameController.class)
class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GameService gameService;

    @Test
    void createGame_returns201WithGameState() throws Exception {
        UUID id = UUID.randomUUID();
        when(gameService.createGame(any())).thenReturn(
                new GameResponse(id, 5, false, Player.USER, null, StrategyType.OPTIMAL));

        mockMvc.perform(post("/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"heapSize": 5, "strategy": "OPTIMAL"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.heapSize").value(5));
    }

    @Test
    void move_returns200WithMoveState() throws Exception {
        UUID id = UUID.randomUUID();
        when(gameService.move(eq(id), eq(1))).thenReturn(
                new MoveResponse(id, 2, false, Player.USER, null, StrategyType.OPTIMAL, 2));

        mockMvc.perform(post("/games/{id}/moves", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"count": 1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.computerMove").value(2));
    }

    @Test
    void getGame_returns200WithGameState() throws Exception {
        UUID id = UUID.randomUUID();
        when(gameService.getGame(id)).thenReturn(
                new GameResponse(id, 5, false, Player.USER, null, StrategyType.OPTIMAL));

        mockMvc.perform(get("/games/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.heapSize").value(5));
    }

    @Test
    void getGame_unknownId_returns404ProblemJson() throws Exception {
        UUID id = UUID.randomUUID();
        when(gameService.getGame(id)).thenThrow(new GameNotFoundException("No game found with id " + id));

        mockMvc.perform(get("/games/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void createGame_zeroHeapSize_returns400ProblemJson() throws Exception {
        mockMvc.perform(post("/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"heapSize": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void createGame_negativeHeapSize_returns400ProblemJson() throws Exception {
        mockMvc.perform(post("/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"heapSize": -1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void move_countOutsideOneToThree_returns400ProblemJson() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/games/{id}/moves", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"count": 4}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void createGame_unknownStrategyValue_returns400ProblemJson() throws Exception {
        mockMvc.perform(post("/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"heapSize": 5, "strategy": "NOT_A_STRATEGY"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void createGame_missingBody_returns400ProblemJson() throws Exception {
        mockMvc.perform(post("/games")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void move_countFieldAbsent_returns400ProblemJson() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/games/{id}/moves", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void getGame_malformedUuid_returns400ProblemJson() throws Exception {
        mockMvc.perform(get("/games/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void move_onFinishedGame_returns409ProblemJson() throws Exception {
        UUID id = UUID.randomUUID();
        when(gameService.move(eq(id), anyInt())).thenThrow(new GameOverException("Game is already over"));

        mockMvc.perform(post("/games/{id}/moves", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"count": 1}
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
