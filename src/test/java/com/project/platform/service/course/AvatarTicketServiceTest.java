package com.project.platform.service.course;

import org.junit.jupiter.api.Test;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvatarTicketServiceTest {

    @Test
    void signsXfyunWebSocketUrlWithStandardAuthorizationFields() {
        Instant now = Instant.parse("2026-07-11T01:02:03Z");

        String signed = AvatarTicketService.sign(
                "wss://avatar.cn-huadong-1.xf-yun.com/v1/interact",
                "test-key", "test-secret", now);

        assertTrue(signed.startsWith("wss://avatar.cn-huadong-1.xf-yun.com/v1/interact?"));
        Map<String, String> query = query(signed);
        assertEquals("avatar.cn-huadong-1.xf-yun.com", query.get("host"));
        assertEquals("Sat, 11 Jul 2026 01:02:03 GMT", query.get("date"));

        String authorization = new String(Base64.getDecoder().decode(query.get("authorization")), StandardCharsets.UTF_8);
        assertTrue(authorization.contains("api_key=\"test-key\""));
        assertTrue(authorization.contains("algorithm=\"hmac-sha256\""));
        assertTrue(authorization.contains("headers=\"host date request-line\""));
        assertTrue(authorization.contains("signature=\""));
        assertFalse(signed.contains("test-secret"));
    }

    private static Map<String, String> query(String url) {
        Map<String, String> values = new LinkedHashMap<>();
        String raw = url.substring(url.indexOf('?') + 1);
        for (String pair : raw.split("&")) {
            String[] parts = pair.split("=", 2);
            values.put(parts[0], URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
        }
        return values;
    }
}
