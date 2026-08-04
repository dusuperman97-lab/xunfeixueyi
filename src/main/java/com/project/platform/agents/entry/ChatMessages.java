package com.project.platform.agents.entry;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document("messages")
public class ChatMessages {
    @Id
    private String id;

    private String userId;

    private String memoryId;

    private String sessionTitle;

    private List<String> imagUrls;

    private List<String> docUrls;

    private String content;

    private Instant expirationTime;

    private Instant createTime;
}
