from selenium.webdriver.common.action_chains import ActionChains


def click_with_pointer(driver, element, delay_seconds=0.5):
    """Click through WebDriver pointer actions instead of JavaScript DOM events."""
    ActionChains(driver).move_to_element(element).pause(max(0, delay_seconds)).click().perform()
