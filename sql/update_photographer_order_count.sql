-- 更新摄影师完成订单数的迁移脚本
-- 这个脚本会为每个摄影师计算并更新其完成订单数

-- 首先确保orderCount字段存在
ALTER TABLE photographer ADD COLUMN IF NOT EXISTS order_count INT DEFAULT 0 COMMENT '完成订单数';

-- 更新所有摄影师的完成订单数
UPDATE photographer p 
SET order_count = (
    SELECT COUNT(*) 
    FROM booking_order bo 
    WHERE bo.photographer_id = p.id 
    AND bo.status = 'COMPLETED'
);

-- 为没有订单的摄影师设置默认值
UPDATE photographer SET order_count = 0 WHERE order_count IS NULL;

-- 添加索引以提高查询性能
CREATE INDEX IF NOT EXISTS idx_photographer_order_count ON photographer(order_count);
CREATE INDEX IF NOT EXISTS idx_order_photographer_status ON booking_order(photographer_id, status); 