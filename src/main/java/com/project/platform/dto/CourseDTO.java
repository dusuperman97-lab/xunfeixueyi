package com.project.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 课程实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseDTO {
    private String courseName;
    private Integer firstChapterId;
    private Integer firstSectionId;
    private String description;
    private List<ChapterOutlineDTO> chapterOutline;
}