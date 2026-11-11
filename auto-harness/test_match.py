"""Small regression tests for fixture validation and result accounting."""
import json
from pathlib import Path
import tempfile
import unittest
import match

class HarnessTests(unittest.TestCase):
    def test_default_openings_are_valid(self):
        for opening in match.DEFAULT_OPENINGS:
            self.assertTrue(match.opening_board(opening).is_valid())

    def test_illegal_opening_is_rejected(self):
        with self.assertRaises(ValueError):
            match.opening_board(['e2e5'])

    def test_unfinished_is_not_draw(self):
        config = {'engines': [{'name': 'A'}, {'name': 'B'}]}
        games = [
            {'white': 'A', 'black': 'B', 'result': '1-0', 'termination': 'checkmate'},
            {'white': 'B', 'black': 'A', 'result': '1-0', 'termination': 'checkmate'},
            {'white': 'A', 'black': 'B', 'result': '1/2-1/2', 'termination': 'stalemate'},
            {'white': 'A', 'black': 'B', 'result': '*', 'termination': 'ply limit'},
        ]
        with tempfile.TemporaryDirectory() as folder:
            summary = match.write_summary(config, games, Path(folder))
        self.assertEqual((1, 1, 1, 1), tuple(summary[k] for k in ('wins','draws','losses','unfinished')))
        self.assertEqual(0.5, summary['score_fraction'])

    def test_bad_control_is_rejected(self):
        config = {'engines': [{'name':'A','command':['a']}, {'name':'B','command':['b']}],
                  'control': {'mode':'clock','seconds':0}}
        with tempfile.TemporaryDirectory() as folder:
            path=Path(folder)/'config.json';path.write_text(json.dumps(config))
            with self.assertRaises(ValueError): match.load_config(path)

if __name__ == '__main__':
    unittest.main()
