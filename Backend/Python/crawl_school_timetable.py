# 后端调度的课表采集入口：读取环境变量，建立门户 SSO 会话，导出供 Java 导入的文件。
# 不直接操作数据库；学校页面、账号和浏览器目录由配置注入。
from selenium import webdriver
from selenium.webdriver.edge.options import Options as EdgeOptions
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC

from bs4 import BeautifulSoup
from io import StringIO
from pathlib import Path

import pandas as pd
import time
import re
import os
import urllib.parse
import urllib.request

from timetable_navigation import (
    AcademicAuthenticationRequired,
    TargetSystemUnavailable,
    ensure_academic_authenticated,
    is_identity_login_page,
    wait_for_target_window,
)
from timetable_browser_profile import (
    AcademicSessionRefreshRequired,
    should_retry_with_visible_authorization,
    should_run_headless,
    should_use_manual_bootstrap,
)
from browser_interaction import click_with_pointer
from timetable_semester import resolve_semester
from academic_system_config import load_academic_system_config


# =========================
# 基础配置
# =========================

ACADEMIC_SYSTEM = load_academic_system_config()

USERNAME = os.environ.get("TIMETABLE_USERNAME", "").strip()
PASSWORD = os.environ.get("TIMETABLE_PASSWORD", "").strip()
HEADLESS = os.environ.get("TIMETABLE_HEADLESS", "true").strip().lower() != "false"
SEMESTER = os.environ.get("TIMETABLE_SEMESTER", "").strip()
NAVIGATION_TIMEOUT_SECONDS = float(os.environ.get("TIMETABLE_NAVIGATION_TIMEOUT_SECONDS", "90"))
PORTAL_INTERACTION_DELAY_SECONDS = float(os.environ.get("TIMETABLE_PORTAL_INTERACTION_DELAY_SECONDS", "2"))
BROWSER_PROFILE_DIR = Path(
    os.environ.get(
        "TIMETABLE_BROWSER_PROFILE_DIR",
        str(Path(__file__).resolve().parent / ".timetable-browser-profile"),
    )
).resolve()
# 此标记只代表曾完成授权，不保证 Cookie 仍有效；失效时需要重新建立会话。
PROFILE_AUTH_MARKER = BROWSER_PROFILE_DIR / ".academic-session-initialized"

# 导出文件夹：当前 Python 文件同目录下的“课表导出结果”
OUTPUT_DIR = Path(__file__).resolve().parent / "课表导出结果"
OUTPUT_DIR.mkdir(parents=True, exist_ok=True)


# =========================
# 工具函数
# =========================

def clean_text(text):
    if text is None:
        return ""
    text = str(text)
    text = text.replace("\xa0", " ")
    text = re.sub(r"\s+", " ", text)
    return text.strip()


# 添加utf8meta。
# 入参：html（HTML）。
# 返回：由当前表达式或分支生成的处理结果。
def add_utf8_meta(html):
    if not html:
        return html

    if "charset" in html.lower():
        return html

    return re.sub(
        r"(<head[^>]*>)",
        r'\1\n<meta charset="UTF-8">',
        html,
        count=1,
        flags=re.IGNORECASE
    )


# 保存HTML。
# 入参：filename（filename）；html（HTML）。
def save_html(filename, html):
    file_path = OUTPUT_DIR / filename

    html = add_utf8_meta(html)

    with open(file_path, "w", encoding="utf-8", newline="") as f:
        f.write(html)

    print("已保存：", file_path)


# 保存文本。
# 入参：filename（filename）；html（HTML）。
def save_text(filename, html):
    file_path = OUTPUT_DIR / filename

    soup = BeautifulSoup(html, "lxml")
    text = soup.get_text("\n")

    lines = []
    for line in text.splitlines():
        line = clean_text(line)
        if line:
            lines.append(line)

    with open(file_path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))

    print("已保存：", file_path)


def js_click(driver, element):
    """
    用 JS 点击，解决 element not interactable
    """
    driver.execute_script("arguments[0].click();", element)


# 等待elementby标识。
# 入参：driver（驱动）；element_id（element标识）；timeout（超时）。
# 返回：由当前表达式或分支生成的处理结果。
def wait_element_by_id(driver, element_id, timeout=20):
    return WebDriverWait(driver, timeout).until(
        EC.presence_of_element_located((By.ID, element_id))
    )


# 等待elementbyxpath。
# 入参：driver（驱动）；xpath（xpath）；timeout（超时）。
# 返回：由当前表达式或分支生成的处理结果。
def wait_element_by_xpath(driver, xpath, timeout=20):
    return WebDriverWait(driver, timeout).until(
        EC.presence_of_element_located((By.XPATH, xpath))
    )


# 等待输入by标识。
# 入参：driver（驱动）；element_id（element标识）；value（值）；timeout（超时）。
# 返回：element 的当前值。
def wait_input_by_id(driver, element_id, value, timeout=20):
    # 查找visible输入。
    # 入参：current_driver（当前驱动）。
    # 返回：由当前表达式或分支生成的处理结果。
    def find_visible_input(current_driver):
        return next(
            (element for element in current_driver.find_elements(By.ID, element_id) if element.is_displayed()),
            False,
        )

    element = WebDriverWait(driver, timeout).until(find_visible_input)
    element.clear()
    element.send_keys(value)
    return element


def click_visible_element_by_id(driver, element_id, timeout=20):
    """
    页面里可能有多个同 ID 元素，有些是隐藏的。
    这里找可见元素再点击。
    """
    WebDriverWait(driver, timeout).until(
        # 处理当前调用传入的回调逻辑。
        # 入参：d。
        # 处理当前调用传入的回调逻辑。
        lambda d: len(d.find_elements(By.ID, element_id)) > 0
    )

    elements = driver.find_elements(By.ID, element_id)

    for element in elements:
        if element.is_displayed():
            driver.execute_script("arguments[0].scrollIntoView(true);", element)
            time.sleep(0.5)
            js_click(driver, element)
            return True

    raise Exception(f"没有找到可见元素：{element_id}")


# clickportalapp的处理逻辑。
# 入参：driver（驱动）；app_id（app标识）；timeout（超时）。
def click_portal_app(driver, app_id, timeout=30):
    selector = f'[amp-appid="{app_id}"]'
    # 查找readyapp。
    # 入参：current_driver（当前驱动）。
    # 返回：由当前表达式或分支生成的处理结果。
    def find_ready_app(current_driver):
        for element in current_driver.find_elements(By.CSS_SELECTOR, selector):
            if element.is_displayed() and str(element.get_attribute("amp-url") or "").strip():
                return element
        return False

    # 双重校验：即使调用方先前检测通过，也不能在掉线后点击服务应用。
    if portal_login_required(driver):
        raise AcademicSessionRefreshRequired("门户仍显示登录入口，禁止提前打开教务服务弹窗")
    element = WebDriverWait(driver, timeout).until(find_ready_app)
    if portal_login_required(driver):
        raise AcademicSessionRefreshRequired("等待应用加载期间登录态已失效")
    driver.execute_script("arguments[0].scrollIntoView({block: 'center'});", element)
    click_with_pointer(driver, element, PORTAL_INTERACTION_DELAY_SECONDS)


# portalappisvisible的处理逻辑。
# 入参：driver（驱动）；app_id（app标识）。
# 返回：由当前表达式或分支生成的处理结果。
def portal_app_is_visible(driver, app_id):
    selector = f'[amp-appid="{app_id}"]'
    return any(element.is_displayed() for element in driver.find_elements(By.CSS_SELECTOR, selector))


def get_visible_portal_login_button(driver):
    """检查门户真实的登录入口；服务应用图标显示不代表已登录。"""
    for element in driver.find_elements(By.ID, "ampLoginBtn"):
        if not element.is_displayed():
            continue
        # 有些门户登录后仍保留同 ID 的用户菜单；只把登录文案当成未登录。
        label = " ".join(
            str(value or "")
            for value in (
                element.text,
                element.get_attribute("aria-label"),
                element.get_attribute("title"),
            )
        ).replace(" ", "")
        if not label or ("登录" in label and "退出登录" not in label):
            return element

    # 兼容门户页面调整按钮 ID，但保留“登录”文字的情况。
    for element in driver.find_elements(
        By.XPATH,
        "//header//*[self::a or self::button][normalize-space(.)='登录']"
        " | //*[contains(@class,'header') or contains(@class,'top')]"
        "//*[self::a or self::button][normalize-space(.)='登录']",
    ):
        if element.is_displayed():
            return element
    return None


def portal_login_required(driver):
    return get_visible_portal_login_button(driver) is not None


def is_portal_authenticated_for_navigation(driver):
    """仅用于允许继续门户导航；教务权限仍由目标页面独立校验。"""
    if is_identity_login_page(driver.current_url, driver.title, driver.page_source):
        return False
    if portal_login_required(driver):
        return False
    return portal_app_is_visible(driver, ACADEMIC_SYSTEM.portal_app_id)


def wait_for_portal_authentication(driver, timeout=60):
    """不能只等待应用出现，必须等待登录入口消失并保持稳定。"""
    stable_since = None

    def authenticated_stably(current_driver):
        nonlocal stable_since
        if is_portal_authenticated_for_navigation(current_driver):
            if stable_since is None:
                stable_since = time.monotonic()
            return time.monotonic() - stable_since >= 1.5
        stable_since = None
        return False

    try:
        WebDriverWait(driver, timeout).until(authenticated_stably)
    except Exception as error:
        reason = (
            "等待门户登录完成超时；请确认右上角不再显示‘登录’。"
            "如有验证码或二次验证，请在可视浏览器中完成。"
        )
        if getattr(driver, "_timetable_headless", HEADLESS):
            raise AcademicSessionRefreshRequired(reason) from error
        raise RuntimeError(reason) from error


# 归还目标portal窗口。
# 入参：driver（驱动）。
def return_to_portal_window(driver):
    portal_host = urllib.parse.urlparse(ACADEMIC_SYSTEM.portal_url).hostname
    if len(driver.window_handles) > 1:
        try:
            driver.close()
        except Exception:
            pass
    for handle in list(driver.window_handles):
        driver.switch_to.window(handle)
        if urllib.parse.urlparse(str(driver.current_url or "")).hostname == portal_host:
            return
    raise RuntimeError("无法返回办事大厅窗口进行人工授权")


# clickportalenter服务的处理逻辑。
# 入参：driver（驱动）；timeout（超时）。
def click_portal_enter_service(driver, timeout=30):
    # 未登录时，不要尝试点击“进入服务”或等待一个禁用按钮。
    if portal_login_required(driver):
        raise AcademicSessionRefreshRequired("门户仍显示登录入口，禁止打开教务系统服务")

    def find_visible_enter_button(current_driver):
        if portal_login_required(current_driver):
            raise AcademicSessionRefreshRequired("门户登录态失效，请先重新登录")
        candidates = current_driver.find_elements(By.ID, "ampDetailEnter")
        candidates.extend(
            current_driver.find_elements(
                By.XPATH,
                "//*[self::a or self::button][contains(normalize-space(.), '进入服务')]",
            )
        )
        # 仅点击可见且未被禁用的入口。
        return next(
            (element for element in candidates if element.is_displayed() and element.is_enabled()),
            False,
        )

    button = WebDriverWait(driver, timeout).until(find_visible_enter_button)
    driver.execute_script("arguments[0].scrollIntoView({block: 'center'});", button)
    click_with_pointer(driver, button, PORTAL_INTERACTION_DELAY_SECONDS)


# =========================
# Excel 导出处理
# =========================

def flatten_columns(df):
    """
    处理 pandas 读取 HTML 合并表头后产生的 MultiIndex 多级表头
    """
    if isinstance(df.columns, pd.MultiIndex):
        new_columns = []

        for col in df.columns:
            parts = []

            for item in col:
                item = clean_text(str(item))

                if not item:
                    continue

                if item.lower() == "nan":
                    continue

                if item.startswith("Unnamed"):
                    continue

                parts.append(item)

            if parts:
                new_columns.append("_".join(parts))
            else:
                new_columns.append("列")

        df.columns = new_columns

    else:
        df.columns = [clean_text(str(col)) for col in df.columns]

    # 防止列名重复
    seen = {}
    final_columns = []

    for col in df.columns:
        if col not in seen:
            seen[col] = 1
            final_columns.append(col)
        else:
            seen[col] += 1
            final_columns.append(f"{col}_{seen[col]}")

    df.columns = final_columns

    return df


def clean_excel_illegal_chars(value):
    """
    清理 Excel 不支持的非法字符
    """
    if isinstance(value, str):
        value = re.sub(r"[\x00-\x08\x0B-\x0C\x0E-\x1F]", "", value)

    return value


def export_tables(html, output_excel):
    """
    把返回 HTML 里的所有表格导出到指定文件夹中的 Excel 和 CSV
    """
    try:
        tables = pd.read_html(StringIO(html))
    except Exception as e:
        print("没有识别到 HTML 表格：", e)
        return False

    if not tables:
        print("没有识别到任何表格。")
        return False

    print("识别到表格数量：", len(tables))

    output_excel_path = OUTPUT_DIR / output_excel

    with pd.ExcelWriter(output_excel_path, engine="openpyxl") as writer:
        for i, df in enumerate(tables, start=1):
            print(f"正在处理 table_{i}：{df.shape[0]} 行，{df.shape[1]} 列")

            # 把多级表头转成普通表头
            df = flatten_columns(df)

            # 清理 Excel 非法字符
            try:
                df = df.map(clean_excel_illegal_chars)
            except AttributeError:
                df = df.applymap(clean_excel_illegal_chars)

            sheet_name = f"table_{i}"
            df.to_excel(writer, sheet_name=sheet_name, index=False)

            csv_path = OUTPUT_DIR / f"班级课表大表结果_{i}.csv"
            df.to_csv(csv_path, index=False, encoding="utf-8-sig")

            print(f"{sheet_name}：{df.shape[0]} 行，{df.shape[1]} 列")
            print("已保存：", csv_path)

    print("导出完成：", output_excel_path)
    return True


# =========================
# 请求真实大表接口
# =========================

def build_big_table_payload():
    return {
        "xnxqh": SEMESTER,
        "kbjcmsid": ACADEMIC_SYSTEM.timetable_mode_id,

        # 下面这些为空，表示不筛选，尽量查大范围
        "skyx": "",
        "sknj": "",
        "skzy": "",
        "skbjid": "",
        "skbj": "",
        "zc1": "",
        "zc2": "",
        "skxq1": "",
        "skxq2": "",
        "jc1": "",
        "jc2": "",
    }


# 解码响应正文。
# 入参：response（响应）。
# 返回：由当前表达式或分支生成的处理结果。
def decode_response_body(response):
    content_type = response.headers.get("Content-Type", "")
    charset_match = re.search(r"charset=([\w-]+)", content_type, re.IGNORECASE)
    charset = charset_match.group(1) if charset_match else "utf-8"
    body = response.read()
    try:
        return body.decode(charset, errors="replace")
    except LookupError:
        return body.decode("utf-8", errors="replace")


def fetch_big_table_by_http(driver):
    """
    使用浏览器当前 Cookie 由 Python 直接请求真实接口。
    这比在页面里执行 fetch 更稳，能避开浏览器跨域/跳转限制。
    """

    payload = build_big_table_payload()
    body = urllib.parse.urlencode(payload).encode("utf-8")
    cookies = driver.get_cookies()
    cookie_header = "; ".join(
        f"{cookie.get('name')}={cookie.get('value')}"
        for cookie in cookies
        if cookie.get("name") and cookie.get("value") is not None
    )
    user_agent = driver.execute_script("return navigator.userAgent") or "Mozilla/5.0"

    request = urllib.request.Request(
        ACADEMIC_SYSTEM.class_timetable_result_url,
        data=body,
        headers={
            "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
            "Cookie": cookie_header,
            "Origin": ACADEMIC_SYSTEM.academic_origin,
            "Referer": ACADEMIC_SYSTEM.class_timetable_url,
            "User-Agent": user_agent,
            "X-Requested-With": "XMLHttpRequest",
        },
        method="POST",
    )

    print("正在用后端 HTTP 请求真实接口：", ACADEMIC_SYSTEM.class_timetable_result_url)
    with urllib.request.urlopen(request, timeout=120) as response:
        html = decode_response_body(response)

    if "统一身份认证" in html or "登录" in BeautifulSoup(html, "lxml").get_text(" "):
        print("HTTP 请求结果疑似登录页，将回退到浏览器 fetch。")
        return ""

    return html


def fetch_big_table_by_browser(driver):
    """
    直接在浏览器当前登录环境中执行 fetch。
    浏览器会自动带上登录 Cookie。
    """

    payload = build_big_table_payload()

    js_code = """
    const url = arguments[0];
    const payload = arguments[1];
    const callback = arguments[2];

    const params = new URLSearchParams();

    for (const key in payload) {
        params.append(key, payload[key]);
    }

    fetch(url, {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: params.toString(),
        credentials: "include"
    })
    .then(response => response.text())
    .then(text => callback(text))
    .catch(error => callback("FETCH_ERROR:" + error.toString()));
    """

    print("正在请求真实接口：", ACADEMIC_SYSTEM.class_timetable_result_url)

    driver.set_script_timeout(120)

    html = driver.execute_async_script(js_code, ACADEMIC_SYSTEM.class_timetable_result_url, payload)

    if html.startswith("FETCH_ERROR:"):
        raise Exception(html)

    return html


# 获取bigtable。
# 入参：driver（驱动）。
# 返回：由当前表达式或分支生成的处理结果。
def fetch_big_table(driver):
    try:
        html = fetch_big_table_by_http(driver)
        if html:
            return html
    except Exception as e:
        print("后端 HTTP 请求真实接口失败，回退到浏览器 fetch：", e)

    return fetch_big_table_by_browser(driver)


# =========================
# 登录门户并进入教务系统
# =========================

def login_portal_and_enter_jw(driver):
    """
    登录门户并进入教务系统
    """
    driver.get(ACADEMIC_SYSTEM.portal_url)
    if not getattr(driver, "_timetable_headless", HEADLESS):
        driver.maximize_window()

    time.sleep(3)

    print("当前地址：", driver.current_url)
    print("当前标题：", driver.title)

    # 1. 判断登录状态时只看真实登录入口，不用应用图标是否出现来判断。
    # 门户可能允许未登录用户打开服务说明弹窗。
    if not is_identity_login_page(driver.current_url, driver.title, driver.page_source):
        login_button = get_visible_portal_login_button(driver)
        if login_button is not None:
            print("检测到门户未登录，先进入统一身份认证...")
            driver.execute_script("arguments[0].scrollIntoView({block: 'center'});", login_button)
            click_with_pointer(driver, login_button, 0.2)
            WebDriverWait(driver, 45).until(
                lambda current_driver: (
                    is_identity_login_page(
                        current_driver.current_url,
                        current_driver.title,
                        current_driver.page_source,
                    )
                    or is_portal_authenticated_for_navigation(current_driver)
                )
            )

    # 2. 在统一身份认证页输入账号密码；验证码等仍由学校正常验证。
    if is_identity_login_page(driver.current_url, driver.title, driver.page_source):
        wait_input_by_id(driver, "username", USERNAME, timeout=15)
        wait_input_by_id(driver, "password", PASSWORD, timeout=15)
        login_btn = WebDriverWait(driver, 20).until(
            EC.element_to_be_clickable((By.CSS_SELECTOR, "#casLoginForm button[type='submit']"))
        )
        js_click(driver, login_btn)
        print("已提交统一身份认证登录")

    # 必须明确等到登录按钮不再可见，才能访问门户里的教务服务。
    wait_for_portal_authentication(driver, timeout=60)
    print("门户登录状态检查通过，准备进入教务系统")

    target_host = urllib.parse.urlparse(ACADEMIC_SYSTEM.class_timetable_url).hostname
    effective_headless = getattr(driver, "_timetable_headless", HEADLESS)
    manual_bootstrap = should_use_manual_bootstrap(effective_headless, PROFILE_AUTH_MARKER.exists())

    if manual_bootstrap:
        # 首次授权也主动导航，仅在自动操作失败时等待人工处理。
        try:
            click_portal_app(driver, ACADEMIC_SYSTEM.portal_app_id)
            print("可视授权：已自动点击教务系统应用")
            click_portal_enter_service(driver, timeout=30)
            print("可视授权：已自动点击进入服务")
            wait_for_target_window(driver, target_host, NAVIGATION_TIMEOUT_SECONDS, target_url=None, require_authenticated=True)
            ensure_academic_authenticated(driver.current_url, driver.title, driver.page_source)
        except Exception as error:
            print("可视自动续权未成功，已切换为人工点击兜底：", error)
            return_to_portal_window(driver)
            print("请完成页面上的授权；如仍停留在门户，请点击教务系统及进入服务。")
            wait_for_target_window(driver, target_host, 300, target_url=None, require_authenticated=True)
    else:
        # 已授权的持久化会话可在后台自动打开门户应用。
        click_portal_app(driver, ACADEMIC_SYSTEM.portal_app_id)
        print("已模拟点击门户中的教务系统应用")

        try:
            click_portal_enter_service(driver, timeout=30)
            print("已模拟点击服务说明弹窗中的进入服务")
        except Exception as error:
            raise RuntimeError("门户服务说明弹窗已打开，但未能点击“进入服务”按钮") from error

        print("正在等待教务系统，最长等待秒数：", NAVIGATION_TIMEOUT_SECONDS)
        try:
            wait_for_target_window(
                driver,
                target_host,
                NAVIGATION_TIMEOUT_SECONDS,
                target_url=None,
                require_authenticated=True,
            )
        except AcademicAuthenticationRequired as error:
            if effective_headless:
                PROFILE_AUTH_MARKER.unlink(missing_ok=True)
                raise AcademicSessionRefreshRequired(str(error)) from error
            raise

    # 4. 已确认进入教务系统
    print("进入教务系统后的地址：", driver.current_url)
    print("进入教务系统后的标题：", driver.title)
    try:
        ensure_academic_authenticated(driver.current_url, driver.title, driver.page_source)
    except AcademicAuthenticationRequired:
        if getattr(driver, "_timetable_headless", HEADLESS):
            PROFILE_AUTH_MARKER.unlink(missing_ok=True)
            raise AcademicSessionRefreshRequired(
                "爬虫保存的教务登录态已失效，授权标记已清除；"
                "需要切换到可视浏览器重新授权"
            )

        print("当前是教务登录页，正在填入账号密码；请在浏览器中输入验证码并点击登录。")
        wait_input_by_id(driver, "userAccount", USERNAME, timeout=15)
        wait_input_by_id(driver, "userPassword", PASSWORD, timeout=15)

        # 教务页码is已认证状态的处理逻辑。
        # 入参：current_driver（当前驱动）。
        # 返回：由当前表达式或分支生成的处理结果。
        def academic_page_is_authenticated(current_driver):
            try:
                ensure_academic_authenticated(
                    current_driver.current_url,
                    current_driver.title,
                    current_driver.page_source,
                )
                return True
            except AcademicAuthenticationRequired:
                return False

        WebDriverWait(driver, 300).until(academic_page_is_authenticated)
        print("教务系统人工授权成功，登录态已保存到持久化浏览器目录")

    PROFILE_AUTH_MARKER.write_text("authorized\n", encoding="utf-8")


# =========================
# 主程序
# =========================

def create_driver(automatic_visible_authorization=False):
    # 有历史授权才允许无头运行；恢复模式显式覆盖为可视浏览器，沿用同一用户目录。
    BROWSER_PROFILE_DIR.mkdir(parents=True, exist_ok=True)
    effective_headless = should_run_headless(HEADLESS, PROFILE_AUTH_MARKER.exists())
    if automatic_visible_authorization:
        effective_headless = False
    options = EdgeOptions()
    options.add_argument("--window-size=1440,1000")
    options.add_argument("--disable-gpu")
    options.add_argument(f"--user-data-dir={BROWSER_PROFILE_DIR}")

    if effective_headless:
        options.add_argument("--headless=new")
        options.add_argument("--disable-popup-blocking")
        options.add_argument("--log-level=3")

    if HEADLESS and not effective_headless:
        print("尚未建立教务持久化登录态，本次自动使用可视浏览器完成首次授权")

    driver = webdriver.Edge(options=options)
    driver._timetable_headless = effective_headless
    driver._timetable_automatic_visible_authorization = automatic_visible_authorization
    return driver


# 执行当前模块的主入口流程。
def main():
    global SEMESTER

    if not USERNAME or not PASSWORD:
        raise RuntimeError("缺少教务系统账号或密码，请先在课表抓取页面保存账号配置")

    driver = create_driver()
    authorization_retried = False

    try:
        try:
            login_portal_and_enter_jw(driver)
        except (AcademicSessionRefreshRequired, TargetSystemUnavailable) as error:
            if not should_retry_with_visible_authorization(
                    getattr(driver, "_timetable_headless", False),
                    authorization_retried):
                raise

            authorization_retried = True
            # 门户跳转超时与会话失效共用一次可视恢复；可视模式再失败时直接结束，防止无限重启。
            # 标记只用于决定是否允许无头模式，删除它不会删除浏览器的 Cookie 或整个用户目录。
            PROFILE_AUTH_MARKER.unlink(missing_ok=True)
            print("后台教务导航失败，准备尝试一次可视恢复：", error)
            driver.quit()
            print("教务会话已过期，正在自动重启可视浏览器；无需重新执行任务。")
            driver = create_driver(automatic_visible_authorization=True)
            login_portal_and_enter_jw(driver)

        print("正在打开班级课表查询页面...")

        # 这里不需要点击左侧菜单，直接打开真实班级课表查询页面
        driver.get(ACADEMIC_SYSTEM.class_timetable_url)

        time.sleep(3)

        print("当前地址：", driver.current_url)
        print("当前标题：", driver.title)

        page_html = driver.page_source
        save_html("班级课表查询页面.html", page_html)

        SEMESTER = resolve_semester(SEMESTER, page_html)
        print("本次抓取学期：", SEMESTER)

        # 请求真实大表接口
        result_html = fetch_big_table(driver)

        save_html("班级课表大表结果.html", result_html)
        save_text("班级课表大表结果.txt", result_html)

        ok = export_tables(result_html, "班级课表大表结果.xlsx")

        if not ok:
            print("没有导出成功。")
            print("请打开：", OUTPUT_DIR / "班级课表大表结果.html")
            print("如果里面是登录页，说明还没有真正进入教务系统。")
            print("如果里面是空白，说明接口还需要其他参数。")
        else:
            print("全部完成。")
            print("所有文件已导出到：", OUTPUT_DIR)

    finally:
        # 成功和失败都释放 WebDriver，避免残留浏览器占用持久化用户目录。
        driver.quit()


if __name__ == "__main__":
    main()
