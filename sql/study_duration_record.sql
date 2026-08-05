-- 学生学习时长记录表
CREATE TABLE IF NOT EXISTS `study_duration_record` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `student_id` INT NOT NULL COMMENT '学生ID',
  `course_id` INT COMMENT '课程ID',
  `study_date` DATE NOT NULL COMMENT '学习日期',
  `duration_minutes` INT NOT NULL DEFAULT 0 COMMENT '学习时长（分钟）',
  `study_type` VARCHAR(50) COMMENT '学习类型：课后巩固练习、理论知识梳理、错题复盘整理、本周内容复盘等',
  `section_id` INT COMMENT '关联小节ID（可选）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_student_date` (`student_id`, `study_date`),
  KEY `idx_student_course_date` (`student_id`, `course_id`, `study_date`),
  KEY `idx_study_date` (`study_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生学习时长记录表';

-- 如果表已存在，补充 course_id 列
-- ALTER TABLE `study_duration_record` ADD COLUMN `course_id` INT COMMENT '课程ID' AFTER `student_id`;
-- ALTER TABLE `study_duration_record` ADD KEY `idx_student_course_date` (`student_id`, `course_id`, `study_date`);

-- 插入示例数据（近七天的学习记录）
INSERT INTO `study_duration_record` (`student_id`, `course_id`, `study_date`, `duration_minutes`, `study_type`, `create_time`) VALUES
-- 学生1的近七天学习记录
(1, 1, CURDATE() - INTERVAL 6 DAY, 100, '课后巩固练习', NOW() - INTERVAL 6 DAY),
(1, 1, CURDATE() - INTERVAL 5 DAY, 200, '理论知识梳理', NOW() - INTERVAL 5 DAY),
(1, 1, CURDATE() - INTERVAL 4 DAY, 150, '错题复盘整理', NOW() - INTERVAL 4 DAY),
(1, 2, CURDATE() - INTERVAL 3 DAY, 80, '理论知识梳理', NOW() - INTERVAL 3 DAY),
(1, 2, CURDATE() - INTERVAL 2 DAY, 120, '课后巩固练习', NOW() - INTERVAL 2 DAY),
(1, 1, CURDATE() - INTERVAL 1 DAY, 90, '本周内容复盘', NOW() - INTERVAL 1 DAY),
(1, 2, CURDATE(), 300, '理论知识梳理', NOW()),

-- 学生2的近七天学习记录
(2, 1, CURDATE() - INTERVAL 6 DAY, 60, '课后巩固练习', NOW() - INTERVAL 6 DAY),
(2, 1, CURDATE() - INTERVAL 5 DAY, 0, NULL, NOW() - INTERVAL 5 DAY),
(2, 2, CURDATE() - INTERVAL 4 DAY, 180, '理论知识梳理', NOW() - INTERVAL 4 DAY),
(2, 2, CURDATE() - INTERVAL 3 DAY, 120, '错题复盘整理', NOW() - INTERVAL 3 DAY),
(2, 1, CURDATE() - INTERVAL 2 DAY, 0, NULL, NOW() - INTERVAL 2 DAY),
(2, 1, CURDATE() - INTERVAL 1 DAY, 90, '本周内容复盘', NOW() - INTERVAL 1 DAY),
(2, 2, CURDATE(), 150, '课后巩固练习', NOW());
