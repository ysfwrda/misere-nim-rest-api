# Design Decisions

One entry per decision, with the reasoning behind it, so this document can be read on its own.

## How this code was produced

I wrote the design first: package layout, domain model, API contract, error table and the required tests, before any code existed. The implementation was then directed against that spec rather than written freehand, with the guardrail that nothing outside the spec gets added and anything ambiguous gets raised rather than decided silently. After the implementation was complete I ran review subagents over the finished code, one of them a correctness and null handling pass, which surfaced the findings in entries 14 and 15. Deciding which of those to fix and which to document and leave was mine. Every decision either of us made is recorded below.

---

## 1. Misère win condition

Taking the last match loses. `Game.applyMove` sets `winner` to the opponent of the player who just moved, but only when that move empties the heap. This single rule is what makes the game misère rather than normal Nim, and it is what shifts the optimal strategy's target position by one (see entry 5).

## 2. Game is a plain, framework-free class

No Spring annotations, no dependency on the web or persistence layers. A `Game` is created per game with `new`, not managed by the container, so there is nothing for the container to do with it. The practical payoff is that the game rules can be exercised with `new Game(...)` and a JUnit assertion: `GameTest` needs no application context and runs in milliseconds.

## 3. Validation is layered

`CreateGameRequest.heapSize` carries `@Min(1)` and `MoveRequest.count` carries `@Min(1) @Max(3)`, checked at the HTTP boundary before any domain object exists. `Game`'s constructor and `applyMove` enforce the same rules independently.

The division is clean rather than redundant: Bean Validation catches everything that can be judged from the request alone, and the domain catches everything that depends on current state. "Count larger than the remaining heap" is the clearest example, since no annotation can see the heap. The annotations are a convenience at the edge; the domain checks are the guarantee, and they hold no matter how the object is reached.

## 4. The computer moves synchronously, in the same request

`GameService.move` applies the human move and, if the game is not over, immediately asks the configured strategy for the computer's reply and applies it, all within one `POST /games/{id}/moves` call. This is what "a human player can play against the computer using curl" means in practice: no polling, no websocket, no second endpoint to trigger the computer.

It also keeps the state machine simple. A stored game is always either finished or waiting on the human, never waiting on itself.

## 5. OptimalStrategy's formula

`chooseMove(heapSize)` returns `(heapSize - 1) % 4`, or 1 when that evaluates to 0.

A heap of size `4k + 1` is lost for whoever has to move from it, because every reply the opponent can make leaves them able to restore the same shape, and the position eventually forces them to take the last match. The optimal player therefore always moves so that the heap it leaves behind satisfies `heap % 4 == 1`, and `(heapSize - 1) % 4` is the number of matches that achieves it.

When the current heap is already `4k + 1` there is no such move, because the position is already lost against correct play. The strategy takes 1 in that case. Game theoretically every move is equally lost from there, so this is not a stronger move; it simply leaves the maximum number of turns in which an imperfect opponent can go wrong.

This is also the point where misère diverges from normal Nim. If taking the last match won, the target would be `heap % 4 == 0` and the move would be `heap % 4`. The whole difference between the two games is that shift by one.

The end-to-end test starts from a heap of 13 for this reason, since 13 is `4 * 3 + 1`: with the human moving first against the optimal strategy, the human cannot win whatever they play.

## 6. Strategy lookup is a map, built once

`GameService`'s constructor takes `List<GameStrategy>`, which Spring populates with every `@Component` implementing the interface, and indexes it by `type()` into a `Map<StrategyType, GameStrategy>`. Lookup at move time is a map get.

The alternative was a switch or an if/else on `StrategyType`. The map means adding a third strategy is one new class and one new enum constant, with no change to `GameService`. That is the concrete justification for the interface existing at all: two implementations exist today and selection between them is a stated requirement, so the abstraction is paying for itself rather than anticipating a future.

## 7. GameRepository.update takes a UnaryOperator, backed by computeIfPresent

The human move and the computer's reply have to land as one atomic unit per game id. A plain find, mutate and save sequence is three separate operations, and two concurrent requests for the same game could interleave between any two of them: both read a heap of 10, both apply their move, and one write silently overwrites the other.

`ConcurrentHashMap.computeIfPresent` applies the remapping function atomically, holding the lock for that key while it runs, so the whole turn is serialized against any other turn on the same game. Different games still proceed in parallel, since the lock is per bin rather than per map.

Worth being explicit about the limit: this is an in-JVM mechanism. It does nothing across multiple instances. With a database and more than one instance the equivalent would be optimistic locking on a version column.

## 8. The computer's move is captured with AtomicReference

`update`'s `UnaryOperator<Game>` can only return a `Game`, so there is no return path for "what did the computer play", and `MoveResponse` needs that number. Java requires any local variable a lambda captures to be effectively final, so a plain `Integer` reassigned inside the lambda will not compile. The value has to travel out through an object whose reference never changes.

`AtomicInteger` was the first instinct, but it cannot hold null, which forces a sentinel such as -1 and a translation step back to null for the response field. `AtomicReference<Integer>` lets "the computer did not move" be the same null the DTO already uses.

Two alternatives were considered and rejected. Making `update` generic, `<R> Optional<R> update(UUID, Function<Game, R>)`, would let the lambda return the finished response directly, but it only relocates the same holder into the repository and gives up the "same type in, same type out" meaning that `UnaryOperator` carries. Deriving the computer's move arithmetically from the heap size before and after would avoid the holder entirely, but encodes a recorded fact as a difference, which is harder to read than it is short.

The holder is the price of doing both moves inside one atomic operation. It is kept at the call site, where the reason for it is visible, rather than hidden behind a generic repository method.

## 9. Mapping stays in the service as private methods

Two mappings exist, `toGameResponse(Game)` and `toMoveResponse(Game, Integer)`, and both are pure field copies with no logic in them.

MapStruct would add an annotation processor and a dependency to eliminate tedium at a scale this project does not have. A dedicated mapper class would exist only to be a separate file, and there is nothing in it worth testing that the controller tests do not already cover. The line where either becomes worthwhile is when mapping acquires logic, when more than one caller needs the same mapping, or when there are enough of them to make the service hard to read. None of those applies here.

## 10. MoveResponse duplicates GameResponse's fields

`MoveResponse` repeats the six fields of `GameResponse` (`id`, `heapSize`, `gameOver`, `nextPlayer`, `winner`, `strategy`) and adds `computerMove`, rather than extending or composing it. Java records cannot extend another record, so the options were a shared interface or a wrapped field. Six plain fields do not justify inventing a supertype that exists purely to avoid repeating them.

## 11. Errors are RFC 9457 ProblemDetail, via one @RestControllerAdvice

`GlobalExceptionHandler` holds one `@ExceptionHandler` per exception type, each returning Spring's built-in `ProblemDetail`, which is serialized as `application/problem+json` automatically.

`MethodArgumentNotValidException` and `HttpMessageNotReadableException` get explicit handlers for a specific reason: left unhandled, Spring's defaults return a different body shape, so the client would see two different error formats depending on which layer rejected the request. A Bean Validation failure and a domain rule violation look like the same class of mistake from outside, so they should look the same on the wire.

Status codes: 400 for input that is wrong regardless of any game's state, 409 for a request that is well formed but cannot be applied to this game right now, 404 for an unknown id. A move on a finished game is 409 rather than 400 because the identical request would have succeeded earlier in that game's life, which is what makes it a state conflict rather than bad input.

## 12. Malformed UUID in the path also maps to 400

`MethodArgumentTypeMismatchException` was added as a seventh row on top of the design's original six. A bad game id in the path, such as `/games/not-a-uuid`, is realistic input that the original table did not name, and without a handler it falls through to Spring's default non problem+json body. That would break the guarantee that every error response has the same shape, for a case anyone poking at the API by hand is likely to hit.

## 13. The scaffolded JPA dependency was removed

The generated `pom.xml` included `spring-boot-starter-data-jpa` and its test counterpart. There is no persistence layer here beyond `InMemoryGameRepository`'s `ConcurrentHashMap`, and left in place Spring Boot would attempt to auto-configure a `DataSource` with nothing to point it at, risking a startup failure. Both were removed, so the dependency set now matches the design exactly.

## 14. Guard against a null strategy

Found by a review subagent I ran over the finished code, checking correctness and null handling.

`strategies.get(current.strategy())` in `GameService.move` would throw an unhandled `NullPointerException` if `Game.strategy` were ever null. That exception is not covered by `GlobalExceptionHandler`, so the client would receive Spring's default error body instead of problem+json. The only path to it was a missing or misspelled `nim.default-strategy` property, since nothing validated `NimProperties.defaultStrategy` and `CreateGameRequest.strategy` falls back to it. Not reachable with the shipped `application.yaml`, but a real gap in the code.

Closed at two levels, matching the layering in entry 3. `NimProperties` is now `@Validated` with `@NotNull` on `defaultStrategy`, so a missing or misspelled property fails loudly at startup instead of binding null in silence. `Game`'s constructor rejects a null strategy with `Objects.requireNonNull`, as a guarantee for any future caller that does not come through the configuration path.

## 15. Known limitations, identified and deliberately not fixed

The same review surfaced two genuine gaps in the concurrency story described in entries 4 and 7. Both are recorded rather than fixed, because closing either one properly means revisiting that design rather than patching locally, and both sit outside the concurrency scope this project stated.

### 15.1 A concurrent GET can observe a transiently inconsistent game

`applyMove` performs two separate unsynchronized field writes: it decrements `heapSize`, then assigns `winner`. `update` serializes writers against each other through `computeIfPresent`, but `findById` is a plain lock-free `games.get(id)` that does not take part in that lock. A `GET /games/{id}` arriving in the window between those two writes could in principle see `heapSize == 0` with `winner == null`, a state the domain says cannot exist.

The window is a single statement wide and the endpoint is read-only, so the practical risk is very low. A real fix means either synchronizing reads against the same lock as writes, or making `Game` immutable and swapping in a new instance on each move. Both are larger changes than the stated scope allows.

### 15.2 A mid-turn failure leaves the human's move applied

The mutation lambda calls `applyMove` for the human and then, if the game continues, again for the computer, both against the same mutable `Game` instance. `computeIfPresent` rolling back on an exception only prevents a new entry being committed for an absent key. It does not undo in-place mutations already made to a value that was already there.

So if anything threw between the two calls, and the null strategy path in entry 14 was one concrete way that could happen, the human's move would remain applied while the client received an error and had no way to know. A real fix requires either snapshot and rollback on `Game`, or building a new `Game` inside the mutation and swapping it in only on full success. That is a change to the domain model's mutability, not something to add locally.

## 16. Out of scope

Carried over from the design, so it is clear what was deliberately not built.

- **No persistence beyond the in-memory map.** A take-home exercise, not a production service. `ConcurrentHashMap` is enough to demonstrate atomic per-game updates.
- **No security or authentication.** There is nothing here worth protecting, and adding it would obscure the game logic the task is about.
- **No distributed concurrency handling beyond the atomic map update.** A single in-memory map has no cross-instance story to solve. See entry 7 for what would replace it.
- **No multi-heap variant.** The task specifies one heap. Multiple heaps is a different game with a different optimal strategy.
- **No Docker, no OpenAPI or Swagger, no frontend, no move history or audit trail, no caching, metrics or tracing.**
