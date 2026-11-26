package com.todo.service;

import com.todo.dto.AiConfig;
import com.todo.dto.AiSettingsDTO;
import com.todo.entity.UserAiConfig;
import com.todo.repository.UserAiConfigRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

@Service
public class AiSettingsService {
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final UserAiConfigRepository repository;
    private final AiService ai;
    private final String secret;

    public AiSettingsService(UserAiConfigRepository repository,
                             AiService ai,
                             @Value("${ai.config-secret:change-me-please-32-bytes-min-secret-key!!}") String secret) {
        this.repository = repository;
        this.ai = ai;
        this.secret = secret;
    }

    public AiSettingsDTO get(Long userId) {
        AiSettingsDTO dto = new AiSettingsDTO();
        repository.findByUserId(userId).ifPresent(config -> {
            dto.setBaseUrl(config.getBaseUrl());
            dto.setChatModel(config.getChatModel());
            dto.setEmbedModel(config.getEmbedModel());
            dto.setApiKeySet(config.getApiKeyEncrypted() != null && !config.getApiKeyEncrypted().isBlank());
            if (dto.isApiKeySet()) dto.setApiKey(mask(decrypt(config.getApiKeyEncrypted())));
        });
        return dto;
    }

    public void save(Long userId, AiSettingsDTO dto) {
        UserAiConfig config = repository.findByUserId(userId).orElseGet(UserAiConfig::new);
        config.setUserId(userId);
        config.setBaseUrl(trimToNull(dto.getBaseUrl()));
        config.setChatModel(trimToNull(dto.getChatModel()));
        config.setEmbedModel(trimToNull(dto.getEmbedModel()));
        // 前端回显的是脱敏值；留空表示保留原 Key
        if (dto.getApiKey() != null && !dto.getApiKey().isBlank()) {
            config.setApiKeyEncrypted(encrypt(dto.getApiKey().trim()));
        }
        repository.save(config);
    }

    public void delete(Long userId) {
        repository.findByUserId(userId).ifPresent(repository::delete);
    }

    /** 默认配置 < 数据库配置 < 单次请求覆盖 */
    public AiConfig resolve(Long userId, Map<String, String> overrides) {
        AiConfig defaults = ai.config(null);
        UserAiConfig stored = repository.findByUserId(userId).orElse(null);

        String baseUrl = stored != null && stored.getBaseUrl() != null ? stored.getBaseUrl() : defaults.baseUrl();
        String apiKey = stored != null && stored.getApiKeyEncrypted() != null
                ? decrypt(stored.getApiKeyEncrypted()) : defaults.apiKey();
        String chatModel = stored != null && stored.getChatModel() != null ? stored.getChatModel() : defaults.chatModel();
        String embedModel = stored != null && stored.getEmbedModel() != null ? stored.getEmbedModel() : defaults.embedModel();

        if (overrides != null) {
            baseUrl = pick(overrides.get("baseUrl"), baseUrl);
            apiKey = pick(overrides.get("apiKey"), apiKey);
            chatModel = pick(overrides.get("chatModel"), chatModel);
            embedModel = pick(overrides.get("embedModel"), embedModel);
        }
        baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return new AiConfig(baseUrl, apiKey, chatModel, embedModel);
    }

    private static String pick(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private static String mask(String key) {
        if (key.length() <= 8) return "****";
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }

    private SecretKeySpec keySpec() {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(digest, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("AI 配置密钥初始化失败", e);
        }
    }

    private String encrypt(String plain) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("API Key 加密失败", e);
        }
    }

    private String decrypt(String payload) {
        try {
            byte[] all = Base64.getDecoder().decode(payload);
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(all, 0, iv, 0, IV_LENGTH);
            byte[] encrypted = new byte[all.length - IV_LENGTH];
            System.arraycopy(all, IV_LENGTH, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec(), new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("API Key 解密失败", e);
        }
    }
}
