# Misère Nim REST API

A REST API for playing misère Nim (one heap, take 1–3 matches per turn, whoever takes the
last match **loses**) against a computer opponent.

## Build and run

Requires Java 21.

```bash
./mvnw spring-boot:run
```

On Windows:

```powershell
mvnw.cmd spring-boot:run
```

The app starts on `http://localhost:8080`.

Run the tests:

```bash
./mvnw test
```

## API

### Start a new game

```bash
curl -X POST http://localhost:8080/games \
  -H "Content-Type: application/json" \
  -d '{"heapSize": 13, "strategy": "OPTIMAL"}'
```

`strategy` is optional (`OPTIMAL` or `RANDOM`); if omitted, the server falls back to the
value configured in `application.yaml` (`nim.default-strategy`, default `OPTIMAL`).

```bash
curl -X POST http://localhost:8080/games \
  -H "Content-Type: application/json" \
  -d '{"heapSize": 13, "strategy": "RANDOM"}'
```

Response (`201 Created`):

```json
{
  "id": "b6e...-...",
  "heapSize": 13,
  "gameOver": false,
  "nextPlayer": "USER",
  "winner": null,
  "strategy": "OPTIMAL"
}
```

### Make a move

The human move is applied first; if the game isn't over, the computer replies
immediately, in the same response.

```bash
curl -X POST http://localhost:8080/games/{id}/moves \
  -H "Content-Type: application/json" \
  -d '{"count": 2}'
```

Response (`200 OK`):

```json
{
  "id": "b6e...-...",
  "heapSize": 9,
  "gameOver": false,
  "nextPlayer": "USER",
  "winner": null,
  "strategy": "OPTIMAL",
  "computerMove": 2
}
```

### Get game state

```bash
curl http://localhost:8080/games/{id}
```

Response (`200 OK`): same shape as the create response.

### Errors

All errors are returned as `application/problem+json` (RFC 9457):

| Condition | Status |
|---|---|
| Heap size below 1 | 400 |
| Move count outside 1–3, or larger than the current heap | 400 |
| Malformed/missing request body, or an unknown `strategy` value | 400 |
| Malformed game id in the URL | 400 |
| Move on a game that's already over | 409 |
| Unknown game id | 404 |

```bash
curl -X POST http://localhost:8080/games/{id}/moves \
  -H "Content-Type: application/json" \
  -d '{"count": 4}'
```

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "count must be less than or equal to 3"
}
```

## Design decisions

The full reasoning behind every architectural and API decision — why the domain model
has no Spring dependencies, why the computer move happens synchronously, why the optimal
strategy uses `(heapSize - 1) % 4`, why errors use `ProblemDetail`, and more — is in
[`docs/Decisions.md`](docs/Decisions.md).

## Out of scope

- No persistence beyond the in-memory map
- No security or authentication
- No Docker
- No OpenAPI or Swagger
- No multi-heap variant
- No move history or audit trail
- No distributed concurrency handling beyond the atomic per-game map update
- No frontend
- No caching, metrics or tracing
