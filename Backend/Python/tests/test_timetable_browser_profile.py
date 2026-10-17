# 课表的 Python 单元测试，测试替身用于隔离浏览器或外部服务。
# 主要处理函数：load_module。

import importlib.util
import unittest
from pathlib import Path


MODULE_PATH = Path(__file__).resolve().parents[1] / "timetable_browser_profile.py"


# 加载module。
# 入参：test_case（testcase）。
# 返回：module 的当前值。
def load_module(test_case):
    test_case.assertTrue(MODULE_PATH.exists(), "timetable_browser_profile.py must exist")
    spec = importlib.util.spec_from_file_location("timetable_browser_profile", MODULE_PATH)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class TimetableBrowserProfileTest(unittest.TestCase):
    # 验证unauthorizedvisibleprofilerequiresmanualbootstrap。
    def test_unauthorized_visible_profile_requires_manual_bootstrap(self):
        module = load_module(self)
        self.assertTrue(module.should_use_manual_bootstrap(False, False))

    # 验证authorizedprofiledoesnotrepeatmanualbootstrap。
    def test_authorized_profile_does_not_repeat_manual_bootstrap(self):
        module = load_module(self)
        self.assertFalse(module.should_use_manual_bootstrap(False, True))

    # 验证headless会话cannotusemanualbootstrap。
    def test_headless_session_cannot_use_manual_bootstrap(self):
        module = load_module(self)
        self.assertFalse(module.should_use_manual_bootstrap(True, False))

    # 验证firstrunisvisibleevenwhenheadlesswasrequested。
    def test_first_run_is_visible_even_when_headless_was_requested(self):
        module = load_module(self)
        self.assertFalse(module.should_run_headless(True, False))

    # 验证authorizedprofilehonorsheadless请求。
    def test_authorized_profile_honors_headless_request(self):
        module = load_module(self)
        self.assertTrue(module.should_run_headless(True, True))

    # 验证visible请求remainsvisible。
    def test_visible_request_remains_visible(self):
        module = load_module(self)
        self.assertFalse(module.should_run_headless(False, True))

    # 验证expiredheadless会话请求visiblereauthorization。
    def test_expired_headless_session_requests_visible_reauthorization(self):
        module = load_module(self)
        self.assertTrue(module.should_retry_with_visible_authorization(True, False))
        self.assertFalse(module.should_retry_with_visible_authorization(False, False))
        self.assertFalse(module.should_retry_with_visible_authorization(True, True))

    # 验证crawlerrestarts进入samerunafter会话expiry。
    def test_crawler_restarts_in_same_run_after_session_expiry(self):
        source = (MODULE_PATH.parent / "crawl_school_timetable.py").read_text(encoding="utf-8")
        self.assertIn("except (AcademicSessionRefreshRequired, TargetSystemUnavailable)", source)
        self.assertIn("driver = create_driver()", source)
        self.assertIn("create_driver(automatic_visible_authorization=True)", source)
        self.assertIn("可视自动续权未成功，已切换为人工点击兜底", source)
        self.assertIn("无需重新执行任务", source)


if __name__ == "__main__":
    unittest.main()
