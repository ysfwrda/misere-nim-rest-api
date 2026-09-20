package de.ysfwrda.nim;

import de.ysfwrda.nim.domain.Player;
import de.ysfwrda.nim.domain.strategy.StrategyType;
import de.ysfwrda.nim.dto.CreateGameRequest;
import de.ysfwrda.nim.dto.GameResponse;
import de.ysfwrda.nim.dto.MoveRequest;
import de.ysfwrda.nim.dto.MoveResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureRestTestClient
class GameEndToEndTest {

    @Autowired
    private RestTestClient restTestClient;

    @DisplayName("The computer wins a full game from heap 13 against the optimal strategy")
    @Test
    void fullGame_heapThirteenAgainstOptimalStrategy_computerWins() {
        GameResponse created = restTestClient.post().uri("/games")
                .body(new CreateGameRequest(13, StrategyType.OPTIMAL))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(GameResponse.class)
                .returnResult()
                .getResponseBody();

        MoveResponse latest = null;
        int heapSize = created.heapSize();
        boolean gameOver = created.gameOver();

        // Heap 13 (≡ 1 mod 4) is a loss for whoever moves first against optimal play,
        // regardless of what the human takes each turn, so any legal move sequence works here.
        while (!gameOver) {
            int humanMove = Math.min(3, heapSize);
            latest = restTestClient.post().uri("/games/{id}/moves", created.id())
                    .body(new MoveRequest(humanMove))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(MoveResponse.class)
                    .returnResult()
                    .getResponseBody();
            heapSize = latest.heapSize();
            gameOver = latest.gameOver();
        }

        assert latest != null;
        assertThat(latest.winner()).isEqualTo(Player.COMPUTER);
    }
}
