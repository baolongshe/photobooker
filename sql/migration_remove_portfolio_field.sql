-- 数据库迁移脚本：从摄影师表中移除portfolio字段
-- 执行时间：2024年
-- 说明：由于作品信息已经迁移到独立的portfolio表中，需要从photographer表中移除portfolio字段

-- 1. 备份现有数据（可选，建议在执行前备份）
-- CREATE TABLE photographer_backup AS SELECT * FROM photographer;

-- 2. 从photographer表中移除portfolio字段
ALTER TABLE `photographer` DROP COLUMN `portfolio`;

-- 3. 验证字段已移除
-- DESCRIBE photographer;

-- 4. 如果需要恢复，可以使用以下命令（谨慎使用）
-- ALTER TABLE `photographer` ADD COLUMN `portfolio` text COMMENT '作品集（JSON格式存储）';

-- 注意事项：
-- 1. 执行前请确保portfolio表已经创建并包含所有作品数据
-- 2. 建议在执行前备份photographer表
-- 3. 确保前端代码已经更新，不再依赖photographer表中的portfolio字段
-- 4. 如果有其他代码引用此字段，需要相应更新 