-- 思维导图表
CREATE TABLE IF NOT EXISTS `mind_map` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `title` varchar(200) NOT NULL COMMENT '思维导图标题',
  `file_url` varchar(500) NOT NULL COMMENT '文件URL地址',
  `chapter_id` int DEFAULT NULL COMMENT '关联章节ID（可选）',
  `section_id` int DEFAULT NULL COMMENT '关联小节ID（可选）',
  `source_type` varchar(20) DEFAULT 'manual' COMMENT '来源类型: manual-手动生成, course-课程生成',
  `topic` varchar(500) DEFAULT NULL COMMENT '主题/关键词',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_chapter_id` (`chapter_id`),
  KEY `idx_section_id` (`section_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='思维导图表';
