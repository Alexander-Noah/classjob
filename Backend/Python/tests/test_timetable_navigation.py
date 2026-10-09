import importlib.util
import unittest
from pathlib import Path


MODULE_PATH = Path(__file__).resolve().parents[1] / "timetable_navigation.py"


def load_navigation_module(test_case):
    test_case.assertTrue(MODULE_PATH.exists(), "timetable_navigation.py must exist")
    spec = importlib.util.spec_from_file_location("timetable_navigation", MODULE_PATH)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class FakeSwitchTo:
    def __init__(self, driver):
        self.driver = driver

    def window(self, handle):
        self.driver.current_handle = handle


class FakeDriver:
    def __init__(self, windows):
        self.windows = windows
        self.current_handle = next(iter(windows))
        self.switch_to = FakeSwitchTo(self)

    @property
    def window_handles(self):
        return list(self.windows)

    @property
    def current_url(self):
        value = self.windows[self.current_handle]
        if isinstance(value, list):
            current = value[0]
            if len(value) > 1:
                value.pop(0)
            return current
        return value

    def get(self, url):
        self.windows[self.current_handle] = url


class TimetableNavigationTest(unittest.TestCase):
    def test_finds_target_window_even_when_it_is_not_the_latest(self):
        navigation = load_navigation_module(self)
        driver = FakeDriver({
            "jw": "https://jw.hniu.cn/jsxsd/",
            "atrust": "https://atrust.hniu.cn/portal/#/common_info",
        })

        url = navigation.wait_for_target_window(driver, "jw.hniu.cn", timeout_seconds=0.02, poll_seconds=0.001)

        self.assertEqual("jw", driver.current_handle)
        self.assertEqual("https://jw.hniu.cn/jsxsd/", url)

    def test_waits_for_authorized_atrust_redirect_to_reach_target(self):
        navigation = load_navigation_module(self)
        driver = FakeDriver({
            "portal": [
                "https://atrust.hniu.cn/portal/#/common_info",
                "https://jw.hniu.cn/jsxsd/",
            ],
        })

        url = navigation.wait_for_target_window(driver, "jw.hniu.cn", timeout_seconds=0.05, poll_seconds=0.001)

        self.assertEqual("https://jw.hniu.cn/jsxsd/", url)

    def test_reports_atrust_authentication_requirement_after_timeout(self):
        navigation = load_navigation_module(self)
        driver = FakeDriver({"atrust": "https://atrust.hniu.cn/portal/#/common_info"})

        with self.assertRaisesRegex(navigation.ATrustAuthenticationRequired, "aTrust"):
            navigation.wait_for_target_window(driver, "jw.hniu.cn", timeout_seconds=0.01, poll_seconds=0.001)

    def test_retries_target_url_once_after_atrust_loses_original_redirect(self):
        navigation = load_navigation_module(self)
        driver = FakeDriver({"atrust": "https://atrust.hniu.cn/portal/#/common_info"})

        url = navigation.wait_for_target_window(
            driver,
            "jw.hniu.cn",
            timeout_seconds=0.05,
            poll_seconds=0.001,
            target_url="https://jw.hniu.cn/jsxsd/kbcx/kbxx_xzb",
        )

        self.assertEqual("https://jw.hniu.cn/jsxsd/kbcx/kbxx_xzb", url)


if __name__ == "__main__":
    unittest.main()
