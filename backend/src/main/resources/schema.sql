-- 创建数据库
CREATE DATABASE IF NOT EXISTS marx_exercise DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE marx_exercise;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码(加密)',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `role` VARCHAR(20) DEFAULT 'USER' COMMENT '角色: USER/ADMIN',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 题库表
CREATE TABLE IF NOT EXISTS `question_bank` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '题库ID',
    `name` VARCHAR(100) NOT NULL COMMENT '题库名称',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '题库描述',
    `file_name` VARCHAR(255) NOT NULL COMMENT '文件名',
    `source_url` VARCHAR(500) DEFAULT NULL COMMENT '来源URL',
    `question_count` INT DEFAULT 0 COMMENT '题目总数',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库表';

-- 题目表
CREATE TABLE IF NOT EXISTS `question` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '题目ID',
    `bank_id` BIGINT NOT NULL COMMENT '所属题库ID',
    `type` VARCHAR(20) NOT NULL COMMENT '题型: single/multiple/judgment',
    `stem` TEXT NOT NULL COMMENT '题干',
    `options` JSON DEFAULT NULL COMMENT '选项(JSON数组)',
    `answer` VARCHAR(255) NOT NULL COMMENT '正确答案',
    `source` VARCHAR(100) DEFAULT NULL COMMENT '题目来源',
    `hash` VARCHAR(64) NOT NULL COMMENT '题目内容哈希，用于去重',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY `uk_bank_hash` (`bank_id`, `hash`),
    INDEX `idx_type` (`type`),
    INDEX `idx_bank_id` (`bank_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目表';

-- 用户答题记录表
CREATE TABLE IF NOT EXISTS `user_answer` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `question_id` BIGINT NOT NULL COMMENT '题目ID',
    `bank_id` BIGINT NOT NULL COMMENT '题库ID',
    `question_type` VARCHAR(20) NOT NULL COMMENT '题型',
    `selected_answer` TEXT COMMENT '用户选择的答案',
    `is_correct` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否正确: 0-错误, 1-正确',
    `round_num` INT NOT NULL DEFAULT 1 COMMENT '第几轮答题',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '答题时间',
    INDEX `idx_user_round` (`user_id`, `bank_id`, `question_type`, `round_num`),
    INDEX `idx_user_question` (`user_id`, `question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户答题记录表';

-- 用户进度表
CREATE TABLE IF NOT EXISTS `user_progress` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '进度ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `bank_id` BIGINT NOT NULL COMMENT '题库ID',
    `question_type` VARCHAR(20) NOT NULL COMMENT '题型',
    `round_num` INT NOT NULL DEFAULT 1 COMMENT '当前轮次',
    `total_count` INT NOT NULL DEFAULT 0 COMMENT '总题数',
    `answered_count` INT NOT NULL DEFAULT 0 COMMENT '已答题数',
    `correct_count` INT NOT NULL DEFAULT 0 COMMENT '正确数',
    `status` VARCHAR(20) DEFAULT 'in_progress' COMMENT '状态: in_progress/completed',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_user_bank_type_round` (`user_id`, `bank_id`, `question_type`, `round_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户进度表';

-- 错题表
CREATE TABLE IF NOT EXISTS `wrong_question` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '错题ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `question_id` BIGINT NOT NULL COMMENT '题目ID',
    `bank_id` BIGINT NOT NULL COMMENT '题库ID',
    `question_type` VARCHAR(20) NOT NULL COMMENT '题型',
    `wrong_count` INT NOT NULL DEFAULT 1 COMMENT '错误次数',
    `last_wrong_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最近错误时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY `uk_user_question` (`user_id`, `question_id`),
    INDEX `idx_user_bank_type` (`user_id`, `bank_id`, `question_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题表';
