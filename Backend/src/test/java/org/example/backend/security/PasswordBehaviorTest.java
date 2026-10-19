package org.example.backend.security;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordBehaviorTest {
    @Test void hashesUseRandomSaltsAndVerifyUnicodeWithoutAcceptingWrongPasswords(){PasswordService service=new PasswordService(1);String first=service.encode("密码Pass123"),second=service.encode("密码Pass123");assertNotEquals(first,second);assertTrue(first.startsWith("pbkdf2$10000$"));assertTrue(service.isEncoded(first));assertTrue(service.matches("密码Pass123",first));assertFalse(service.matches("wrong",first));assertFalse(service.needsRehash(first));}
    @Test void iterationUpgradesPreserveOldHashVerification(){PasswordService old=new PasswordService(10000),next=new PasswordService(20000);String encoded=old.encode("Pass1234");assertTrue(next.needsRehash(encoded));assertTrue(next.matches("Pass1234",encoded));assertFalse(old.needsRehash(next.encode("Pass1234")));}
    @Test void legacyPlaintextAndMalformedHashesFailSafely(){PasswordService service=new PasswordService(10000);assertTrue(service.matches("legacy","legacy"));assertFalse(service.matches("wrong","legacy"));assertTrue(service.needsRehash("legacy"));for(String stored:new String[]{null,"","pbkdf2$bad$notbase64$hash","pbkdf2$0$AA==$AA==","pbkdf2$-1$AA==$AA=="}){assertFalse(service.matches("password",stored));assertTrue(service.needsRehash(stored));}assertFalse(service.matches(null,"legacy"));assertFalse(service.isEncoded(null));assertFalse(service.isEncoded("pbkdf2$10000$missing"));}
}
