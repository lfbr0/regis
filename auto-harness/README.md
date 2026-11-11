# Regis version match harness

Run two executable builds through UCI and compare their game results. Python 3.10+ and python-chess are required. Java is required when testing Regis JARs.

## Setup

```bash
python3 -m venv .venv
. .venv/bin/activate
pip install -r requirements.txt
```

Keep separate builds of the baseline and candidate. Update `match.json` with their absolute paths. `command` is an argument array executed directly, without a shell. A launcher script can be supplied as a single array element instead. Add an absolute `cwd` per engine if its resources require a working directory. Optional `options` supplies only UCI options advertised by that engine.

```bash
python match.py match.json --out runs/candidate-vs-baseline-001
```

The output directory must be new. Run from the same working directory on repeat comparisons, or use absolute paths throughout. `pairs: 10` plays 20 games. The candidate is the first configured engine.

## What a run does

- Plays each opening twice with colors swapped.
- Starts fresh engine processes for each game.
- Uses independent legal-move validation and game-result detection.
- Defaults to six short opening sequences, cycling through them by pair. These are prescribed starting positions and aren't an automatically played book.
- Treats response failures, illegal moves, missing moves, and crashes during play as forfeits.
- Aborts on engine startup/configuration errors. Check `uci.log` and fix the setup before rerunning.
- Treats a ply-limit finish as unscored, preserving `*` in the PGN.
- Automatically claims available threefold/50-move draws, including claims available through an intended move.
- Runs one game at a time to reduce resource contention. Both engines can still keep their own internal worker pools.

The default configuration uses 250 milliseconds per move and a 500 millisecond response grace. Grace tolerates process scheduling and protocol overhead. For clock games, elapsed wall time is deducted and the increment is added after a legal move. A response beyond the player's remaining clock plus grace forfeits. This is a testing policy, so keep it identical across comparisons.

## Controls

Fixed time per move, in milliseconds:

```json
"control": {"mode": "movetime", "milliseconds": 250}
```

Fixed depth with a response watchdog:

```json
"control": {"mode": "depth", "depth": 3},
"response_timeout_seconds": 30
```

Whole-game clock, in seconds, with per-move increment:

```json
"control": {"mode": "clock", "seconds": 60, "increment": 0.5}
```

Clock mode sends `wtime`, `btime`, `winc`, and `binc`. It uses sudden-death clocks and doesn't currently schedule periodic `movestogo` time controls. The default `movetime` mode works with older versions that lack clock support.

## Starting positions

Override defaults using UCI sequences or full FEN strings. Every supplied position must be legal and nonterminal.

```json
"openings": [
  [],
  ["e2e4", "e7e5", "g1f3", "b8c6"],
  ["d2d4", "d7d5", "c2c4", "e7e6"],
  "7k/5K2/6Q1/8/8/8/8/8 w - - 0 1"
]
```

An empty array starts at the initial position. Move sequences retain repetition history. FEN fixtures only supply history from that position onward. The same opening index is used for both games of a pair.

## Results

- `games.pgn` contains played games, move times, and termination reasons.
- `games.jsonl` contains a completed-game record per line.
- `moves.jsonl` contains move elapsed time and reported depth/nodes/NPS where supplied.
- `summary.json` contains candidate W/D/L, score fraction, unfinished games, and termination counts. It updates after each game.
- `run.json` saves resolved settings, environment versions, and SHA-256 hashes of command arguments that identify local files, including directly specified JARs.
- `uci.log` records protocol traffic and engine stderr, including Regis fallback messages.

If using a shell launcher, its hash is recorded but its referenced JAR isn't discovered automatically. Prefer direct `java -jar` commands to fingerprint the engine artifact.

Compare playing strength using equal time controls and hardware. Depth tests are useful for search regressions but don't give equal computing budgets across versions. Report unscored games and failure counts with W/D/L. The harness doesn't produce an Elo estimate or confidence interval yet. Deterministic engines can repeat games, so expand the opening set before interpreting results as statistical evidence.

## Checks

```bash
python -m unittest discover -v
```

The harness was checked with small synthetic UCI engines for normal play under all three controls, illegal moves, process crashes, and response timeouts. It hasn't been run against your current Java builds.

## References

- python-chess engine communication: https://python-chess.readthedocs.io/en/latest/engine.html
- python-chess game outcomes: https://python-chess.readthedocs.io/en/latest/core.html
