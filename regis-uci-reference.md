# Regis: UCI implementation reference

This is a practical implementation checklist for standard chess, not Chess960. “First release” and “later” are Regis implementation priorities, not official UCI conformance categories.

## Transport and move notation

- Read commands from stdin; write protocol replies to stdout, one line per command, and flush promptly.
- Send ordinary logging and stack traces to stderr.
- Continue reading input while searching. Serialize output so worker threads cannot interleave partial lines.
- Accept extra spaces and tabs between tokens. Ignore unknown commands without crashing.
- Moves use coordinates: `e2e4`, `e1g1` (kingside castling), `e1c1` (queenside castling), `a7a8q` (promotion). Promotion letters are `q`, `r`, `b`, `n`.
- Use `0000` as the null move when no legal move exists. Never use it merely because a search timed out while legal moves remain.

## First-release commands

| GUI → engine | Expected engine → GUI | Meaning |
| --- | --- | --- |
| `uci` | `id name Regis`, `id author Luis Filipe Ribeiro`, supported `option` lines, then `uciok` | Identify engine and complete handshake. |
| `isready` | `readyok` | Acknowledge readiness; answer during search without stopping it. |
| `ucinewgame` | No direct reply | Clear game-specific search history/caches as appropriate. Wait for a subsequent `position`; do not start searching. |
| `position startpos [moves <move> ...]` | No direct reply | Replace the position, then apply the optional move sequence. |
| `position fen <six FEN fields> [moves <move> ...]` | No direct reply | Load the supplied position, then apply optional moves. |
| `go depth <plies>` | Optional `info`; ultimately `bestmove <move>` | Search with a depth limit. One ply is one move by one side. |
| `go movetime <milliseconds>` | Optional `info`; ultimately `bestmove <move>` | Search with a fixed time allowance. |
| `go wtime <ms> btime <ms> [winc <ms>] [binc <ms>] [movestogo <n>]` | Optional `info`; ultimately `bestmove <move>` | Search using the game clocks. Allocate part of the current side's remaining time. |
| `go infinite` | Optional `info`; `bestmove` after `stop` | Analyze until explicitly stopped. Do not finish early just because mate is found. |
| `stop` | Exactly one final `bestmove` for the active search | Stop promptly and return a usable result. Ignore when idle. |
| `quit` | No acknowledgement required | Cancel work, close executors, and terminate promptly. |

Square brackets denote optional syntax; do not transmit the brackets.

### Position examples

```text
position startpos
position startpos moves e2e4 e7e5 g1f3
position fen rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1
position fen rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1 moves d2d4 d7d5
```

The `moves` keyword terminates the FEN section. Parse the six FEN fields before it. Apply moves in order with your move handler. `position` itself does not trigger a search or produce `bestmove`.

### Search limits

```text
go depth 5
go movetime 3000
go wtime 60000 btime 58000 winc 1000 binc 1000
go wtime 120000 btime 115000 movestogo 20
go depth 8 movetime 5000
go infinite
```

Accept parameters in different orders. Missing limits must not become zero limits. When multiple finite limits apply, stop when an applicable limit is reached. `wtime` and `btime` are total remaining clock times, not a budget to spend entirely on this move. Leave a safety margin for communication and scheduling.

## Replies and output formats

### Identification

```text
id name Regis 0.1
id author Luis Filipe Ribeiro
uciok
```

Options, if any, go before `uciok`. An engine with no configurable options can omit all `option` lines.

### Best move

```text
bestmove e2e4
bestmove a7a8q
bestmove 0000
bestmove e2e4 ponder e7e5
```

The `ponder` suffix is optional and gives an expected opponent reply; it does not start another search. Return one `bestmove` when a normal search finishes, including when stopped. Prevent duplicate publication by the command thread and search workers.

### Search progress

```text
info depth 5 score cp 35 time 420 nodes 12000 nps 28571 pv e2e4 e7e5 g1f3
info depth 3 score mate 2 pv f5f6 h8h7 g4g7
info string Search stopped; returning last completed result
```

| Field | Format and meaning |
| --- | --- |
| `depth <n>` | Search depth in plies; report genuine search progress, not merely the requested limit. |
| `score cp <integer>` | Centipawn score from the root side-to-move perspective; positive favors that side. Convert your pawn-unit score by multiplying by 100. |
| `score mate <integer>` | Mate distance in moves, not plies. Negative means the root side is being mated. |
| `time <ms>` | Elapsed search time. |
| `nodes <n>` | Positions searched. |
| `nps <n>` | Nodes per second. |
| `pv <move> ...` | Legal principal variation beginning at the root position. |
| `multipv <n>` | One-based variation rank, when MultiPV is implemented. |
| `string <text>` | Human-readable text consuming the remainder of the line. |

Do not invent statistics or a PV you did not retain. Your current fixed mate reward proves a mate but does not encode its distance: track distance before publishing `score mate N`. You may initially omit scores and send `info string` diagnostics instead. A final `info` line before `bestmove` is useful.

## Additional UCI commands

| Command | Handling |
| --- | --- |
| `debug on` / `debug off` | Toggle protocol diagnostics; off initially. No acknowledgement. |
| `setoption name <name> [value <value>]` | Change an advertised option while idle. Names/values may contain spaces; button options omit the value. No generic acknowledgement; GUI can follow with `isready`. |
| `go searchmoves <move> ...` | Restrict root moves to the listed moves. Useful for analysis and testing. |
| `go nodes <n>` | Stop after the node budget. Requires counting nodes across workers. |
| `go mate <n>` | Search for mate within the requested number of moves. |
| `go ponder` | Background pondering. Do not emit the final move merely on finding mate or completing an ordinary iteration. |
| `ponderhit` | Switch an active ponder search to normal search when the predicted reply occurs. |
| `register later` / `register name <name> code <code>` | Registration support; unnecessary for Regis unless registration is introduced. |

Pondering and registration can be deferred. Do not advertise pondering capability before implementing it. For broader protocol support, implement the remaining `go` limits rather than silently treating them as unrelated searches.

### Option declarations

These are examples, not features Regis already has. Advertise only working options.

```text
option name Threads type spin default 1 min 1 max 16
option name Hash type spin default 16 min 1 max 1024
option name MultiPV type spin default 1 min 1 max 32
option name Ponder type check default false
option name Clear Hash type button
```

Corresponding GUI commands:

```text
setoption name Threads value 4
setoption name Hash value 64
setoption name MultiPV value 3
setoption name Ponder value false
setoption name Clear Hash
isready
```

Expected final reply: `readyok`. UCI also supports `combo` and `string` option types. `Threads` is a widely used option name; it is not a mandatory fixed UCI field.

For MultiPV, publish separate ranked lines:

```text
info depth 4 multipv 1 score cp 30 pv e2e4
info depth 4 multipv 2 score cp 20 pv d2d4
```

## Regis integration requirements

1. Keep the stdin command loop separate from background search.
2. Give each search its own cancellation signal, deadline, and identifier.
3. On `stop`, signal workers and publish a result once. Your mate-only AtomicBoolean is not a substitute for external cancellation.
4. Keep a legal fallback before evaluating root moves. Your current timed API can return an empty result even when legal moves exist.
5. In `go infinite` and pondering mode, retain a discovered mate but wait for the appropriate command before publishing `bestmove`.
6. Suppress stale callbacks from searches superseded by a new position or shutdown.
7. Do not mutate the root position while workers read it.
8. Emit only completed evaluations; never describe an aborted branch as an exact result.
9. For strength testing, use consistent clocks and report completed depth rather than claiming the requested depth was reached after a timeout.

These are implementation recommendations for the existing Regis design.

## Complete example session

`GUI>` and `REGIS>` indicate direction only; they are not transmitted.

```text
GUI> uci
REGIS> id name Regis 0.1
REGIS> id author Luis Filipe Ribeiro
REGIS> uciok
GUI> isready
REGIS> readyok
GUI> ucinewgame
GUI> isready
REGIS> readyok
GUI> position startpos moves e2e4 e7e5
GUI> go movetime 1000
REGIS> info depth 3 time 800 pv g1f3
REGIS> bestmove g1f3
GUI> position startpos moves e2e4 e7e5 g1f3 b8c6
GUI> go infinite
GUI> isready
REGIS> readyok
GUI> stop
REGIS> bestmove f1c4
GUI> quit
```

Moves and statistics above illustrate formatting, not guaranteed Regis output.

## Acceptance checklist

- [ ] Handshake ends with `uciok`.
- [ ] Every `isready` gets `readyok`, including during search.
- [ ] `position` accepts both forms and optional move sequences.
- [ ] Finite searches return legal `bestmove` output.
- [ ] Clock-based search reserves time for future moves.
- [ ] `stop` works before the first root evaluation finishes.
- [ ] `stop` while idle is harmless.
- [ ] No legal moves produces `bestmove 0000`.
- [ ] Infinite search waits for `stop`, including after finding mate.
- [ ] Workers cannot publish duplicate or stale final moves.
- [ ] Unknown commands and extra whitespace do not crash the process.
- [ ] `quit` terminates executors and the process.

## Sources

- Original UCI specification, April 2006, by Rudolf Huber and Stefan Meyer-Kahlen, reproduced without substantive changes: https://backscattering.de/chess/uci/
- Original specification distribution: https://www.shredderchess.com/download/div/uci.zip
- Stockfish documentation, for practical interoperability examples: https://official-stockfish.github.io/docs/stockfish-wiki/UCI-%26-Commands.html

The protocol reference is paraphrased; example values and Regis implementation priorities are original recommendations.
