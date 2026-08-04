package com.project.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDTO {
    private String type;
    private List<String> docUrls;
    private List<String> imagUrls;
    private String content;
    private Long timestamp;
}
