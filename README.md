# Regis — TODO

## 1. Make timed searches useful

- [ ] Add iterative deepening: complete depth 1, then 2, then 3, until stopped.
- [ ] Use one shared deadline across all iterations and root evaluators.
- [ ] Keep the last fully completed iteration when a deeper iteration times out.
- [ ] Track whether every root move finished; a nonempty result array doesn't prove completion.
- [ ] Preserve completed results on external stop instead of losing them when invokeAll is interrupted.
- [ ] Keep a legal fallback only for searches where no iteration completed.
- [ ] Report fallback usage through `info string`.
- [ ] Check cancellation during recursion and expensive operations.

## 2. Complete the UCI interface

### Commands and limits

- [x] Engine identification: `uci`, `id`, `uciok`.
- [x] Readiness: `isready`, `readyok`.
- [x] Position loading: `startpos`, full FEN, optional move sequence.
- [x] Basic depth-limited search: `go depth N`.
- [x] Basic timed-search plumbing: `go movetime N` — needs iterative deepening above.
- [x] Basic `stop`, `ucinewgame`, and `quit` handling.
- [ ] Add clock controls: `wtime`, `btime`, `winc`, `binc`, `movestogo`.
- [ ] Allocate time per move with a safety margin for GUI/process overhead.
- [ ] Add `go infinite`; don't publish bestmove until stopped.
- [ ] Add node limits: `go nodes N`.
- [ ] Add root-move restrictions: `go searchmoves ...`.
- [ ] Add mate-search limits: `go mate N`.
- [ ] Add `debug on/off`.
- [ ] Add `setoption` and advertise only options that actually work.
- [ ] Later: add `go ponder` and `ponderhit`.

### Search lifecycle and output

- [ ] Verify exactly one final `bestmove` per active search.
- [ ] Suppress obsolete results after replacing a position or search.
- [ ] Keep the command loop responsive while searching and stopping.
- [ ] Ensure quit/EOF terminate the coordinator and workers cleanly.
- [ ] Emit `bestmove 0000` only when no legal move exists.
- [ ] Send completed depth, elapsed time, nodes, and nodes/second using `info`.
- [ ] Retain and publish a legal principal variation.
- [ ] Publish centipawn scores from the root side's perspective.
- [ ] Track actual mate distance before publishing `score mate N`.
- [ ] Later: MultiPV with ranked lines at consistent search depth.

## 3. Improve search efficiency

- [ ] Add alpha-beta pruning.
- [ ] Order moves: previous best move, promising captures, promotions, then quiet moves.
- [ ] Add a bounded transposition table shared across evaluators of one search.
- [ ] Start with an exact position key and remaining depth; measure key-generation cost.
- [ ] Add exact/lower-bound/upper-bound entries when combining the table with alpha-beta.
- [ ] Never store cancelled or unfinished search results.
- [ ] Keep score perspective consistent across cached entries.
- [ ] Later: use Zobrist hashing, with collision handling.
- [ ] Measure allocation and copying costs before changing board representation.
- [ ] Compare parallel root search with sequential search under equal resource limits.

## 4. Improve playing strength

- [ ] Add quiescence search to reduce evaluation in the middle of exchanges.
- [ ] Handle check evasions correctly in quiescence; don't allow stand-pat while in check.
- [ ] Add piece-square tables with correct indexing for both colors.
- [ ] Add development and central-control incentives where not already captured by tables.
- [ ] Add mobility evaluation.
- [ ] Add king safety and castling-related evaluation.
- [ ] Add pawn structure: isolated, doubled, and passed pawns.
- [ ] Use different king placement preferences in middlegames and endgames.
- [ ] Prefer shorter forced mates and delay unavoidable mate.
- [ ] Tune one change at a time against the baseline.

## 5. Complete game-result handling

- [x] Basic checkmate and stalemate detection in search.
- [ ] Track position history for repetition detection.
- [ ] Handle claimable draws separately from automatic draws.
- [ ] Support the 50-move and automatic 75-move rules, including mate precedence.
- [ ] Support threefold claims and automatic fivefold repetition.
- [ ] Detect supported dead-position cases conservatively; start with obvious insufficient-material cases.
- [ ] Make transposition reuse respect draw-relevant state and history.

## 6. Strengthen correctness tests

- [ ] Run standard perft positions against known node counts.
- [ ] Cover castling, promotions, en passant, pins, checks, and double check in perft fixtures.
- [ ] Keep tactical engine tests based on moves, not fixed heuristic scores.
- [ ] Accept any valid forced mate when several solutions exist.
- [ ] Add horizon-effect and quiescence regression positions.
- [ ] Test timeout followed immediately by another search.
- [ ] Test stop before any root evaluation completes.
- [ ] Test concurrent independent engine searches and cancellation isolation.
- [ ] Test repeated UCI searches, EOF, malformed input, and stale output suppression.
- [ ] Verify searching doesn't mutate the supplied position.

## 7. Collect repeatable metrics

- [ ] Count searched nodes across workers.
- [ ] Record elapsed time, completed depth, and nodes/second.
- [ ] Record completed/cancelled root evaluations and fallback frequency.
- [ ] Once caching exists, record cache probes, hits, and occupancy.
- [ ] Create a fixed benchmark set covering openings, middlegames, and endgames.
- [ ] Record engine revision, JVM, hardware, thread count, limits, and book settings.
- [ ] Separate JVM warm-up from measured runs.
- [ ] Run paired matches with colors reversed and consistent opening positions.
- [ ] Use fixed time per move after timed search is fixed; add standard clocks after UCI clock support.
- [ ] Estimate relative Elo with uncertainty from enough games; don't infer Elo from one game or nominal search depth.
- [ ] Keep performance benchmarks separate from playing-strength comparisons.

## 8. Packaging and usability

- [x] Build an executable shaded JAR.
- [x] Generate a launcher script that finds the JAR beside itself.
- [ ] Document the required Java version and supported UCI commands.
- [ ] Test the launcher from a different working directory and after moving both files.
- [ ] Add Windows launcher support if needed.
- [ ] Keep stdout reserved for UCI; put ordinary diagnostics on stderr.

## Suggested order

1. Iterative deepening and reliable stop results.
2. UCI clock controls and search statistics.
3. Perft verification and a reproducible benchmark baseline.
4. Alpha-beta and move ordering.
5. Quiescence and piece-square tables.
6. Transposition table and further evaluation improvements.

Checked items reflect the implementation discussed so far, not independent certification of the full engine.