import importlib.util
import unittest
from pathlib import Path


MODULE_PATH = Path(__file__).resolve().parents[1] / "timetable_browser_profile.py"


def load_module(test_case):
    test_case.assertTrue(MODULE_PATH.exists(), "timetable_browser_profile.py must exist")
    spec = importlib.util.spec_from_file_location("timetable_browser_profile", MODULE_PATH)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class TimetableBrowserProfileTest(unittest.TestCase):
    def test_first_run_is_visible_even_when_headless_was_requested(self):
        module = load_module(self)
        self.assertFalse(module.should_run_headless(True, False))

    def test_authorized_profile_honors_headless_request(self):
        module = load_module(self)
        self.assertTrue(module.should_run_headless(True, True))

    def test_visible_request_remains_visible(self):
        module = load_module(self)
        self.assertFalse(module.should_run_headless(False, True))


if __name__ == "__main__":
    unittest.main()
