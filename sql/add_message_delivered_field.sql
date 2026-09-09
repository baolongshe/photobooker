-- 为message表添加is_delivered字段
ALTER TABLE `message` 
ADD COLUMN `is_delivered` tinyint NOT NULL DEFAULT '0' COMMENT '是否已送达（0：未送达，1：已送达）' 
AFTER `create_time`;

-- 添加索引以提高查询性能
ALTER TABLE `message` 
ADD INDEX `idx_is_delivered` (`is_delivered`); 