#!/usr/bin/env python3
"""Play paired UCI matches between two engine versions."""
import argparse
import collections
import concurrent.futures
import datetime
import hashlib
import json
import logging
import math
from pathlib import Path
import platform
import time

import chess
import chess.engine
import chess.pgn

DEFAULT_OPENINGS = [
    [],
    ['e2e4', 'e7e5', 'g1f3', 'b8c6'],
    ['d2d4', 'd7d5', 'c2c4', 'e7e6'],
    ['e2e4', 'c7c5', 'g1f3', 'd7d6'],
    ['d2d4', 'g8f6', 'c2c4', 'e7e6'],
    ['c2c4', 'e7e5', 'b1c3', 'g8f6'],
]


def load_config(path):
    config = json.loads(path.read_text())
    engines = config['engines']
    if len(engines) != 2:
        raise ValueError('Exactly two engines are required')
    if len({engine['name'] for engine in engines}) != 2:
        raise ValueError('Engine names must differ')
    for engine in engines:
        if not isinstance(engine['command'], list) or not engine['command']:
            raise ValueError('command must be a nonempty JSON array of arguments')
        if not all(isinstance(arg, str) and arg for arg in engine['command']):
            raise ValueError('Every command argument must be a nonempty string')
    control = config.get('control', {'mode': 'movetime', 'milliseconds': 250})
    mode = control['mode']
    if mode == 'movetime':
        require_positive(control['milliseconds'], 'milliseconds')
    elif mode == 'depth':
        require_positive(control['depth'], 'depth', integer=True)
    elif mode == 'clock':
        require_positive(control['seconds'], 'seconds')
        require_positive(control.get('increment', 0), 'increment', allow_zero=True)
    else:
        raise ValueError('Control mode must be movetime, depth, or clock')
    config['control'] = control
    require_positive(config.get('pairs', 10), 'pairs', integer=True)
    require_positive(config.get('max_plies', 400), 'max_plies', integer=True)
    require_positive(config.get('response_timeout_seconds', 30), 'response timeout')
    require_positive(config.get('grace_seconds', 0.5), 'grace', allow_zero=True)
    for opening in config.get('openings', DEFAULT_OPENINGS):
        opening_board(opening)
    if not config.get('openings', DEFAULT_OPENINGS):
        raise ValueError('At least one opening is required')
    return config


def require_positive(value, name, integer=False, allow_zero=False):
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        raise ValueError(f'{name} must be numeric')
    if not math.isfinite(value) or value < 0 or (value == 0 and not allow_zero):
        raise ValueError(f'Invalid {name}')
    if integer and not isinstance(value, int):
        raise ValueError(f'{name} must be an integer')


def opening_board(opening):
    # A FEN allows tactical/endgame fixtures. A move list preserves history.
    if isinstance(opening, str):
        board = chess.Board(opening)
    elif isinstance(opening, list):
        board = chess.Board()
        for move in opening:
            board.push_uci(move)
    else:
        raise ValueError('Each opening must be a UCI move array or a full FEN')
    if not board.is_valid() or board.is_game_over(claim_draw=True):
        raise ValueError('Opening must be a valid, nonterminal position')
    return board


def close_engine(engine):
    # Bound cleanup even when an engine ignores quit.
    try:
        engine.timeout = 2
        engine.quit()
    except Exception:
        pass
    finally:
        engine.close()


def build_limit(control, clocks, config):
    if control['mode'] == 'movetime':
        seconds = control['milliseconds'] / 1000
        return chess.engine.Limit(time=seconds), seconds + config.get('grace_seconds', 0.5)
    if control['mode'] == 'depth':
        return chess.engine.Limit(depth=control['depth']), config.get('response_timeout_seconds', 30)
    return chess.engine.Limit(
        white_clock=clocks[chess.WHITE], black_clock=clocks[chess.BLACK],
        white_inc=control.get('increment', 0), black_inc=control.get('increment', 0)), None


def play_game(config, white, black, opening, game_number, output):
    board = opening_board(opening)
    game = chess.pgn.Game.from_board(board)
    game.headers.update({
        'Event': 'Regis version comparison', 'Site': 'local',
        'Date': datetime.date.today().strftime('%Y.%m.%d'),
        'Round': str(game_number), 'White': white['name'], 'Black': black['name'],
    })
    control = config['control']
    if control['mode'] == 'clock':
        game.headers['TimeControl'] = f"{control['seconds']}+{control.get('increment', 0)}"
    elif control['mode'] == 'depth':
        game.headers['TimeControl'] = '-'
        game.headers['SearchDepth'] = str(control['depth'])
    else:
        game.headers['TimeControl'] = f"1/{control['milliseconds'] / 1000}"
    node = game.end()
    record = {'game': game_number, 'white': white['name'], 'black': black['name'],
              'opening': opening, 'result': '*', 'termination': '', 'error': None,
              'moves': 0, 'wall_seconds': 0}
    engines = {}
    clocks = {chess.WHITE: control.get('seconds', 0), chess.BLACK: control.get('seconds', 0)}
    started = time.monotonic()
    game_id = object()
    max_plies = config.get('max_plies', 400)
    try:
        for color, spec in [(chess.WHITE, white), (chess.BLACK, black)]:
            # Fresh processes each game avoid stale state between versions/games.
            engine = chess.engine.SimpleEngine.popen_uci(
                spec['command'], timeout=config.get('response_timeout_seconds', 30),
                cwd=spec.get('cwd'))
            engines[color] = engine
            if spec.get('options'):
                engine.configure(spec['options'])
        while record['moves'] < max_plies:
            outcome = board.outcome(claim_draw=True)
            if outcome:
                record['result'] = outcome.result()
                record['termination'] = outcome.termination.name.lower()
                break
            color = board.turn
            spec = white if color else black
            limit, allowance = build_limit(control, clocks, config)
            if allowance is None:
                allowance = clocks[color] + config.get('grace_seconds', 0.5)
            engine = engines[color]
            # python-chess adds limit.time to its own timeout for movetime searches.
            engine.timeout = (config.get('grace_seconds', 0.5)
                              if control['mode'] == 'movetime' else allowance)
            move_started = time.monotonic()
            failure = None
            result = None
            try:
                result = engine.play(board, limit, game=game_id, info=chess.engine.INFO_ALL)
            except (TimeoutError, concurrent.futures.TimeoutError):
                failure = 'response timeout'
            except (chess.engine.EngineError, chess.engine.EngineTerminatedError) as error:
                failure = 'engine/protocol failure'
                record['error'] = str(error)
            elapsed = time.monotonic() - move_started
            if failure is None and elapsed > allowance:
                failure = 'time forfeit'
            if failure is None and (result.move is None or result.move not in board.legal_moves):
                failure = 'missing/illegal bestmove'
            metrics = {'game': game_number, 'ply': board.ply() + 1,
                       'engine': spec['name'], 'elapsed_seconds': elapsed,
                       'move': result.move.uci() if result and result.move else None,
                       'failure': failure}
            if result:
                for key in ('depth', 'seldepth', 'nodes', 'nps', 'time', 'string'):
                    if key in result.info:
                        metrics[key] = result.info[key]
            with (output / 'moves.jsonl').open('a') as file:
                file.write(json.dumps(metrics) + '\n')
            if failure:
                record['result'] = '0-1' if color else '1-0'
                record['termination'] = failure
                break
            if control['mode'] == 'clock':
                clocks[color] = max(0, clocks[color] - elapsed) + control.get('increment', 0)
            board.push(result.move)
            node = node.add_variation(result.move)
            node.comment = f'{elapsed:.3f}s'
            record['moves'] += 1
        if not record['termination']:
            outcome = board.outcome(claim_draw=True)
            record['result'] = outcome.result() if outcome else '*'
            record['termination'] = outcome.termination.name.lower() if outcome else 'ply limit (unscored)'
    finally:
        for engine in engines.values():
            close_engine(engine)
    record['wall_seconds'] = time.monotonic() - started
    record['final_fen'] = board.fen()
    game.headers['Result'] = record['result']
    game.headers['Termination'] = record['termination']
    with (output / 'games.pgn').open('a') as file:
        print(game, file=file, end='\n\n')
    return record


def write_summary(config, games, output):
    candidate = config['engines'][0]['name']
    tally = collections.Counter()
    for game in games:
        if game['result'] == '*':
            tally['unfinished'] += 1
        elif game['result'] == '1/2-1/2':
            tally['draws'] += 1
        else:
            winner = game['white'] if game['result'] == '1-0' else game['black']
            tally['wins' if winner == candidate else 'losses'] += 1
    scored = tally['wins'] + tally['draws'] + tally['losses']
    summary = {'candidate': candidate, 'baseline': config['engines'][1]['name'],
               'games': len(games), 'wins': tally['wins'], 'draws': tally['draws'],
               'losses': tally['losses'], 'unfinished': tally['unfinished'],
               'score_fraction': (tally['wins'] + tally['draws'] / 2) / scored if scored else None,
               'terminations': dict(collections.Counter(g['termination'] for g in games))}
    # Replace atomically so interruption can't leave a half-written summary.
    temporary = output / 'summary.tmp'
    temporary.write_text(json.dumps(summary, indent=2) + '\n')
    temporary.replace(output / 'summary.json')
    return summary


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('config', type=Path)
    parser.add_argument('--out', type=Path, required=True)
    args = parser.parse_args()
    config = load_config(args.config)
    # Require a new directory to avoid mixing runs accidentally.
    args.out.mkdir(parents=True, exist_ok=False)
    logging.basicConfig(filename=args.out / 'uci.log', level=logging.DEBUG,
                        format='%(asctime)s %(levelname)s %(message)s')
    metadata = {'config': config, 'python': platform.python_version(),
                'platform': platform.platform(), 'chess_version': chess.__version__,
                'started_utc': datetime.datetime.now(datetime.timezone.utc).isoformat(),
                'file_sha256': {}}
    for spec in config['engines']:
        for argument in spec['command']:
            path = Path(argument)
            if not path.is_absolute() and spec.get('cwd'):
                path = Path(spec['cwd']) / path
            if path.is_file():
                metadata['file_sha256'][str(path.resolve())] = hashlib.sha256(path.read_bytes()).hexdigest()
    (args.out / 'run.json').write_text(json.dumps(metadata, indent=2) + '\n')
    games = []
    openings = config.get('openings', DEFAULT_OPENINGS)
    try:
        for pair in range(config.get('pairs', 10)):
            opening = openings[pair % len(openings)]
            for reverse in (False, True):
                white, black = config['engines'][::(-1 if reverse else 1)]
                record = play_game(config, white, black, opening, len(games) + 1, args.out)
                record['pair'] = pair + 1
                games.append(record)
                with (args.out / 'games.jsonl').open('a') as file:
                    file.write(json.dumps(record) + '\n')
                summary = write_summary(config, games, args.out)
                print(f"Game {record['game']}: {white['name']} vs {black['name']} "
                      f"{record['result']} ({record['termination']})", flush=True)
                print(f"Candidate W/D/L: {summary['wins']}/{summary['draws']}/{summary['losses']}", flush=True)
    finally:
        write_summary(config, games, args.out)


if __name__ == '__main__':
    main()
