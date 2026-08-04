package com.project.platform.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SessionVO {
    private String memoryId;
    private String sessionTitle;
    private Instant createTime;
    private Instant expirationTime;
}
