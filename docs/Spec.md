# Specification: Misère Nim REST API

## Context

This is a REST API built as a take-home task for a technical interview. I will submit it and
discuss it in an interview, so I must be able to explain and defend every decision in it.

This document is the specification the implementation was directed against. It was written
before any code existed. Decisions taken during implementation, and findings from the review
passes that followed, are recorded separately in [`Decisions.md`](Decisions.md).

## Task description, as given by the company

> **Coding Task: Misère Nim Game API**
>
> Implement the misère version of the Nim game as a small Spring Boot application.
>
> - The game is played with one heap of matches.
> - Two players take turns. On each turn, a player must take at least one and at most three matches from the heap.
> - The player who takes the last match loses.
>
> Your task is to expose a simple REST API so that a human player can play against the computer using curl (or Postman, etc.).
>
> **Requirements**
> - Use Spring Boot with Kotlin or Java.
> - No UI is required. A JSON API is enough.
> - The application should support: start a new game (initialize the heap with a given number of matches); make a move (human removes matches from a heap); computer move (server makes its move after the human); game state (return the current heap, whose turn it is, and whether the game is over).
>
> **Expectations**
> - The computer should play with a strategy (maybe random, maybe optimal). Ideally, you can configure which strategy to use.
> - Keep the code clean, modular, and testable.
> - Provide at least a few unit tests for the core game logic.
> - Include a short README with: how to build and run the application, and example curl commands for starting a game and making moves.
>
> **Be prepared**
> - To submit your solution at least 1 day prior to the next interview.
> - To present your solution in the next interview (ideally on your laptop).
> - To answer questions regarding your solution, code structure, readability and coding best practices. We just want to chat about your solution to simulate a typical work situation.

## Existing code, do NOT regenerate

The Spring Initializr scaffolding in the project `nim` already exists and must be used as is:
`NimApplication.java`, `application.yaml`, `NimApplicationTests.java`, and `pom.xml`.

Base package is `de.ysfwrda.nim`. Do not create a second package tree.

---

## Design

### Stack

Java 21, Spring Boot 4.1.1, Maven.

Spring Boot 4 is built on Spring Framework 7. Do not assume Spring Boot 3 idioms, imports or
test-slice behaviour carry over. Compile and run the existing test suite as the first step,
before any feature work.

Dependencies: `spring-boot-starter-web`, `spring-boot-starter-validation`,
`spring-boot-starter-test`, and their Boot 4 equivalents as present in the scaffold. No
others. No Lombok: DTOs are Java records, and the domain model is hand written so its
invariants stay under its own control.

### Package layout

```
de.ysfwrda.nim
├── controller
├── service
├── domain
│   ├── Game
│   ├── Player (enum: USER, COMPUTER)
│   └── strategy
│       ├── GameStrategy (interface)
│       ├── RandomStrategy
│       ├── OptimalStrategy
│       └── StrategyType (enum: RANDOM, OPTIMAL)
├── repository
├── dto
├── config
└── exception
```

### Game rules

One heap. Each turn a player takes 1 to 3 matches. Whoever takes the last match loses. The
human always moves first. After each human move the server immediately plays the computer
move, unless the human's move ended the game.

### Domain model

`Game` holds `id` (UUID), `heapSize` (int), `strategy` (StrategyType), `winner` (Player, null
while the game runs). No setters for `heapSize` or `winner`.

The constructor rejects a heap size below 1.

`applyMove(int count, Player player)` is the only way to change state. It rejects a count
outside 1 to 3, a count larger than the current heap, and any move when the game is already
over. Otherwise it subtracts the count, and if the heap reaches 0 it sets the winner to the
opponent of `player`, since the mover took the last match and therefore lost.

Read accessors: `isGameOver()`, `heapSize()`, `winner()`, `nextPlayer()`. `nextPlayer` returns
`USER` while the game runs and null once it is over, since the computer always replies within
the same request.

Plain Java class. No Spring annotations, no framework dependencies.

### Strategy

```java
public interface GameStrategy {
    int chooseMove(int heapSize);
    StrategyType type();
}
```

Both implementations are `@Component`.

`OptimalStrategy` returns `(heapSize - 1) % 4`. When that evaluates to 0 the position is lost
with correct play and no winning move exists, so it returns 1.

`RandomStrategy` takes a `java.util.Random` as a constructor argument, provided as a bean in
`config`. It returns a value in the range 1 to `min(3, heapSize)` inclusive.

`GameService` receives `List<GameStrategy>` by constructor injection and indexes it into a
`Map<StrategyType, GameStrategy>` keyed by `type()` once at construction.

### Configuration

`@ConfigurationProperties` bound to `nim.default-strategy` in `application.yaml`, defaulting
to `OPTIMAL`. A create request may override it. An unknown value in the request body is
rejected by Spring enum binding and must surface through the same problem+json handler as
every other 400.

### API

`POST /games` with `{ "heapSize": int, "strategy": "OPTIMAL" | "RANDOM" }`, where `strategy`
is optional and falls back to the configured default. Returns 201 with the game state.

`POST /games/{id}/moves` with `{ "count": int }`. Applies the human move, then the computer
move if the game is still running. Returns 200 with the state after both moves, including the
computer's move.

`GET /games/{id}` returns the current state. 200.

Request bodies are annotated `@Valid`, which is what makes the Bean Validation annotations on
the DTOs fire and produces `MethodArgumentNotValidException` on a violation.

### DTOs

All DTOs are Java records.

`CreateGameRequest`: `heapSize` with `@Min(1)`, `strategy` optional and nullable.

`MoveRequest`: `count` with `@Min(1) @Max(3)`.

`GameResponse`: `id`, `heapSize`, `gameOver`, `nextPlayer`, `winner`, `strategy`.

`MoveResponse`: the `GameResponse` fields plus `computerMove` (Integer, null when the human's
move ended the game).

Mapping from `Game` to the response records stays as private methods in `GameService`. No
mapper class, no MapStruct: two pure field copies do not justify either.

### Repository

```java
public interface GameRepository {
    Game save(Game game);
    Optional<Game> findById(UUID id);
    Optional<Game> update(UUID id, UnaryOperator<Game> mutation);
}
```

`InMemoryGameRepository` backed by `ConcurrentHashMap<UUID, Game>`. The `update` method uses
`computeIfPresent` so that loading, mutating and storing a game happen as one atomic operation
per key.

### Service

`GameService` has three methods.

`createGame(CreateGameRequest)` resolves the strategy, constructs the `Game`, saves it, maps
to `GameResponse`.

`move(UUID, int)` calls `repository.update`, and inside the mutation applies the human move,
then, if the game is still running, resolves the strategy, asks it for a move, and applies it
as COMPUTER. Maps the result to `MoveResponse`. Throws `GameNotFoundException` when the id is
unknown.

The computer's move count is computed inside the mutation lambda but needed outside it, and
`UnaryOperator<Game>` gives no return path for it. Capture it in an
`AtomicReference<Integer>` initialised to null, set only when the computer actually moves. Do
not use `AtomicInteger`, which cannot hold null and would force a sentinel value. Do not make
`update` generic to avoid the holder.

`getGame(UUID)` loads and maps, or throws `GameNotFoundException`.

The service contains no game rules. It orchestrates only.

### Errors

All error responses use RFC 9457 `application/problem+json`, produced by a single
`@RestControllerAdvice` returning Spring's `ProblemDetail`.

| Condition | Exception | Status |
|---|---|---|
| Heap size below 1 | `InvalidHeapSizeException` | 400 |
| Count outside 1 to 3, or larger than the heap | `IllegalMoveException` | 400 |
| Move on a finished game | `GameOverException` | 409 |
| Unknown game id | `GameNotFoundException` | 404 |
| Bean Validation failure | `MethodArgumentNotValidException` | 400 |
| Malformed body or unknown enum value | `HttpMessageNotReadableException` | 400 |
| Malformed UUID in the path | `MethodArgumentTypeMismatchException` | 400 |

The last three must be handled explicitly so Spring's default error body never reaches the
client and every error has the same shape.

### Required tests

**Domain (plain JUnit, no Spring):**
- rejects construction with heap size below 1
- rejects a count of 0 and of 4
- rejects a count larger than the remaining heap
- rejects any move once the game is over
- taking the last match sets the winner to the opponent of the mover
- heap decreases by exactly the count taken

**OptimalStrategy:**
- returns the move reaching `heap % 4 == 1` for a representative heap in each residue class
- `takesOneWhenNoWinningMoveExists` for heaps 1, 5, 9, 13
- never returns a value outside 1 to `min(3, heap)`

**RandomStrategy:**
- with a seeded `Random`, always returns a value in 1 to `min(3, heap)` for heap sizes 1 through 20
- returns 1 when the heap is 1

**Service (mocked strategy):**
- human move is applied before the computer move
- the computer does not move when the human's move ended the game
- unknown game id throws `GameNotFoundException`
- the configured default strategy is used when the create request omits one

**Controller (`@WebMvcTest`):**
- 201 on create, 200 on move and state, 404 on unknown id, 400 on invalid input, 409 on a finished game
- every error response has `application/problem+json` as its content type
- unknown strategy string in the create body returns 400
- negative heap size returns 400, distinct from zero
- missing or empty request body returns 400
- absent `count` field returns 400
- malformed UUID in the path returns 400

**End to end:**
- a full game played to completion against the optimal strategy from heap 13 with the human moving first, asserting the computer wins

### Out of scope

No persistence beyond the in-memory map. No security or authentication. No Docker. No OpenAPI
or Swagger. No multi-heap variant. No move history or audit trail. No distributed concurrency
handling beyond the atomic map update. No frontend. No caching, metrics or tracing.

### README

Build and run instructions, curl examples covering create, move and state for both strategies,
a short explanation of how the optimal strategy works, a pointer to `Decisions.md`, and the
out-of-scope list.

---

## Execution

Implement in two passes, stopping between them for review.

**Pass 1:** domain model, strategies, repository, and their tests. Stop.

**Pass 2:** service, controller, DTOs, error handling, configuration, README, `Decisions.md`.

## Acceptance criteria

- Everything in the design is implemented as written
- Every decision is recorded as an entry in `docs/Decisions.md`, stating the decision, the alternatives considered, and the reason
- The application builds and starts with no errors and no warnings
- All tests pass, none skipped or disabled
- The tests named in the design are required. Additional tests only where a public method has a branch the named tests do not reach. No tests for accessors
- Code is readable and modular
- Comments explain why, never what. A comment that restates the code does not belong. As many as needed, as few as possible

## Guardrails

1. Decisions about architecture, layering, API shape, error handling and dependencies are already made and must not be changed. Anything ambiguous, stop and ask rather than choosing.
2. Do not add classes, fields, endpoints, dependencies or abstractions that are not in this spec. If a dependency seems necessary, stop and ask.
3. Do not weaken, skip, disable or delete a test to make the suite green. If a test fails, fix the code or stop and ask.
4. When stopping to ask, present the options and wait. Do not pick one and note it.
5. This code will be read line by line and defended in an interview. Prefer the simplest construction that satisfies the design. No helper classes, no generics, no patterns beyond what is specified.
