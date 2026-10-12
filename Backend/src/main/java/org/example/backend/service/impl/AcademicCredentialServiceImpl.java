package org.example.backend.service.impl;

import org.example.backend.entity.AcademicCredentialView;
import org.example.backend.entity.AcademicScheduleImportRequest;
import org.example.backend.mapper.ClassTimetableMapper;
import org.example.backend.service.AcademicCredentialService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

@Service
public class AcademicCredentialServiceImpl implements AcademicCredentialService {
    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int MIN_SECRET_LENGTH = 32;

    private final SecureRandom secureRandom = new SecureRandom();
    private final ClassTimetableMapper classTimetableMapper;
    private final SecretKeySpec secretKey;

    public AcademicCredentialServiceImpl(
            ClassTimetableMapper classTimetableMapper,
            @Value("${academic.credentials.secret:${ACADEMIC_CREDENTIAL_SECRET:}}") String secret
    ) {
        this.classTimetableMapper = classTimetableMapper;
        this.secretKey = buildSecretKey(requireStrongSecret(secret, "ACADEMIC_CREDENTIAL_SECRET"));
    }

    private String requireStrongSecret(String value, String name) {
        String secret = value == null ? "" : value.trim();
        if (secret.isBlank()) {
            throw new IllegalStateException(name + " must be configured");
        }
        if (secret.toLowerCase().contains("change-me") || secret.toLowerCase().contains("dev-secret")) {
            throw new IllegalStateException(name + " must not use the development default");
        }
        if (secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(name + " must be at least 32 characters");
        }
        return secret;
    }

    @Override
    public synchronized AcademicCredentialView saveCredential(AcademicScheduleImportRequest request) {
        String credentialKey = resolveCredentialKey(request);
        if (isBlank(request.getUsername()) || isBlank(request.getPassword())) {
            throw new IllegalArgumentException("保存教务凭据需要账号和密码");
        }

        classTimetableMapper.upsertCrawlerCredential(
                credentialKey,
                encrypt(request.getUsername()),
                encrypt(request.getPassword())
        );
        return getCredentialView(credentialKey);
    }

    @Override
    public synchronized AcademicCredentialView getCredentialView(String credentialKey) {
        String resolvedKey = normalizeCredentialKey(credentialKey);
        return toView(resolvedKey, classTimetableMapper.getCrawlerCredential(resolvedKey));
    }

    @Override
    public synchronized void deleteCredential(String credentialKey) {
        String resolvedKey = normalizeCredentialKey(credentialKey);
        classTimetableMapper.deleteCrawlerCredential(resolvedKey);
    }

    @Override
    public synchronized void applySavedCredential(AcademicScheduleImportRequest request) {
        String credentialKey = resolveCredentialKey(request);
        Map<String, Object> credential = classTimetableMapper.getCrawlerCredential(credentialKey);
        if (credential == null || credential.isEmpty()) {
            throw new IllegalArgumentException("未配置本地教务账号，请先保存账号密码");
        }
        String usernameCipher = stringValue(credential.get("usernameCipher"));
        String passwordCipher = stringValue(credential.get("passwordCipher"));
        if (isBlank(usernameCipher) || isBlank(passwordCipher)) {
            throw new IllegalArgumentException("未配置本地教务账号，请先保存账号密码");
        }
        request.setUsername(decrypt(usernameCipher));
        request.setPassword(decrypt(passwordCipher));
    }

    private AcademicCredentialView toView(String credentialKey, Map<String, Object> credential) {
        AcademicCredentialView view = new AcademicCredentialView();
        view.setCredentialKey(credentialKey);
        view.setConfigured(credential != null);
        if (credential != null) {
            String usernameCipher = stringValue(credential.get("usernameCipher"));
            String passwordCipher = stringValue(credential.get("passwordCipher"));
            view.setConfigured(!isBlank(usernameCipher) && !isBlank(passwordCipher));
            if (Boolean.TRUE.equals(view.getConfigured())) {
                view.setUsernameMasked(maskUsername(decrypt(usernameCipher)));
                view.setUpdatedAt(stringValue(credential.get("updatedAt")));
            }
        }
        return view;
    }

    private String encrypt(String value) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception exception) {
            throw new IllegalStateException("教务凭据加密失败", exception);
        }
    }

    private String decrypt(String value) {
        try {
            byte[] payload = Base64.getDecoder().decode(value);
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[payload.length - IV_LENGTH];
            System.arraycopy(payload, 0, iv, 0, IV_LENGTH);
            System.arraycopy(payload, IV_LENGTH, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("教务凭据解密失败，请确认 academic.credentials.secret 未变更", exception);
        }
    }

    private SecretKeySpec buildSecretKey(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] key = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(key, "AES");
        } catch (Exception exception) {
            throw new IllegalStateException("初始化教务凭据密钥失败", exception);
        }
    }

    private String resolveCredentialKey(AcademicScheduleImportRequest request) {
        return normalizeCredentialKey(request == null ? null : request.getCredentialKey());
    }

    private String normalizeCredentialKey(String credentialKey) {
        if (isBlank(credentialKey)) {
            return "default";
        }
        return credentialKey.replaceAll("[^a-zA-Z0-9_.-]", "_");
    }

    private String maskUsername(String username) {
        if (isBlank(username)) {
            return "";
        }
        if (username.length() <= 4) {
            return username.charAt(0) + "***";
        }
        return username.substring(0, 2) + "****" + username.substring(username.length() - 2);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
