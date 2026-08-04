package com.project.platform.entity;

import lombok.Data;

import java.util.List;
@Data
public class ChapterOutline {
    private Integer id;
    private Integer chapter;
    private List<String> sections;
}
