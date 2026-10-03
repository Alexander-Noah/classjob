package org.example.backend.platform.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthenticationAttributesTest {
    @Test
    void exposesStableRequestAttributeNames() {
        assertEquals("authUsername", AuthenticationAttributes.USERNAME);
        assertEquals("authRole", AuthenticationAttributes.ROLE);
        assertEquals("authUserId", AuthenticationAttributes.USER_ID);
    }
}
