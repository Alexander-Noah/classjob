package org.example.backend.identityaccess.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Pbkdf2PasswordAdapterTest {
    @Test
    void verifiesEncodedAndLegacyPasswordsAndSignalsUpgrade() {
        Pbkdf2PasswordAdapter adapter = new Pbkdf2PasswordAdapter(10_000);
        String encoded = adapter.encode("Secret123");

        assertTrue(adapter.matches("Secret123", encoded));
        assertFalse(adapter.matches("wrong", encoded));
        assertTrue(adapter.matches("Secret123", "Secret123"));
        assertTrue(adapter.needsRehash("Secret123"));
        assertFalse(adapter.needsRehash(encoded));
    }
}
