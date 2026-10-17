# browser_interaction的 Python 脚本模块，入口及参数以 main/命令行解析和环境变量读取为准。
# 主要处理函数：click_with_pointer。

from selenium.webdriver.common.action_chains import ActionChains


def click_with_pointer(driver, element, delay_seconds=0.5):
    """Click through WebDriver pointer actions instead of JavaScript DOM events."""
    # 门户可能依赖鼠标事件链启动 SSO；先移动再点击，并给悬停状态留出处理时间。
    ActionChains(driver).move_to_element(element).pause(max(0, delay_seconds)).click().perform()
