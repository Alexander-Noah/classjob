package org.example.backend.platform.security;

public interface AuthorizationPolicy {
    boolean isAllowed(String authority, String action);
}
