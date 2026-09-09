-- =====================================================
-- 售后服务表 (after_sales_service)
-- 用于管理退款、投诉、重新预约等售后服务
-- =====================================================

USE `photo`;

DROP TABLE IF EXISTS `after_sales_service`;
CREATE TABLE `after_sales_service` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id` bigint NOT NULL COMMENT '关联订单ID',
  `user_id` bigint NOT NULL COMMENT '申请用户ID',
  `photographer_id` bigint DEFAULT NULL COMMENT '摄影师ID',
  
  -- 服务类型和状态
  `service_type` varchar(50) NOT NULL COMMENT '服务类型（REFUND：退款，RESCHEDULE：重新预约，COMPLAINT：投诉建议，ADDITIONAL：加选服务）',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态（PENDING：待处理，APPROVED：已批准，REJECTED：已拒绝，PROCESSING：处理中，COMPLETED：已完成，CANCELLED：已撤销）',
  
  -- JSON字段，存储灵活的服务数据
  `service_data` json DEFAULT NULL COMMENT '服务详细数据（JSON格式）',
  
  -- 精修照片相关
  `photo_numbers` varchar(500) DEFAULT NULL COMMENT '需要精修的照片编号（逗号分隔）',
  `refine_requirements` text COMMENT '精修要求说明',
  
  -- 重新预约相关
  `expected_time` datetime DEFAULT NULL COMMENT '期望的重新拍摄时间',
  `reschedule_reason` text COMMENT '重新预约原因',
  
  -- 退款相关
  `refund_reason` text COMMENT '退款原因',
  `refund_amount` decimal(10,2) DEFAULT NULL COMMENT '退款金额',
  
  -- 投诉建议相关
  `complaint_type` varchar(50) DEFAULT NULL COMMENT '投诉类型（attitude：服务态度，quality：服务质量，delay：时间延误，other：其他）',
  
  -- 加选服务相关
  `additional_items` json DEFAULT NULL COMMENT '加选项目（JSON格式）',
  `additional_price` decimal(10,2) DEFAULT NULL COMMENT '加选服务价格',
  
  -- 通用字段
  `description` text COMMENT '问题描述',
  `contact_phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `evidence_images` json DEFAULT NULL COMMENT '凭证图片URL数组',
  
  -- 处理记录
  `handler_id` bigint DEFAULT NULL COMMENT '处理人ID（管理员/客服）',
  `handler_response` text COMMENT '处理回复',
  `process_notes` text COMMENT '处理备注',
  
  -- 时间戳
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `handle_time` datetime DEFAULT NULL COMMENT '处理完成时间',
  
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_photographer_id` (`photographer_id`),
  KEY `idx_service_type` (`service_type`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_after_sales_order` FOREIGN KEY (`order_id`) REFERENCES `booking_order` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_after_sales_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_after_sales_photographer` FOREIGN KEY (`photographer_id`) REFERENCES `photographer` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='售后服务表';
