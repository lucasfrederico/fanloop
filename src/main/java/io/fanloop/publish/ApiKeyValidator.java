package io.fanloop.publish;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

@Component
public class ApiKeyValidator {

    private final byte[] expected;

    public ApiKeyValidator(@Value("${fanloop.api-key}") String apiKey) {
        this.expected = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    /** Constant-time comparison to avoid timing leaks on the key. */
    public boolean isValid(String provided) {
        if (provided == null) {
            return false;
        }
        return MessageDigest.isEqual(expected, provided.getBytes(StandardCharsets.UTF_8));
    }
}
