# 多窗口门户导航与身份状态识别；只观察页面，不绕过 CAS、验证码或 aTrust 授权。
import time
from urllib.parse import urlparse


class ATrustAuthenticationRequired(RuntimeError):
    pass


class AcademicAuthenticationRequired(RuntimeError):
    pass


class TargetSystemUnavailable(RuntimeError):
    pass


# 提取 URL 中的主机名。
# 入参：url（URL）。
# 返回：由当前表达式或分支生成的处理结果。
def _hostname(url):
    return (urlparse(str(url or "")).hostname or "").lower()


# 判断是否匹配主机。
# 入参：actual_host（实际主机）；target_host（目标主机）。
# 返回：由当前表达式或分支生成的处理结果。
def _matches_host(actual_host, target_host):
    expected = str(target_host or "").strip().lower()
    return bool(expected) and (actual_host == expected or actual_host.endswith(f".{expected}"))


# 判断是否为身份登录页码。
# 入参：current_url（当前URL）；title（title）；page_html（页码HTML）。
# 返回：由当前表达式或分支生成的处理结果。
def is_identity_login_page(current_url, title, page_html):
    host = _hostname(current_url)
    normalized_title = str(title or "").strip().lower()
    normalized_html = str(page_html or "").lower()
    return (
        _matches_host(host, "authserver.hniu.cn")
        or normalized_title == "统一身份认证"
        or 'id="casloginform"' in normalized_html
        or "id='casloginform'" in normalized_html
    )


# 确保满足教务已认证状态。
# 入参：current_url（当前URL）；title（title）；page_html（页码HTML）。
# 返回：由当前表达式或分支生成的处理结果。
def ensure_academic_authenticated(current_url, title, page_html):
    # 到达教务域名不等于登录成功，仍需识别登录表单，交给上层触发会话恢复。
    normalized_url = str(current_url or "").strip().lower()
    normalized_title = str(title or "").strip().lower()
    normalized_html = str(page_html or "").lower()
    if normalized_title in {"首页", "home"} or (
        "/jsxsd/framework/xsmain.htmlx" in normalized_url
        and normalized_title not in {"登录", "login"}
    ):
        return str(current_url or "")

    is_login_page = (
        normalized_title in {"登录", "login"}
        or 'id="loginform"' in normalized_html
        or "id='loginform'" in normalized_html
    )
    if is_login_page:
        raise AcademicAuthenticationRequired(
            "已到达教务系统域名，但仍是登录页，门户登录态没有转换为教务系统会话；"
            "请确认通过办事大厅点击教务系统后能够免登录进入"
        )
    return str(current_url or "")


def open_portal_app_and_wait(
    driver,
    portal_app_url,
    target_host,
    timeout_seconds,
    poll_seconds=1,
    retry_url=None,
):
    """Open an application through the authenticated portal SSO entry."""
    driver.get(portal_app_url)
    return wait_for_target_window(
        driver,
        target_host,
        timeout_seconds,
        poll_seconds=poll_seconds,
        target_url=retry_url,
    )


def wait_for_target_window(driver, target_host, timeout_seconds, poll_seconds=1, target_url=None, require_authenticated=False):
    """在所有浏览器窗口中等待目标主机，并将 WebDriver 留在命中的窗口。

    Args:
        driver: 已登录门户的 WebDriver，本函数会改变其当前窗口。
        target_host: 目标主机名，接受该主机及其子域名，不接受任意包含同名的域名。
        timeout_seconds: 轮询等待上限，单位秒；不覆盖 WebDriver 单次网络命令的超时。
        poll_seconds: 扫描窗口之间的轮询间隔，负值按零处理。
        target_url: 观察到 aTrust 后可再请求一次的目标地址，None 表示不主动重试。
        require_authenticated: 等待 SSO 完成并出现教务首页或课表页面，暂时登录页不提前返回。

    Returns:
        str: 命中目标主机时的 URL。域名匹配仅确认到达入口，不保证已建立教务登录态。

    Raises:
        ATrustAuthenticationRequired: 观察到学校零信任页面，等待结束后仍未进入目标。
        TargetSystemUnavailable: 未观察到零信任页面，且目标在等待期内没有出现。

    Notes:
        调用方须继续调用 ensure_academic_authenticated 检查身份状态，
        并负责 WebDriver 的 finally 释放；此函数不会关闭浏览器。
    """
    # 单调时钟不受系统时间校准影响；每轮重新读取句柄以发现 SSO 打开的新窗口。
    deadline = time.monotonic() + max(0, float(timeout_seconds))
    atrust_seen = False
    observed_urls = []
    target_retry_attempted = False
    academic_login_seen = False

    while True:
        for handle in list(driver.window_handles):
            try:
                driver.switch_to.window(handle)
                current_url = str(driver.current_url or "")
            except Exception:
                continue

            if current_url and current_url not in observed_urls:
                observed_urls.append(current_url)

            current_host = _hostname(current_url)
            if _matches_host(current_host, target_host):
                if not require_authenticated:
                    return current_url
                try:
                    title = str(driver.title or "").strip()
                    page_html = str(driver.page_source or "")
                    ensure_academic_authenticated(current_url, title, page_html)
                    # SSO 中转页和空白加载页不能作为登录成功的证据。
                    if title in {"首页", "home", "全校行政班课表"} or (
                        "/jsxsd/framework/xsmain.htmlx" in current_url.lower()
                        and title.lower() not in {"", "loading", "loading..."}
                    ) or 'id="timetable"' in page_html or "id='timetable'" in page_html:
                        return current_url
                except AcademicAuthenticationRequired:
                    academic_login_seen = True
                except Exception:
                    # 重定向时窗口或文档可能短暂不可读，下一轮重新观察。
                    continue
            if _matches_host(current_host, "atrust.hniu.cn"):
                atrust_seen = True

        # 仅在观察到 aTrust 后重试目标一次；持续被拦截则明确要求完成学校授权。
        if atrust_seen and target_url and not target_retry_attempted and not require_authenticated:
            target_retry_attempted = True
            try:
                driver.get(target_url)
            except Exception:
                pass

        if time.monotonic() >= deadline:
            break
        time.sleep(max(0, float(poll_seconds)))

    if academic_login_seen:
        raise AcademicAuthenticationRequired("等待门户 SSO 完成超时，教务系统仍未出现已登录首页或课表页")

    if atrust_seen:
        raise ATrustAuthenticationRequired(
            "检测到学校 aTrust 零信任页面，但在等待时间内未进入教务系统；"
            "脚本已重新请求教务系统但仍被拦截，请先使用学校授权的 aTrust 客户端完成连接后再重试"
        )

    detail = f"；已观察到：{', '.join(observed_urls)}" if observed_urls else ""
    raise TargetSystemUnavailable(f"等待教务系统 {target_host} 超时{detail}")
