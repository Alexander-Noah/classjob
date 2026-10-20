"""首次可视授权必须主动导航，不能只等待人工点击。"""
import ast
from pathlib import Path
from types import SimpleNamespace
import unittest
from unittest.mock import Mock
from urllib.parse import urlparse


class FirstNavigationTest(unittest.TestCase):
    def test_first_profile_clicks_app_and_service_before_waiting(self):
        source = (Path(__file__).resolve().parents[1] / 'crawl_school_timetable.py').read_text(encoding='utf-8')
        function = next(n for n in ast.parse(source).body if isinstance(n, ast.FunctionDef) and n.name == 'login_portal_and_enter_jw')
        calls = []
        driver = SimpleNamespace(
            _timetable_headless=False, current_url='https://ehall.example/',
            title='门户', page_source='', get=Mock(), maximize_window=Mock(),
        )
        marker = Mock()
        marker.exists.return_value = False
        config = SimpleNamespace(portal_url='https://ehall.example/', portal_app_id='app', class_timetable_url='https://jw.example/query')
        namespace = dict(
            ACADEMIC_SYSTEM=config, HEADLESS=True, PROFILE_AUTH_MARKER=marker,
            NAVIGATION_TIMEOUT_SECONDS=90,
            time=SimpleNamespace(sleep=Mock()), print=Mock(),
            urllib=SimpleNamespace(parse=SimpleNamespace(urlparse=urlparse)),
            is_identity_login_page=lambda *args: False,
            get_visible_portal_login_button=lambda d: None,
            wait_for_portal_authentication=Mock(),
            should_use_manual_bootstrap=lambda headless, authorized: not headless and not authorized,
            click_portal_app=lambda *args: calls.append('app'),
            click_portal_enter_service=lambda *args, **kwargs: calls.append('service'),
            wait_for_target_window=lambda *args, **kwargs: calls.append('wait'),
            ensure_academic_authenticated=Mock(),
        )
        exec(compile(ast.Module(body=[function], type_ignores=[]), 'first-navigation', 'exec'), namespace)
        namespace['login_portal_and_enter_jw'](driver)
        self.assertEqual(['app', 'service', 'wait'], calls)
        marker.write_text.assert_called_once()


if __name__ == '__main__':
    unittest.main()
