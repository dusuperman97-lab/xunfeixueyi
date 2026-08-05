-- 资源推送表
-- 存储系统主动推送给学生的课程学习资源（视频/教材/文献）
-- 每次推送追加新记录，不覆盖已有资源
CREATE TABLE IF NOT EXISTS `resource_push` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `course_id` INT NOT NULL COMMENT '课程ID',
    `chapter_id` INT DEFAULT NULL COMMENT '章节ID（可选）',
    `section_id` INT DEFAULT NULL COMMENT '小节ID（可选）',

    `resource_type` VARCHAR(20) NOT NULL DEFAULT 'TEXTBOOK' COMMENT '资源类型: VIDEO / TEXTBOOK / LITERATURE',
    `title` VARCHAR(500) NOT NULL COMMENT '资源标题',
    `description` TEXT COMMENT '资源描述/摘要',
    `url` VARCHAR(1000) DEFAULT NULL COMMENT '资源原始链接',
    `embed_url` VARCHAR(1000) DEFAULT NULL COMMENT '内嵌播放地址（B站等平台用iframe嵌入）',
    `cover_url` VARCHAR(1000) DEFAULT NULL COMMENT '封面图片URL',

    `source` VARCHAR(100) DEFAULT NULL COMMENT '来源平台: B站/知乎/CSDN/GitHub等',
    `duration` VARCHAR(20) DEFAULT NULL COMMENT '视频时长 (MM:SS)',
    `author` VARCHAR(200) DEFAULT NULL COMMENT '作者/讲师',

    `pages` INT DEFAULT NULL COMMENT '教材页数',
    `journal` VARCHAR(300) DEFAULT NULL COMMENT '文献期刊名',
    `year` VARCHAR(10) DEFAULT NULL COMMENT '文献年份',
    `citations` INT DEFAULT NULL COMMENT '文献引用次数',

    `push_time` BIGINT DEFAULT NULL COMMENT '推送时间 (epoch millis)',
    `is_new` TINYINT(1) DEFAULT 1 COMMENT '是否最新推送 (1=新, 0=旧)',
    `sort_order` INT DEFAULT 0 COMMENT '排序顺序',
    `created_at` BIGINT DEFAULT NULL COMMENT '创建时间 (epoch millis)',
    `updated_at` BIGINT DEFAULT NULL COMMENT '更新时间 (epoch millis)',

    PRIMARY KEY (`id`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_course_type` (`course_id`, `resource_type`),
    KEY `idx_course_url` (`course_id`, `url`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源推送表';
