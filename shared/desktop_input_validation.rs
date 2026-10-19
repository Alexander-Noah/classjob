//! Common input boundaries for both desktop HTTP servers.
use serde_json::Value;

pub const MAX_BODY_BYTES: u64 = 65536;

pub fn validate_json(body: &str, action: bool) -> Result<(), &'static str> {
    if body.len() as u64 > MAX_BODY_BYTES { return Err("request body exceeds 64KB"); }
    let value: Value = serde_json::from_str(body).map_err(|_| "invalid JSON")?;
    if !value.is_object() { return Err("request body must be an object"); }
    visit(&value, 0)?;
    if action {
        let name = value.get("action").and_then(Value::as_str).ok_or("action is required")?;
        if !matches!(name, "refresh" | "message" | "lock" | "reboot" | "shutdown") { return Err("unsupported action"); }
        if let Some(message) = value.get("message").filter(|v| !v.is_null()) {
            let text = message.as_str().ok_or("message must be text")?;
            if text.chars().count() > 1000 { return Err("message exceeds 1000 characters"); }
        }
        if let Some(id) = value.get("requestId").filter(|v| !v.is_null()) {
            if id.as_str().map_or(true, |text| text.len() > 128 || text.chars().any(char::is_control)) { return Err("invalid requestId"); }
        }
    }
    Ok(())
}

fn visit(value: &Value, depth: usize) -> Result<(), &'static str> {
    if depth > 12 { return Err("JSON nesting exceeds limit"); }
    match value {
        Value::Object(map) => {
            if map.len() > 100 { return Err("too many fields"); }
            for (key, child) in map {
                if key.len() > 100 || key.chars().any(char::is_control) { return Err("invalid field name"); }
                visit(child, depth + 1)?;
            }
        }
        Value::Array(items) => {
            if items.len() > 1000 { return Err("too many items"); }
            for item in items { visit(item, depth + 1)?; }
        }
        Value::String(text) => {
            if text.len() > 16000 || text.chars().any(|c| c.is_control() && c != '\n' && c != '\r' && c != '\t') { return Err("invalid or overlong text"); }
        }
        _ => {}
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test] fn action_allowlist_and_bounds() {
        assert!(validate_json(r#"{"action":"message","message":"O'Brien 实验室"}"#,true).is_ok());
        assert!(validate_json(r#"{"action":"execute"}"#,true).is_err());
        assert!(validate_json(r#"{"action":"lock","message":123}"#,true).is_err());
        assert!(validate_json(&format!(r#"{{"action":"message","message":"{}"}}"#,"a".repeat(1001)),true).is_err());
        assert!(validate_json(&"a".repeat(65537),false).is_err());
    }
}
