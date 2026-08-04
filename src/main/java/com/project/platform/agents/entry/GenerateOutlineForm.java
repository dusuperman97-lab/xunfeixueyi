package com.project.platform.agents.entry;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GenerateOutlineForm {
    private String topic;        // 主题，如 "Java后端开发"
    private String level;        // 难度级别，如 "beginner", "intermediate", "advanced"
    private String duration;     // 学习周期，如 "30天"
    private String style;        // 教学风格，如 "project-based", "theory-based"
    private String description;  // 课程描述
}
