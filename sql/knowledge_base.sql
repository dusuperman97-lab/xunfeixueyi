-- 个人知识库表
CREATE TABLE IF NOT EXISTS `knowledge_base` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `student_id` BIGINT NOT NULL COMMENT '用户ID',
    `name` VARCHAR(100) NOT NULL COMMENT '知识库名称',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '知识库描述',
    `document_count` INT DEFAULT 0 COMMENT '文档数量',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `pinecone_namespace` VARCHAR(100) DEFAULT NULL COMMENT 'Pinecone向量数据库命名空间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_student_id` (`student_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个人知识库表';

-- 知识文档表
CREATE TABLE IF NOT EXISTS `knowledge_document` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `kb_id` BIGINT NOT NULL COMMENT '知识库ID',
    `student_id` BIGINT NOT NULL COMMENT '用户ID',
    `file_name` VARCHAR(200) NOT NULL COMMENT '文件名称',
    `file_path` VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    `file_size` BIGINT DEFAULT 0 COMMENT '文件大小（字节）',
    `file_type` VARCHAR(50) DEFAULT NULL COMMENT '文件类型：pdf, docx, txt, md',
    `content_summary` TEXT DEFAULT NULL COMMENT '内容摘要',
    `segment_count` INT DEFAULT 0 COMMENT '文档分片数量',
    `embedding_status` TINYINT DEFAULT 0 COMMENT '向量化状态：0-未处理，1-处理中，2-已完成，3-失败',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-删除，1-正常',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_kb_id` (`kb_id`),
    INDEX `idx_student_id` (`student_id`),
    INDEX `idx_embedding_status` (`embedding_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识文档表';
