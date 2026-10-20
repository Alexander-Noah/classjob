"""离线测试：不连接学校系统，不读取 Cookie/账号或个人课表。"""
import ast
import pathlib
import types
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
SOURCE = (ROOT / "crawl_school_timetable.py").read_text(encoding="utf-8")
SELECTED = [
    "get_visible_portal_login_button",
    "portal_login_required",
    "is_portal_authenticated_for_navigation",
    "wait_for_portal_authentication",
    "click_portal_app",
]
MODULE = ast.parse(SOURCE)
PART = ast.Module(body=[node for node in MODULE.body if isinstance(node, ast.FunctionDef) and node.name in SELECTED], type_ignores=[])

class Element:
    def __init__(self, displayed=True):
        self.displayed = displayed
    text = '登录'
    def is_displayed(self):
        return self.displayed
    def get_attribute(self, name):
        return None

class Driver:
    _timetable_headless = False
    current_url = 'https://ehall.example/'
    title = '网上办事大厅'
    page_source = '<html></html>'
    def __init__(self, logged_out):
        self.logged_out = logged_out
    def find_elements(self, by, selector):
        if by == 'id' and selector == 'ampLoginBtn':
            return [Element(True)] if self.logged_out else []
        return []

class FakeWait:
    def __init__(self, d, timeout):
        self.driver = d
    def until(self, fn):
        for _ in range(6):
            result = fn(self.driver)
            if result:
                return result
        raise TimeoutError('not ready')

class RefreshRequired(RuntimeError):
    pass

TICKS = iter(range(0, 100))
ENV = {
    'By': types.SimpleNamespace(ID='id', XPATH='xpath'),
    'WebDriverWait': FakeWait,
    'AcademicSessionRefreshRequired': RefreshRequired,
    'ACADEMIC_SYSTEM': types.SimpleNamespace(portal_app_id='app'),
    'HEADLESS': True,
    'time': types.SimpleNamespace(monotonic=lambda: next(TICKS)),
    'is_identity_login_page': lambda *_: False,
    'portal_app_is_visible': lambda driver, app: True,
}
exec(compile(PART, '<selected code under test>', 'exec'), ENV)

class PortalLoginGuardTests(unittest.TestCase):
    def test_visible_login_button_blocks_authenticated_state(self):
        self.assertFalse(ENV['is_portal_authenticated_for_navigation'](Driver(True)))
    def test_portal_app_alone_not_proof_of_login(self):
        driver = Driver(True)
        self.assertTrue(ENV['portal_login_required'](driver))
        with self.assertRaises(RefreshRequired):
            ENV['click_portal_app'](driver, 'app')
    def test_authenticated_state_can_proceed(self):
        d = Driver(False)
        self.assertTrue(ENV['is_portal_authenticated_for_navigation'](d))
        ENV['wait_for_portal_authentication'](d, 30)
    def test_logged_out_headless_returns_refresh_required(self):
        d = Driver(True)
        d._timetable_headless = True
        with self.assertRaises(RefreshRequired):
            ENV['wait_for_portal_authentication'](d, 30)

if __name__ == '__main__':
    unittest.main()
