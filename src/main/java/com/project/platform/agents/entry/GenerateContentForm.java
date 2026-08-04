package com.project.platform.agents.entry;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GenerateContentForm {
    private String chapter;      // 章节名称，如 "第1章 Java基础"
    private String[] sections;   // 小节列表，如 ["变量与数据类型", "流程控制"]
}
