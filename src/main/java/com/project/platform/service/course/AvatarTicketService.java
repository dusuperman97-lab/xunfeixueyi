package com.project.platform.service.course;

import com.project.platform.dto.AvatarTicketResponse;
import com.project.platform.entity.DigitalHuman;
import com.project.platform.mapper.DigitalHumanMapper;
import com.project.platform.properties.AvatarProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Slf4j
@Service
public class AvatarTicketService {

    private static final DateTimeFormatter RFC_1123 = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC);

    private final AvatarProperties properties;
    private final DigitalHumanMapper digitalHumanMapper;
    private final Clock clock;

    @Autowired
    public AvatarTicketService(AvatarProperties properties, DigitalHumanMapper digitalHumanMapper) {
        this(properties, digitalHumanMapper, Clock.systemUTC());
    }

    AvatarTicketService(AvatarProperties properties, DigitalHumanMapper digitalHumanMapper, Clock clock) {
        this.properties = properties;
        this.digitalHumanMapper = digitalHumanMapper;
        this.clock = clock;
    }

    /**
     * 为指定的数字人创建短期有效的 SDK 签名票据。
     * <p>
     * 客户端凭此票据初始化 AvatarPlatform，无需持有长期密钥。
     * </p>
     *
     * @param digitalHumanId 数字人平台 ID（如 "111213003"）
     * @return 包含 appId、signedUrl、avatarId、vcn 等字段的票据
     * @throws IllegalArgumentException 数字人不存在、未启用或配置缺失
     */
    public AvatarTicketResponse create(String digitalHumanId) {
        requireText(digitalHumanId, "digitalHumanId 不能为空");
        log.debug("开始为 digitalHumanId={} 创建票据", digitalHumanId);

        DigitalHuman digitalHuman = digitalHumanMapper.selectByDigitalHumanId(digitalHumanId);
        if (digitalHuman == null) {
            throw new IllegalArgumentException("数字人不存在: " + digitalHumanId);
        }
        validateDigitalHumanStatus(digitalHuman);
        requireText(digitalHuman.getAvatarId(), "数字人 avatarId 未配置");
        validateConfiguration();

        Instant now = clock.instant();
        long ttlSeconds = Math.max(1, properties.getTicketTtlSeconds());
        Instant expiresAt = now.plusSeconds(ttlSeconds);
        String signedUrl = sign(properties.getServerUrl(), properties.getApiKey(), properties.getApiSecret(), now);

        AvatarTicketResponse ticket = AvatarTicketResponse.builder()
                .appId(properties.getAppId())
                .sceneId(properties.getSceneId())
                .sceneVersion(properties.getSceneVersion())
                .serverUrl(properties.getServerUrl())
                .signedUrl(signedUrl)
                .avatarId(digitalHuman.getAvatarId())
                .vcn(digitalHuman.getVcn())
                .expiresAt(expiresAt.toEpochMilli())
                .build();

        log.info("数字人票据已生成: digitalHumanId={}, avatarId={}, vcn={}, ttlSeconds={}, expiresAt={}",
                digitalHumanId, ticket.getAvatarId(), ticket.getVcn(), ttlSeconds, expiresAt);
        return ticket;
    }

    private void validateDigitalHumanStatus(DigitalHuman digitalHuman) {
        String status = digitalHuman.getStatus();
        if (status == null) {
            return; // 未设置状态视为可用
        }
        if ("启用".equals(status) || "ENABLED".equalsIgnoreCase(status)) {
            return;
        }
        throw new IllegalArgumentException(
                "数字人 " + digitalHuman.getDigitalHumanId() + " 当前状态为「" + status + "」，不可用");
    }

    static String sign(String serverUrl, String apiKey, String apiSecret, Instant now) {
        try {
            URI uri = URI.create(serverUrl);
            String host = uri.getHost();
            if (uri.getPort() != -1) {
                host += ":" + uri.getPort();
            }
            String path = uri.getRawPath();
            if (path == null || path.isBlank()) {
                path = "/";
            }
            String date = RFC_1123.format(now);
            String signatureOrigin = "host: " + host + "\n"
                    + "date: " + date + "\n"
                    + "GET " + path + " HTTP/1.1";
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String signature = Base64.getEncoder().encodeToString(
                    mac.doFinal(signatureOrigin.getBytes(StandardCharsets.UTF_8)));
            String authorizationOrigin = "api_key=\"" + apiKey
                    + "\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\""
                    + signature + "\"";
            String authorization = Base64.getEncoder().encodeToString(
                    authorizationOrigin.getBytes(StandardCharsets.UTF_8));
            String separator = serverUrl.contains("?") ? "&" : "?";
            return serverUrl + separator
                    + "authorization=" + encode(authorization)
                    + "&date=" + encode(date)
                    + "&host=" + encode(host);
        } catch (Exception e) {
            throw new IllegalStateException("生成数字人鉴权地址失败", e);
        }
    }

    private void validateConfiguration() {
        requireText(properties.getAppId(), "数字人 appId 未配置");
        requireText(properties.getApiKey(), "数字人 apiKey 未配置");
        requireText(properties.getApiSecret(), "数字人 apiSecret 未配置");
        requireText(properties.getSceneId(), "数字人 sceneId 未配置");
        requireText(properties.getServerUrl(), "数字人 serverUrl 未配置");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
