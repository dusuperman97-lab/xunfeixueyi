package com.project.platform.dto;

import java.util.List;

import com.project.platform.entity.Section;
import lombok.Data;

@Data
public class ChapterOutlineDTO {
    private String chapter;      // 章节名称
    private String chapterDesc;
    private List<String> sections;  // 小节列表
}
