# 纯策略函数不启动浏览器，便于对首次授权、已授权和会话恢复分别做单元测试。
def should_run_headless(headless_requested, profile_authorized):
    return bool(headless_requested and profile_authorized)


# 判断是否需要usemanualbootstrap。
# 入参：effective_headless（effectiveheadless）；profile_authorized（profileauthorized）。
# 返回：由当前表达式或分支生成的处理结果。
def should_use_manual_bootstrap(effective_headless, profile_authorized):
    return bool(not effective_headless and not profile_authorized)


# 判断是否需要重试withvisibleauthorization。
# 入参：effective_headless（effectiveheadless）；already_retried（alreadyretried）。
# 返回：由当前表达式或分支生成的处理结果。
def should_retry_with_visible_authorization(effective_headless, already_retried):
    # 仅允许无头模式在单次任务内恢复一次，防止无限重启浏览器。
    return bool(effective_headless and not already_retried)


class AcademicSessionRefreshRequired(RuntimeError):
    """The persisted browser profile exists, but its academic SSO session expired."""
