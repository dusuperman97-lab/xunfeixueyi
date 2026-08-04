-- 多智能体课程生成工作流持久化表

CREATE TABLE IF NOT EXISTS `course_workflow_task` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `task_id` VARCHAR(64) NOT NULL COMMENT '任务ID (UUID)',
    `course_id` INT NULL COMMENT '生成的课程ID',
    `user_id` INT NOT NULL COMMENT '用户ID',
    `status` VARCHAR(20) NOT NULL DEFAULT 'running' COMMENT 'running/completed/failed',
    `current_stage` VARCHAR(50) NULL COMMENT '当前阶段ID',
    `request_message` TEXT NULL COMMENT '用户输入的初始需求',
    `course_mode` VARCHAR(20) NULL COMMENT '课程模式',
    `difficulty` VARCHAR(20) NULL COMMENT '难度',
    `title` VARCHAR(200) NULL COMMENT '任务标题',
    `subtitle` VARCHAR(500) NULL COMMENT '任务副标题',
    `summary` TEXT NULL COMMENT '完成摘要',
    `result_json` JSON NULL COMMENT '最终结果JSON',
    `created_at` BIGINT NOT NULL COMMENT '创建时间戳(ms)',
    `updated_at` BIGINT NOT NULL COMMENT '更新时间戳(ms)',
    `finished_at` BIGINT NULL COMMENT '完成时间戳(ms)',
    UNIQUE KEY `uk_task_id` (`task_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程工作流任务表';

CREATE TABLE IF NOT EXISTS `course_workflow_resource` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `task_id` VARCHAR(64) NOT NULL COMMENT '关联任务ID',
    `stage_id` VARCHAR(50) NULL COMMENT '产生阶段',
    `kind` VARCHAR(20) NULL COMMENT 'web/video/image',
    `title` VARCHAR(500) NULL COMMENT '资源标题',
    `url` VARCHAR(2000) NULL COMMENT '资源URL',
    `source` VARCHAR(200) NULL COMMENT '来源名称',
    `snippet` VARCHAR(1000) NULL COMMENT '内容摘要',
    `credibility_label` VARCHAR(30) NULL COMMENT '可信度标签: verified/caution/low-confidence/pending',
    `score` DOUBLE NULL COMMENT '可信度评分 0.0-1.0',
    `note` VARCHAR(500) NULL COMMENT '备注/审查意见',
    `created_at` BIGINT NOT NULL COMMENT '创建时间戳(ms)',
    KEY `idx_task_id` (`task_id`),
    KEY `idx_stage_id` (`stage_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程工作流资源表';

CREATE TABLE IF NOT EXISTS `course_workflow_activity` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `task_id` VARCHAR(64) NOT NULL COMMENT '关联任务ID',
    `activity_id` VARCHAR(64) NOT NULL COMMENT '活动UUID',
    `stage_id` VARCHAR(50) NULL COMMENT '关联阶段',
    `agent_name` VARCHAR(100) NULL COMMENT 'Agent名称',
    `agent_role` VARCHAR(200) NULL COMMENT 'Agent角色',
    `type` VARCHAR(30) NULL COMMENT '活动类型: analysis/dispatch/search/verification/writing/result',
    `label` VARCHAR(100) NULL COMMENT '活动标签',
    `message` TEXT NULL COMMENT '活动消息',
    `timestamp` BIGINT NOT NULL COMMENT '时间戳(ms)',
    KEY `idx_task_id` (`task_id`),
    KEY `idx_stage_id` (`stage_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程工作流活动日志表';

CREATE TABLE IF NOT EXISTS `student_section_progress` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_id` INT NOT NULL COMMENT '学生ID',
    `course_id` INT NOT NULL COMMENT '课程ID',
    `chapter_id` INT NOT NULL COMMENT '章节ID',
    `section_id` INT NOT NULL COMMENT '小节ID',
    `content_viewed` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已查看基础内容',
    `practice_completed` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已完成练习',
    `resource_reviewed` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已复习资源',
    `completed` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '当前学生是否完成该小节',
    `mastery_state` VARCHAR(32) NOT NULL DEFAULT 'NOT_STARTED' COMMENT '掌握状态',
    `last_position` VARCHAR(255) NULL COMMENT '最后学习位置',
    `recommended_next_action` VARCHAR(64) NULL COMMENT '推荐下一步动作',
    `last_event_time` BIGINT NULL COMMENT '最后学习事件时间戳(ms)',
    `weak_knowledge_points` JSON NULL COMMENT '薄弱知识点列表',
    `created_at` BIGINT NOT NULL COMMENT '创建时间戳(ms)',
    `updated_at` BIGINT NOT NULL COMMENT '更新时间戳(ms)',
    UNIQUE KEY `uk_student_section_progress` (`student_id`, `course_id`, `chapter_id`, `section_id`),
    KEY `idx_student_section` (`student_id`, `section_id`),
    KEY `idx_course_section` (`course_id`, `section_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生小节学习进度表';

CREATE TABLE IF NOT EXISTS `learning_action_event` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `event_id` VARCHAR(64) NOT NULL COMMENT '事件ID',
    `student_id` INT NOT NULL COMMENT '学生ID',
    `course_id` INT NOT NULL COMMENT '课程ID',
    `chapter_id` INT NULL COMMENT '章节ID',
    `section_id` INT NOT NULL COMMENT '小节ID',
    `active_module` VARCHAR(64) NULL COMMENT '触发时所在模块',
    `knowledge_point` VARCHAR(255) NULL COMMENT '关联知识点',
    `event_type` VARCHAR(64) NOT NULL COMMENT '学习事件类型',
    `trigger_source` VARCHAR(64) NULL COMMENT '触发来源',
    `resource_id` INT NULL COMMENT '资源ID',
    `resource_url` VARCHAR(2000) NULL COMMENT '资源URL',
    `last_position` VARCHAR(255) NULL COMMENT '最后学习位置',
    `accuracy_rate` DECIMAL(8,4) NULL COMMENT '练习正确率',
    `weak_knowledge_points` JSON NULL COMMENT '事件薄弱知识点列表',
    `payload_json` JSON NULL COMMENT '扩展事件负载',
    `occurred_at` BIGINT NOT NULL COMMENT '事件发生时间戳(ms)',
    `created_at` BIGINT NOT NULL COMMENT '创建时间戳(ms)',
    UNIQUE KEY `uk_learning_event_id` (`event_id`),
    KEY `idx_learning_event_student_section` (`student_id`, `section_id`, `event_type`),
    KEY `idx_learning_event_resource` (`student_id`, `resource_id`, `event_type`),
    KEY `idx_learning_event_resource_url` (`student_id`, `resource_url`(255), `event_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习动作事件表';
