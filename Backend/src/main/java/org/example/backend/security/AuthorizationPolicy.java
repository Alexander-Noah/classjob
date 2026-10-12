package org.example.backend.security;

public interface AuthorizationPolicy {
    boolean isAllowed(String authority, String action);
}
