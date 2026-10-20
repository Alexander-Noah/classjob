package org.example.backend.security;

import org.example.backend.entity.UserEntity;
import org.example.backend.mapper.ClassTimetableMapper;
import org.example.backend.service.TeachingScheduleQuery;
import org.example.backend.service.impl.ClassTimetableServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CryptoFailureBoundaryTest {
    private static final String SECRET="test-only-cryptographic-key-over-thirty-two-characters";
    private final ClassTimetableMapper mapper=mock(ClassTimetableMapper.class);
    private ClassTimetableServiceImpl timetable() {
        return new ClassTimetableServiceImpl(mapper,mock(TeachingScheduleQuery.class),new MockEnvironment(),
                mock(ThreadPoolTaskScheduler.class),SECRET);
    }

    @Test void unavailableSigningProviderNeverProducesAnUnsignedToken() {
        var service=new JwtService(SECRET,7200,"smart-lab");var user=new UserEntity();user.setUsername("admin");
        var cause=new NoSuchAlgorithmException("test provider unavailable");
        try(var provider=mockStatic(Mac.class)) {
            provider.when(()->Mac.getInstance("HmacSHA256")).thenThrow(cause);
            var failure=assertThrows(IllegalStateException.class,()->service.generateToken(user));
            assertEquals("JWT 签名失败",failure.getMessage());assertSame(cause,failure.getCause());
        }
    }

    @Test void unavailableEncryptionProviderNeverWritesPlaintextAcademicCredentials() {
        var service=timetable();var cause=new NoSuchAlgorithmException("test provider unavailable");
        try(var provider=mockStatic(Cipher.class)) {
            provider.when(()->Cipher.getInstance("AES/GCM/NoPadding")).thenThrow(cause);
            var failure=assertThrows(IllegalStateException.class,()->service.saveCrawlerCredential(Map.of("username","teacher","password","test-only-password")));
            assertEquals("教务账号加密失败",failure.getMessage());assertSame(cause,failure.getCause());
        }
        verifyNoInteractions(mapper);
    }

    @Test void unavailableKeyDerivationProviderPreventsCredentialServiceStartup() {
        var cause=new NoSuchAlgorithmException("test provider unavailable");
        try(var provider=mockStatic(MessageDigest.class)) {
            provider.when(()->MessageDigest.getInstance("SHA-256")).thenThrow(cause);
            var failure=assertThrows(IllegalStateException.class,this::timetable);
            assertEquals("初始化教务账号加密密钥失败",failure.getMessage());assertSame(cause,failure.getCause());
        }
        verifyNoInteractions(mapper);
    }
}
