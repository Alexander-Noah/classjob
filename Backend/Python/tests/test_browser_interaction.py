# test_browser_interaction的 Python 单元测试，测试替身用于隔离浏览器或外部服务。
# 主要处理函数：load_module。

import importlib.util
import unittest
from pathlib import Path


MODULE_PATH = Path(__file__).resolve().parents[1] / "browser_interaction.py"


# 加载module。
# 入参：test_case（testcase）。
# 返回：module 的当前值。
def load_module(test_case):
    test_case.assertTrue(MODULE_PATH.exists(), "browser_interaction.py must exist")
    spec = importlib.util.spec_from_file_location("browser_interaction", MODULE_PATH)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class FakeActions:
    # 初始化当前实例及其所需状态。
    def __init__(self):
        self.calls = []

    # 移动目标element的处理逻辑。
    # 入参：element（element）。
    # 返回：self 的当前值。
    def move_to_element(self, element):
        self.calls.append(("move", element))
        return self

    # pause的处理逻辑。
    # 入参：seconds（seconds）。
    # 返回：self 的当前值。
    def pause(self, seconds):
        self.calls.append(("pause", seconds))
        return self

    # click的处理逻辑。
    # 返回：self 的当前值。
    def click(self):
        self.calls.append(("click",))
        return self

    # perform的处理逻辑。
    def perform(self):
        self.calls.append(("perform",))


class BrowserInteractionTest(unittest.TestCase):
    # 验证clickwith指针usesnativeactionchain。
    def test_click_with_pointer_uses_native_action_chain(self):
        module = load_module(self)
        actions = FakeActions()
        # 处理当前调用传入的回调逻辑。
        # 入参：driver。
        module.ActionChains = lambda driver: actions
        element = object()

        module.click_with_pointer(object(), element, delay_seconds=0.25)

        self.assertEqual(
            [("move", element), ("pause", 0.25), ("click",), ("perform",)],
            actions.calls,
        )


if __name__ == "__main__":
    unittest.main()
