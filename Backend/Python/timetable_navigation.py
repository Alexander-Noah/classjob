import time
from urllib.parse import urlparse


class ATrustAuthenticationRequired(RuntimeError):
    pass


class TargetSystemUnavailable(RuntimeError):
    pass


def _hostname(url):
    return (urlparse(str(url or "")).hostname or "").lower()


def _matches_host(actual_host, target_host):
    expected = str(target_host or "").strip().lower()
    return bool(expected) and (actual_host == expected or actual_host.endswith(f".{expected}"))


def wait_for_target_window(driver, target_host, timeout_seconds, poll_seconds=1):
    deadline = time.monotonic() + max(0, float(timeout_seconds))
    atrust_seen = False
    observed_urls = []

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

        if time.monotonic() >= deadline:
            break
        time.sleep(max(0, float(poll_seconds)))

    if atrust_seen:
        raise ATrustAuthenticationRequired(
            "检测到学校 aTrust 零信任页面，但在等待时间内未进入教务系统；"
            "请先使用学校授权的 aTrust 客户端完成连接后再重试"
        )

    detail = f"；已观察到：{', '.join(observed_urls)}" if observed_urls else ""
    raise TargetSystemUnavailable(f"等待教务系统 {target_host} 超时{detail}")

