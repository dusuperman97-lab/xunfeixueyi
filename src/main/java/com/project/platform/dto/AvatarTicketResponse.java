package com.project.platform.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AvatarTicketResponse {
    String appId;
    String sceneId;
    String sceneVersion;
    String serverUrl;
    String signedUrl;
    String avatarId;
    String vcn;
    long expiresAt;
}
