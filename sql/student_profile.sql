-- =============================================
-- 学生画像表结构 - 多维度学习特征画像
-- 适用于讯飞学弈教育平台
-- =============================================

DROP TABLE IF EXISTS `student_profile`;

CREATE TABLE `student_profile` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `student_id` INT NOT NULL COMMENT '学生ID（关联用户表）',
  
  -- ============ 基本信息维度 ============
  `username` VARCHAR(100) DEFAULT NULL COMMENT '用户名',
  `nickname` VARCHAR(100) DEFAULT NULL COMMENT '昵称',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `major` VARCHAR(100) DEFAULT NULL COMMENT '专业方向（如：计算机科学与技术）',
  `grade` VARCHAR(50) DEFAULT NULL COMMENT '年级（如：2021级本科生）',
  `learning_goal` VARCHAR(500) DEFAULT NULL COMMENT '学习目标（如：掌握Python编程）',
  `learning_style` VARCHAR(100) DEFAULT NULL COMMENT '学习风格',
  `weakness_points` TEXT DEFAULT NULL COMMENT '薄弱知识点（JSON数组）',
  `preferred_style` VARCHAR(100) DEFAULT NULL COMMENT '偏好学习风格',
  
  -- ============ 知识基础维度 (0-100分) ============
  `concept_clarity` INT DEFAULT 50 COMMENT '概念清晰度（0-100分）',
  `memory_stability` INT DEFAULT 50 COMMENT '记忆稳固性（0-100分）',
  `transfer_ability` INT DEFAULT 50 COMMENT '迁移能力（0-100分）',
  `structural_integrity` INT DEFAULT 50 COMMENT '结构完整性（0-100分）',
  `knowledge_depth` INT DEFAULT 50 COMMENT '知识深度（0-100分）',
  
  -- ============ 认知风格维度 (0-100分，六维雷达图) ============
  `information_processing` INT DEFAULT 50 COMMENT '信息加工（整体加工/序列加工）（0-100分）',
  `knowledge_representation` INT DEFAULT 50 COMMENT '知识表征（视觉倾向/言语倾向）（0-100分）',
  `learning_initiative` INT DEFAULT 50 COMMENT '学习主动性（主动建构/被动接受）（0-100分）',
  `cognitive_control` INT DEFAULT 50 COMMENT '认知控制（反思型/冲动型）（0-100分）',
  `practice_orientation` INT DEFAULT 50 COMMENT '实践导向（工程实践/理论导向）（0-100分）',
  `task_management` INT DEFAULT 50 COMMENT '任务管理（规划型/随机型）（0-100分）',
  `cognitive_style_summary` TEXT DEFAULT NULL COMMENT '认知风格总结描述',
  
  -- ============ 学习投入度维度 ============
  `weekly_study_data` TEXT DEFAULT NULL COMMENT '周学习数据（JSON格式：每日视频学习时长、练习时长、专注度）',
  `avg_daily_study_minutes` DOUBLE DEFAULT 0 COMMENT '日均学习时长（分钟）',
  `avg_concentration` DOUBLE DEFAULT 50 COMMENT '平均专注度（0-100分）',
  
  -- ============ 易错偏好维度 (JSON格式存储) ============
  `error_preferences` TEXT DEFAULT NULL COMMENT '易错类型偏好（JSON数组：包含错误类型、频率、出现率、描述、标签等）',
  `error_type_count` INT DEFAULT 0 COMMENT '偏好错误类型数量',
  `high_frequency_error_count` INT DEFAULT 0 COMMENT '高频次错误数量',
  `avg_error_rate` DOUBLE DEFAULT 0 COMMENT '平均出现率',
  
  -- ============ 知识获取偏好维度 (0-100%) ============
  `text_preference` INT DEFAULT 25 COMMENT '偏文字（0-100%）',
  `image_preference` INT DEFAULT 25 COMMENT '偏图像（0-100%）',
  `example_preference` INT DEFAULT 25 COMMENT '偏示例（0-100%）',
  `video_preference` INT DEFAULT 25 COMMENT '偏视频（0-100%）',
  
  -- ============ 学科兴趣领域维度 ============
  `subject_interests` TEXT DEFAULT NULL COMMENT '学科兴趣数据（JSON格式：包含领域名称、分数、等级、图标等）',
  `interest_summary` TEXT DEFAULT NULL COMMENT '兴趣领域总结描述',
  `recommended_path` TEXT DEFAULT NULL COMMENT '推荐学习路径（JSON格式：包含阶段、标题、描述）',
  
  -- ============ 元数据 ============
  `version` INT DEFAULT 1 COMMENT '版本号（随学习递增）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_id` (`student_id`),
  KEY `idx_major` (`major`),
  KEY `idx_version` (`version`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生画像表 - 多维度学习特征画像';

-- =============================================
-- 索引说明：
-- uk_student_id: 唯一索引，确保每个学生只有一个画像记录
-- idx_major: 按专业查询索引
-- idx_version: 按版本查询索引
-- idx_create_time: 按创建时间查询索引
-- =============================================

-- =============================================
-- 数据迁移脚本（如果已有旧数据）
-- =============================================
-- 如果你的数据库已有旧版表结构，可以使用以下脚本迁移数据：

/*
-- 步骤1: 备份旧表数据
CREATE TABLE student_profile_backup AS SELECT * FROM student_profile;

-- 步骤2: 删除旧表
DROP TABLE IF EXISTS student_profile;

-- 步骤3: 创建新表结构（执行上面的CREATE TABLE语句）

-- 步骤4: 迁移数据（如果有备份数据）
INSERT INTO student_profile (
  student_id, username, nickname, email, major, grade, learning_goal, learning_style, weakness_points, preferred_style,
  concept_clarity, memory_stability, transfer_ability, structural_integrity, knowledge_depth,
  information_processing, knowledge_representation, learning_initiative, 
  cognitive_control, practice_orientation, task_management,
  cognitive_style_summary,
  weekly_study_data, avg_daily_study_minutes, avg_concentration,
  error_preferences, error_type_count, high_frequency_error_count, avg_error_rate,
  text_preference, image_preference, example_preference, video_preference,
  subject_interests, interest_summary, recommended_path,
  version, create_time, update_time
)
SELECT 
  student_id, 
  NULL as username, 
  NULL as nickname, 
  NULL as email,
  major, 
  NULL as grade,
  learning_goal, 
  learning_style,
  weakness_points,
  preferred_style,
  -- 将旧字段映射到新字段（如果存在的话）
  COALESCE(concept_clarity, 50) as concept_clarity,
  COALESCE(memory_stability, 50) as memory_stability,
  COALESCE(transfer_ability, 50) as transfer_ability,
  COALESCE(structural_integrity, 50) as structural_integrity,
  COALESCE(knowledge_depth, 50) as knowledge_depth,
  -- 认知风格维度使用默认值50
  50, 50, 50, 50, 50, 50,
  summary as cognitive_style_summary,
  NULL as weekly_study_data,
  0 as avg_daily_study_minutes,
  50 as avg_concentration,
  error_preferences,
  COALESCE(error_type_count, 0) as error_type_count,
  COALESCE(high_frequency_error_count, 0) as high_frequency_error_count,
  COALESCE(avg_error_rate, 0) as avg_error_rate,
  COALESCE(text_preference, 25) as text_preference,
  COALESCE(image_preference, 25) as image_preference,
  COALESCE(example_preference, 25) as example_preference,
  COALESCE(video_preference, 25) as video_preference,
  NULL as subject_interests,
  summary as interest_summary,
  NULL as recommended_path,
  version, create_time, update_time
FROM student_profile_backup;

-- 步骤5: 验证数据后删除备份表
-- DROP TABLE IF EXISTS student_profile_backup;
*/
