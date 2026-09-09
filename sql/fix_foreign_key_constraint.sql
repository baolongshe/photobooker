-- 修复摄影师套餐表的外键约束，添加级联删除
-- 执行此脚本后，删除摄影师时会自动删除该摄影师的所有套餐记录

-- 1. 删除现有的外键约束
ALTER TABLE `photographer_package` 
DROP FOREIGN KEY `photographer_package_photographer_id_fk`;

-- 2. 添加新的外键约束，设置 ON DELETE CASCADE
ALTER TABLE `photographer_package` 
ADD CONSTRAINT `fk_package_photographer` 
FOREIGN KEY (`photographer_id`) 
REFERENCES `photographer` (`id`) 
ON DELETE CASCADE;

-- 3. 验证外键约束已创建
-- SHOW CREATE TABLE photographer_package;

-- 注意事项：
-- 1. 执行此脚本前请确保已备份数据
-- 2. 执行后，删除摄影师时会自动级联删除相关套餐
-- 3. 如果外键约束名称不同，请相应修改
