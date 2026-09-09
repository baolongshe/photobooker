-- 简化版摄影师套餐表结构更新
-- 手动执行每个ALTER语句，如果字段已存在会报错，可以忽略

-- 1. 添加拍摄时长字段
ALTER TABLE `photographer_package` ADD COLUMN `duration` int DEFAULT NULL COMMENT '拍摄时长（小时）' AFTER `price`;

-- 2. 添加精修照片数量字段
ALTER TABLE `photographer_package` ADD COLUMN `photo_count` int DEFAULT NULL COMMENT '精修照片数量' AFTER `duration`;

-- 3. 添加服务详情字段
ALTER TABLE `photographer_package` ADD COLUMN `service_details` text COMMENT '服务详情' AFTER `photo_count`;

-- 4. 添加套餐分类字段
ALTER TABLE `photographer_package` ADD COLUMN `category` varchar(50) DEFAULT NULL COMMENT '套餐分类' AFTER `service_details`;

-- 5. 添加状态字段
ALTER TABLE `photographer_package` ADD COLUMN `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态（0：下架，1：上架）' AFTER `category`;

-- 6. 添加索引（如果已存在会报错，可以忽略）
ALTER TABLE `photographer_package` ADD INDEX `idx_category` (`category`);
ALTER TABLE `photographer_package` ADD INDEX `idx_status` (`status`);
ALTER TABLE `photographer_package` ADD INDEX `idx_price` (`price`);

-- 7. 更新现有数据的分类信息
UPDATE `photographer_package` SET `category` = '写真摄影' WHERE `name` LIKE '%写真%' AND `category` IS NULL;
UPDATE `photographer_package` SET `category` = '婚纱摄影' WHERE `name` LIKE '%婚纱%' AND `category` IS NULL;
UPDATE `photographer_package` SET `category` = '儿童摄影' WHERE `name` LIKE '%儿童%' AND `category` IS NULL;
UPDATE `photographer_package` SET `category` = '儿童摄影' WHERE `name` LIKE '%亲子%' AND `category` IS NULL;
UPDATE `photographer_package` SET `category` = '儿童摄影' WHERE `name` LIKE '%周岁%' AND `category` IS NULL;
UPDATE `photographer_package` SET `category` = '商业摄影' WHERE `name` LIKE '%商业%' AND `category` IS NULL;
UPDATE `photographer_package` SET `category` = '商业摄影' WHERE `name` LIKE '%活动%' AND `category` IS NULL;
UPDATE `photographer_package` SET `category` = '古风摄影' WHERE `name` LIKE '%古风%' AND `category` IS NULL;
UPDATE `photographer_package` SET `category` = '时尚摄影' WHERE `name` LIKE '%时尚%' AND `category` IS NULL;
UPDATE `photographer_package` SET `category` = '时尚摄影' WHERE `name` LIKE '%街拍%' AND `category` IS NULL;
UPDATE `photographer_package` SET `category` = '时尚摄影' WHERE `name` LIKE '%杂志%' AND `category` IS NULL;
UPDATE `photographer_package` SET `category` = '毕业摄影' WHERE `name` LIKE '%毕业%' AND `category` IS NULL;

-- 8. 更新现有数据的拍摄时长
UPDATE `photographer_package` SET `duration` = 2 WHERE `name` LIKE '%基础%' AND `duration` IS NULL;
UPDATE `photographer_package` SET `duration` = 3 WHERE `name` LIKE '%情侣%' AND `duration` IS NULL;
UPDATE `photographer_package` SET `duration` = 3 WHERE `name` LIKE '%亲子%' AND `duration` IS NULL;
UPDATE `photographer_package` SET `duration` = 4 WHERE `name` LIKE '%高级%' AND `duration` IS NULL;
UPDATE `photographer_package` SET `duration` = 6 WHERE `name` LIKE '%婚纱%' AND `duration` IS NULL;
UPDATE `photographer_package` SET `duration` = 8 WHERE `name` LIKE '%豪华%' AND `duration` IS NULL;
UPDATE `photographer_package` SET `duration` = 10 WHERE `name` LIKE '%定制%' AND `duration` IS NULL;

-- 9. 更新现有数据的精修照片数量
UPDATE `photographer_package` SET `photo_count` = 20 WHERE `name` LIKE '%基础%' AND `photo_count` IS NULL;
UPDATE `photographer_package` SET `photo_count` = 25 WHERE `name` LIKE '%儿童%' AND `photo_count` IS NULL;
UPDATE `photographer_package` SET `photo_count` = 25 WHERE `name` LIKE '%毕业%' AND `photo_count` IS NULL;
UPDATE `photographer_package` SET `photo_count` = 30 WHERE `name` LIKE '%情侣%' AND `photo_count` IS NULL;
UPDATE `photographer_package` SET `photo_count` = 35 WHERE `name` LIKE '%亲子%' AND `photo_count` IS NULL;
UPDATE `photographer_package` SET `photo_count` = 40 WHERE `name` LIKE '%高级%' AND `photo_count` IS NULL;
UPDATE `photographer_package` SET `photo_count` = 45 WHERE `name` LIKE '%周岁%' AND `photo_count` IS NULL;
UPDATE `photographer_package` SET `photo_count` = 50 WHERE `name` LIKE '%婚纱基础%' AND `photo_count` IS NULL;
UPDATE `photographer_package` SET `photo_count` = 50 WHERE `name` LIKE '%商业%' AND `photo_count` IS NULL;
UPDATE `photographer_package` SET `photo_count` = 60 WHERE `name` LIKE '%古风%' AND `photo_count` IS NULL;
UPDATE `photographer_package` SET `photo_count` = 80 WHERE `name` LIKE '%豪华%' AND `photo_count` IS NULL;
UPDATE `photographer_package` SET `photo_count` = 100 WHERE `name` LIKE '%定制%' AND `photo_count` IS NULL;

-- 10. 更新现有数据的服务详情
UPDATE `photographer_package` SET `service_details` = CONCAT('包含', `duration`, '小时拍摄，提供', `photo_count`, '张精修照片，', `description`) WHERE `service_details` IS NULL;

-- 11. 验证表结构
DESCRIBE `photographer_package`;

-- 12. 查看更新后的数据
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