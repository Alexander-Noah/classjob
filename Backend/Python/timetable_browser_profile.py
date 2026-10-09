def should_run_headless(headless_requested, profile_authorized):
    return bool(headless_requested and profile_authorized)
