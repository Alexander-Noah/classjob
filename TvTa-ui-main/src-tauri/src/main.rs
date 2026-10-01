// Prevents additional console window on Windows in release, DO NOT REMOVE!!
#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

fn main() {
    #[cfg(feature = "student-client")]
    tvta_ui_lib::run_student();

    #[cfg(not(feature = "student-client"))]
    tvta_ui_lib::run()
}
