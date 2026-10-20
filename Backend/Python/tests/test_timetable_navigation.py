# 课表的 Python 单元测试，测试替身用于隔离浏览器或外部服务。
# 主要处理函数：load_navigation_module。

import importlib.util
import unittest
from pathlib import Path


MODULE_PATH = Path(__file__).resolve().parents[1] / "timetable_navigation.py"


# 加载navigationmodule。
# 入参：test_case（testcase）。
# 返回：module 的当前值。
def load_navigation_module(test_case):
    test_case.assertTrue(MODULE_PATH.exists(), "timetable_navigation.py must exist")
    spec = importlib.util.spec_from_file_location("timetable_navigation", MODULE_PATH)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class FakeSwitchTo:
    # 初始化当前实例及其所需状态。
    # 入参：driver（驱动）。
    def __init__(self, driver):
        self.driver = driver

    # 窗口的处理逻辑。
    # 入参：handle（handle）。
    def window(self, handle):
        self.driver.current_handle = handle


class FakeDriver:
    # 初始化当前实例及其所需状态。
    # 入参：windows（窗口）；redirects（redirects）。
    def __init__(self, windows, redirects=None):
        self.windows = windows
        self.redirects = redirects or {}
        self.current_handle = next(iter(windows))
        self.switch_to = FakeSwitchTo(self)
        self.requested_urls = []

    # 窗口handles的处理逻辑。
    # 返回：由当前表达式或分支生成的处理结果。
    @property
    def window_handles(self):
        return list(self.windows)

    # 当前URL的处理逻辑。
    # 返回：由当前表达式或分支生成的处理结果。
    @property
    def current_url(self):
        value = self.windows[self.current_handle]
        if isinstance(value, list):
            current = value[0]
            if len(value) > 1:
                value.pop(0)
            return current
        return value

    # 获取当前操作。
    # 入参：url（URL）。
    def get(self, url):
        self.requested_urls.append(url)
        self.windows[self.current_handle] = self.redirects.get(url, url)


class TimetableNavigationTest(unittest.TestCase):
    def test_sso_waits_past_temporary_login_and_loading_pages(self):
        navigation = load_navigation_module(self)
        class SsoDriver(FakeDriver):
            titles = ["登录", "", "Loading...", "首页"]
            @property
            def title(self):
                self.last_title = self.titles.pop(0) if len(self.titles) > 1 else self.titles[0]
                return self.last_title
            @property
            def page_source(self):
                return '<form id="loginForm"></form>' if self.last_title == '登录' else '<html></html>'
        driver = SsoDriver({'sso': 'https://jw.hniu.cn/jsxsd/framework/xsMain.htmlx'})
        navigation.wait_for_target_window(driver, 'jw.hniu.cn', .1, poll_seconds=.001, require_authenticated=True)
        self.assertEqual('首页', driver.last_title)
        self.assertEqual([], driver.requested_urls)

    def test_persistent_login_times_out_instead_of_being_accepted(self):
        navigation = load_navigation_module(self)
        driver = FakeDriver({'sso': 'https://jw.hniu.cn/jsxsd/'})
        driver.title = '登录'
        driver.page_source = '<form id="loginForm"></form>'
        with self.assertRaises(navigation.AcademicAuthenticationRequired):
            navigation.wait_for_target_window(driver, 'jw.hniu.cn', .01, poll_seconds=.001, require_authenticated=True)

    def test_authenticated_wait_does_not_interrupt_atrust_redirect(self):
        navigation = load_navigation_module(self)
        driver = FakeDriver({'sso': ['https://atrust.hniu.cn/portal/', 'https://jw.hniu.cn/jsxsd/framework/xsMain.htmlx']})
        driver.title = '首页'
        driver.page_source = '<html></html>'
        navigation.wait_for_target_window(driver, 'jw.hniu.cn', .1, poll_seconds=.001,
                                          target_url='https://jw.hniu.cn/jsxsd/kbcx/kbxx_xzb', require_authenticated=True)
        self.assertEqual([], driver.requested_urls)

    def test_blank_target_page_is_not_authenticated(self):
        navigation = load_navigation_module(self)
        driver = FakeDriver({'sso': 'https://jw.hniu.cn/jsxsd/'})
        driver.title = ''
        driver.page_source = ''
        with self.assertRaises(navigation.TargetSystemUnavailable):
            navigation.wait_for_target_window(driver, 'jw.hniu.cn', .01, poll_seconds=.001, require_authenticated=True)

    # 验证detectscas身份登录页码。
    def test_detects_cas_identity_login_page(self):
        navigation = load_navigation_module(self)

        self.assertTrue(
            navigation.is_identity_login_page(
                "https://authserver.hniu.cn/authserver/login?service=example",
                "统一身份认证",
                '<form id="casLoginForm"></form>',
            )
        )

    # 验证doesnottreatportalas身份登录页码。
    def test_does_not_treat_portal_as_identity_login_page(self):
        navigation = load_navigation_module(self)

        self.assertFalse(
            navigation.is_identity_login_page(
                "https://ehall.hniu.cn/new/index.html",
                "网上办事服务大厅",
                '<div id="cardMyFavoriteContent"></div>',
            )
        )

    # 验证rejects教务登录页码evenon目标主机。
    def test_rejects_academic_login_page_even_on_target_host(self):
        navigation = load_navigation_module(self)

        with self.assertRaisesRegex(navigation.AcademicAuthenticationRequired, "登录页"):
            navigation.ensure_academic_authenticated(
                "https://jw.hniu.cn/jsxsd/kbcx/kbxx_xzb",
                "登录",
                '<form id="loginForm" action="/jsxsd/xk/LoginToXk"></form>',
            )

    # 验证accepts已认证状态教务页码。
    def test_accepts_authenticated_academic_page(self):
        navigation = load_navigation_module(self)

        navigation.ensure_academic_authenticated(
            "https://jw.hniu.cn/jsxsd/framework/xsMain.htmlx",
            "首页",
            '<div id="main">个人中心</div>',
        )

    # 验证已认证状态homepagewinsover登录引用进入来源。
    def test_authenticated_homepage_wins_over_login_reference_in_source(self):
        navigation = load_navigation_module(self)

        navigation.ensure_academic_authenticated(
            "https://jw.hniu.cn/jsxsd/framework/xsMain.htmlx",
            "首页",
            '<script>const fallback = "/jsxsd/xk/LoginToXk";</script>',
        )

    # 验证opens已认证状态portalapp条目beforewaitingfor目标。
    def test_opens_authenticated_portal_app_entry_before_waiting_for_target(self):
        navigation = load_navigation_module(self)
        entry_url = "https://ehall.hniu.cn/appShow?appId=6190757434612287"
        driver = FakeDriver(
            {"portal": "https://ehall.hniu.cn/new/index.html"},
            redirects={entry_url: [entry_url, "https://jw.hniu.cn/jsxsd/framework/xsMain.htmlx"]},
        )

        url = navigation.open_portal_app_and_wait(
            driver,
            entry_url,
            "jw.hniu.cn",
            timeout_seconds=0.02,
            poll_seconds=0.001,
        )

        self.assertEqual(
            entry_url,
            driver.requested_urls[0],
        )
        self.assertEqual(
            "https://jw.hniu.cn/jsxsd/framework/xsMain.htmlx",
            url,
        )

    # 验证finds目标窗口evenwhenitisnotthelatest。
    def test_finds_target_window_even_when_it_is_not_the_latest(self):
        navigation = load_navigation_module(self)
        driver = FakeDriver({
            "jw": "https://jw.hniu.cn/jsxsd/",
            "atrust": "https://atrust.hniu.cn/portal/#/common_info",
        })

        url = navigation.wait_for_target_window(driver, "jw.hniu.cn", timeout_seconds=0.02, poll_seconds=0.001)

        self.assertEqual("jw", driver.current_handle)
        self.assertEqual("https://jw.hniu.cn/jsxsd/", url)

    # 验证waitsforauthorizedatrustredirect目标reach目标。
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

    # 验证reportsatrustauthenticationrequirementafter超时。
    def test_reports_atrust_authentication_requirement_after_timeout(self):
        navigation = load_navigation_module(self)
        driver = FakeDriver({"atrust": "https://atrust.hniu.cn/portal/#/common_info"})

        with self.assertRaisesRegex(navigation.ATrustAuthenticationRequired, "aTrust"):
            navigation.wait_for_target_window(driver, "jw.hniu.cn", timeout_seconds=0.01, poll_seconds=0.001)

    # 验证重试次数目标URLonceafteratrustlosesoriginalredirect。
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
