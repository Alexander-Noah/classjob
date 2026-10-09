import time
from urllib.parse import urlparse


class ATrustAuthenticationRequired(RuntimeError):
    pass


class AcademicAuthenticationRequired(RuntimeError):
    pass


class TargetSystemUnavailable(RuntimeError):
    pass


def _hostname(url):
    return (urlparse(str(url or "")).hostname or "").lower()


def _matches_host(actual_host, target_host):
    expected = str(target_host or "").strip().lower()
    return bool(expected) and (actual_host == expected or actual_host.endswith(f".{expected}"))


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


def ensure_academic_authenticated(current_url, title, page_html):
    normalized_title = str(title or "").strip().lower()
    normalized_html = str(page_html or "").lower()
    is_login_page = (
        normalized_title in {"登录", "login"}
        or 'id="loginform"' in normalized_html
        or "id='loginform'" in normalized_html
        or "/jsxsd/xk/logintoxk" in normalized_html
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


def wait_for_target_window(driver, target_host, timeout_seconds, poll_seconds=1, target_url=None):
    deadline = time.monotonic() + max(0, float(timeout_seconds))
    atrust_seen = False
    observed_urls = []
    target_retry_attempted = False

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
                return current_url
            if _matches_host(current_host, "atrust.hniu.cn"):
                atrust_seen = True

        if atrust_seen and target_url and not target_retry_attempted:
            target_retry_attempted = True
            try:
                driver.get(target_url)
            except Exception:
                pass

        if time.monotonic() >= deadline:
            break
        time.sleep(max(0, float(poll_seconds)))

    if atrust_seen:
        raise ATrustAuthenticationRequired(
            "检测到学校 aTrust 零信任页面，但在等待时间内未进入教务系统；"
            "脚本已重新请求教务系统但仍被拦截，请先使用学校授权的 aTrust 客户端完成连接后再重试"
        )

    detail = f"；已观察到：{', '.join(observed_urls)}" if observed_urls else ""
    raise TargetSystemUnavailable(f"等待教务系统 {target_host} 超时{detail}")
