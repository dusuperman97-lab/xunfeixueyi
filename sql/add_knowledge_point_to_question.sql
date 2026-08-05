-- 为 question 表添加 knowledge_point 字段
-- 执行时间：2026-04-22
-- 说明：修复生成随堂练习题时缺少 knowledge_point 字段的问题

ALTER TABLE `question` 
ADD COLUMN `knowledge_point` VARCHAR(500) DEFAULT NULL COMMENT '考察知识点' AFTER `analysis`;

-- 验证字段是否添加成功
DESCRIBE `question`;
