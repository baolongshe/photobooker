-- 更新摄影师套餐表结构
-- 添加缺失的字段以匹配训练数据

-- 1. 首先创建新的表结构（如果表不存在）
CREATE TABLE IF NOT EXISTS `photographer_package` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `photographer_id` bigint NOT NULL COMMENT '摄影师ID',
  `name` varchar(100) NOT NULL COMMENT '套餐名称',
  `description` text COMMENT '套餐描述',
  `price` decimal(10,2) NOT NULL COMMENT '套餐价格',
  `duration` int DEFAULT NULL COMMENT '拍摄时长（小时）',
  `photo_count` int DEFAULT NULL COMMENT '精修照片数量',
  `service_details` text COMMENT '服务详情',
  `category` varchar(50) DEFAULT NULL COMMENT '套餐分类',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态（0：下架，1：上架）',
  `is_default` tinyint NOT NULL DEFAULT '0' COMMENT '是否默认套餐（0：否，1：是）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_photographer_id` (`photographer_id`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`),
  KEY `idx_is_default` (`is_default`),
  KEY `idx_price` (`price`),
  CONSTRAINT `fk_package_photographer` FOREIGN KEY (`photographer_id`) REFERENCES `photographer` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='摄影师套餐表';

-- 2. 如果表已存在，添加缺失的字段（使用错误处理）
-- 添加拍摄时长字段
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'photographer_package' 
     AND COLUMN_NAME = 'duration') = 0,
    'ALTER TABLE `photographer_package` ADD COLUMN `duration` int DEFAULT NULL COMMENT ''拍摄时长（小时）'' AFTER `price`',
    'SELECT ''duration column already exists'' as message'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加精修照片数量字段
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'photographer_package' 
     AND COLUMN_NAME = 'photo_count') = 0,
    'ALTER TABLE `photographer_package` ADD COLUMN `photo_count` int DEFAULT NULL COMMENT ''精修照片数量'' AFTER `duration`',
    'SELECT ''photo_count column already exists'' as message'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加服务详情字段
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'photographer_package' 
     AND COLUMN_NAME = 'service_details') = 0,
    'ALTER TABLE `photographer_package` ADD COLUMN `service_details` text COMMENT ''服务详情'' AFTER `photo_count`',
    'SELECT ''service_details column already exists'' as message'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加套餐分类字段
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'photographer_package' 
     AND COLUMN_NAME = 'category') = 0,
    'ALTER TABLE `photographer_package` ADD COLUMN `category` varchar(50) DEFAULT NULL COMMENT ''套餐分类'' AFTER `service_details`',
    'SELECT ''category column already exists'' as message'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加状态字段
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'photographer_package' 
     AND COLUMN_NAME = 'status') = 0,
    'ALTER TABLE `photographer_package` ADD COLUMN `status` tinyint NOT NULL DEFAULT ''1'' COMMENT ''状态（0：下架，1：上架）'' AFTER `category`',
    'SELECT ''status column already exists'' as message'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 添加索引（如果不存在）
-- 注意：MySQL不支持IF NOT EXISTS for INDEX，所以这里只是添加索引，如果已存在会报错但不会影响数据
ALTER TABLE `photographer_package` 
ADD INDEX `idx_category` (`category`);

ALTER TABLE `photographer_package` 
ADD INDEX `idx_status` (`status`);

ALTER TABLE `photographer_package` 
ADD INDEX `idx_price` (`price`);

-- 4. 更新现有数据的分类信息（基于套餐名称推断）
UPDATE `photographer_package` SET `category` = '写真摄影' WHERE `name` LIKE '%写真%' AND `category` IS NULL;
UPDATE `photographer_package` SET `category` = '婚纱摄影' WHERE `name` LIKE '%婚纱%' AND `category` IS NULL;
UPDATE `photographer_package` SET `category` = '儿童摄影' WHERE `name` LIKE '%儿童%' OR `name` LIKE '%亲子%' OR `name` LIKE '%周岁%' AND `category` IS NULL;
UPDATE `photographer_package` SET `category` = '商业摄影' WHERE `name` LIKE '%商业%' OR `name` LIKE '%活动%' AND `category` IS NULL;
UPDATE `photographer_package` SET `category` = '古风摄影' WHERE `name` LIKE '%古风%' AND `category` IS NULL;
UPDATE `photographer_package` SET `category` = '时尚摄影' WHERE `name` LIKE '%时尚%' OR `name` LIKE '%街拍%' OR `name` LIKE '%杂志%' AND `category` IS NULL;
UPDATE `photographer_package` SET `category` = '毕业摄影' WHERE `name` LIKE '%毕业%' AND `category` IS NULL;

-- 5. 更新现有数据的拍摄时长（基于套餐名称推断）
UPDATE `photographer_package` SET `duration` = 2 WHERE `name` LIKE '%基础%' OR `name` LIKE '%2小时%' AND `duration` IS NULL;
UPDATE `photographer_package` SET `duration` = 3 WHERE `name` LIKE '%情侣%' OR `name` LIKE '%亲子%' OR `name` LIKE '%3小时%' AND `duration` IS NULL;
UPDATE `photographer_package` SET `duration` = 4 WHERE `name` LIKE '%高级%' OR `name` LIKE '%4小时%' AND `duration` IS NULL;
UPDATE `photographer_package` SET `duration` = 6 WHERE `name` LIKE '%婚纱%' OR `name` LIKE '%6小时%' AND `duration` IS NULL;
UPDATE `photographer_package` SET `duration` = 8 WHERE `name` LIKE '%豪华%' OR `name` LIKE '%8小时%' AND `duration` IS NULL;
UPDATE `photographer_package` SET `duration` = 10 WHERE `name` LIKE '%定制%' OR `name` LIKE '%10小时%' AND `duration` IS NULL;

-- 6. 更新现有数据的精修照片数量（基于套餐名称推断）
UPDATE `photographer_package` SET `photo_count` = 20 WHERE `name` LIKE '%基础%' OR `name` LIKE '%20张%' AND `photo_count` IS NULL;
UPDATE `photographer_package` SET `photo_count` = 25 WHERE `name` LIKE '%儿童%' OR `name` LIKE '%毕业%' OR `name` LIKE '%25张%' AND `photo_count` IS NULL;
UPDATE `photographer_package` SET `photo_count` = 30 WHERE `name` LIKE '%情侣%' OR `name` LIKE '%30张%' AND `photo_count` IS NULL;
UPDATE `photographer_package` SET `photo_count` = 35 WHERE `name` LIKE '%亲子%' OR `name` LIKE '%35张%' AND `photo_count` IS NULL;
UPDATE `photographer_package` SET `photo_count` = 40 WHERE `name` LIKE '%高级%' OR `name` LIKE '%40张%' AND `photo_count` IS NULL;
UPDATE `photographer_package` SET `photo_count` = 45 WHERE `name` LIKE '%周岁%' OR `name` LIKE '%45张%' AND `photo_count` IS NULL;
UPDATE `photographer_package` SET `photo_count` = 50 WHERE `name` LIKE '%婚纱基础%' OR `name` LIKE '%商业%' OR `name` LIKE '%50张%' AND `photo_count` IS NULL;
UPDATE `photographer_package` SET `photo_count` = 60 WHERE `name` LIKE '%古风%' OR `name` LIKE '%60张%' AND `photo_count` IS NULL;
UPDATE `photographer_package` SET `photo_count` = 80 WHERE `name` LIKE '%豪华%' OR `name` LIKE '%80张%' AND `photo_count` IS NULL;
UPDATE `photographer_package` SET `photo_count` = 100 WHERE `name` LIKE '%定制%' OR `name` LIKE '%100张%' AND `photo_count` IS NULL;

-- 7. 更新现有数据的服务详情（基于套餐名称和描述推断）
UPDATE `photographer_package` SET `service_details` = CONCAT('包含', `duration`, '小时拍摄，提供', `photo_count`, '张精修照片，', `description`) WHERE `service_details` IS NULL;

-- 8. 验证表结构
DESCRIBE `photographer_package`;

-- 9. 查看更新后的数据
SELECT 
    id,
    photographer_id,
    name,
    price,
    duration,
    photo_count,
    category,
    status,
    is_default
FROM `photographer_package`
ORDER BY photographer_id, is_default DESC, price ASC; 