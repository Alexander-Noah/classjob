# 课表的 Python 单元测试，测试替身用于隔离浏览器或外部服务。

"""Exercise main's recovery branch without opening a browser or contacting school."""
import ast
import importlib.util
from pathlib import Path
import unittest
from unittest.mock import Mock


ROOT = Path(__file__).resolve().parents[1]


class TimetableRecoveryTest(unittest.TestCase):
    def run_main(self, headless):
        spec = importlib.util.spec_from_file_location("profile", ROOT / "timetable_browser_profile.py")
        profile = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(profile)
        class NavigationTimeout(RuntimeError):
            pass
        driver = Mock()
        driver._timetable_headless = headless
        recovered_driver = Mock()
        create = Mock(side_effect=[driver, recovered_driver])
        login = Mock(side_effect=[NavigationTimeout("portal only"), RuntimeError("fixture stop")])
        marker = Mock()
        namespace = dict(
            USERNAME="fixture", PASSWORD="fixture", SEMESTER="fixture",
            create_driver=create, login_portal_and_enter_jw=login,
            AcademicSessionRefreshRequired=profile.AcademicSessionRefreshRequired,
            TargetSystemUnavailable=NavigationTimeout,
            should_retry_with_visible_authorization=profile.should_retry_with_visible_authorization,
            PROFILE_AUTH_MARKER=marker, print=Mock(),
        )
        module = ast.parse((ROOT / "crawl_school_timetable.py").read_text(encoding="utf-8"))
        main = next(node for node in module.body if isinstance(node, ast.FunctionDef) and node.name == "main")
        exec(compile(ast.Module(body=[main], type_ignores=[]), "crawler-main", "exec"), namespace)
        with self.assertRaises(RuntimeError):
            namespace["main"]()
        return create, login, marker, driver, recovered_driver

    def test_portal_timeout_in_headless_mode_attempts_visible_recovery_once(self):
        create, login, marker, first, second = self.run_main(True)
        self.assertEqual(2, create.call_count)
        create.assert_called_with(automatic_visible_authorization=True)
        marker.unlink.assert_called_once_with(missing_ok=True)
        self.assertEqual(2, login.call_count)
        first.quit.assert_called_once()
        second.quit.assert_called_once()

    def test_visible_timeout_is_terminal_without_another_browser(self):
        create, login, marker, first, _ = self.run_main(False)
        create.assert_called_once_with()
        login.assert_called_once_with(first)
        marker.unlink.assert_not_called()
        first.quit.assert_called_once()


if __name__ == "__main__":
    unittest.main()
