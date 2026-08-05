-- 智能体角色表
CREATE TABLE IF NOT EXISTS `agent_role` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码，如 demand_analyst',
    `role_name` VARCHAR(100) NOT NULL COMMENT '角色名称，如 需求解析师',
    `stage_id` VARCHAR(50) NULL COMMENT '所属阶段ID',
    `stage_name` VARCHAR(100) NULL COMMENT '所属阶段名称',
    `description` VARCHAR(500) NULL COMMENT '角色描述',
    `accent_color` VARCHAR(20) NULL COMMENT 'UI 强调色',
    `avatar` VARCHAR(500) NULL COMMENT '头像文件名，如 画像分析师.png',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    `is_active` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用: 1-启用, 0-停用',
    `created_at` BIGINT NOT NULL COMMENT '创建时间戳(ms)',
    `updated_at` BIGINT NOT NULL COMMENT '更新时间戳(ms)',
    UNIQUE KEY `uk_role_code` (`role_code`),
    KEY `idx_stage_id` (`stage_id`),
    KEY `idx_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能体角色表';

-- 初始数据：多智能体课程生成工作流中的 11 个默认角色
INSERT INTO `agent_role` (`role_code`, `role_name`, `stage_id`, `stage_name`, `description`, `accent_color`, `avatar`, `sort_order`, `is_active`, `created_at`, `updated_at`) VALUES
('profile_analyst',      '画像分析师',           'profile_analysis',    '学生画像分析','分析学生画像数据，根据学习风格与能力水平定制个性化策略',    '#8B5CF6', '画像分析师.png',           1,  1, UNIX_TIMESTAMP(NOW())*1000, UNIX_TIMESTAMP(NOW())*1000),
('demand_analyst',       '需求解析师',           'demand_extraction',   '需求提取',   '分析用户输入，提取课程关键需求与学习目标',                    '#6366F1', '续修分析师.png',           2,  1, UNIX_TIMESTAMP(NOW())*1000, UNIX_TIMESTAMP(NOW())*1000),
('task_scheduler',       '任务调度师',           'orchestration',       '编排派遣',   '将课程需求拆解为可执行的任务，派遣给对应智能体',              '#0F766E', '编排调度师.png',           3,  1, UNIX_TIMESTAMP(NOW())*1000, UNIX_TIMESTAMP(NOW())*1000),
('image_hunter',         '图片猎手',             'resource_collection', '资源搜集',   '在互联网中检索与课程主题相关的图片素材',                      '#F59E0B', '图片猎手.png',             4,  1, UNIX_TIMESTAMP(NOW())*1000, UNIX_TIMESTAMP(NOW())*1000),
('video_hunter',         '视频猎手',             'resource_collection', '资源搜集',   '在互联网中检索与课程主题相关的视频资源',                      '#DC2626', '视频猎手.png',             5,  1, UNIX_TIMESTAMP(NOW())*1000, UNIX_TIMESTAMP(NOW())*1000),
('web_searcher',         '网页检索师',           'resource_collection', '资源搜集',   '搜索高质量网页内容，提取结构化的课程参考资料',                '#7C3AED', '网页检索师.png',           6,  1, UNIX_TIMESTAMP(NOW())*1000, UNIX_TIMESTAMP(NOW())*1000),
('community_scout',      '社区探勘师',           'resource_collection', '资源搜集',   '在技术社区（如知乎、GitHub、Stack Overflow）挖掘前沿内容',     '#0891B2', '社区勘探师.png',           7,  1, UNIX_TIMESTAMP(NOW())*1000, UNIX_TIMESTAMP(NOW())*1000),
('quality_reviewer',     '质量审核师',           'cross_review',        '交叉审核',   '审核搜集到的资源质量，标记可信度等级',                        '#BE123C', '质量审核师.png',           8,  1, UNIX_TIMESTAMP(NOW())*1000, UNIX_TIMESTAMP(NOW())*1000),
('content_verifier',     '资料可信度验证专家',   'cross_review',        '交叉审核',   '深度验证资料准确性，交叉比对信息源，消除AI幻觉',              '#B91C1C', '资料可信度验证专家.png',   9,  1, UNIX_TIMESTAMP(NOW())*1000, UNIX_TIMESTAMP(NOW())*1000),
('content_writer',       '内容生成专家',         'delivery',            '资源交付入库','将审核通过的资料整合为结构化的课程内容',                      '#1D4ED8', '内容生成专家.png',         10, 1, UNIX_TIMESTAMP(NOW())*1000, UNIX_TIMESTAMP(NOW())*1000),
('exercise_designer',    '习题设计专家',         'delivery',            '资源交付入库','根据课程内容设计配套的练习题与测验',                          '#7E22CE', '习题设计专家.png',         11, 1, UNIX_TIMESTAMP(NOW())*1000, UNIX_TIMESTAMP(NOW())*1000);
