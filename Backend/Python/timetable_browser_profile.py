def should_run_headless(headless_requested, profile_authorized):
    return bool(headless_requested and profile_authorized)


def should_use_manual_bootstrap(effective_headless, profile_authorized):
    return bool(not effective_headless and not profile_authorized)


def should_retry_with_visible_authorization(effective_headless, already_retried):
    return bool(effective_headless and not already_retried)


class AcademicSessionRefreshRequired(RuntimeError):
    """The persisted browser profile exists, but its academic SSO session expired."""
