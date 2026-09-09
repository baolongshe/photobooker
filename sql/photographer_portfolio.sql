-- 摄影师作品表
CREATE TABLE `portfolio` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `photographer_id` bigint NOT NULL COMMENT '摄影师ID',
  `title` varchar(255) NOT NULL COMMENT '作品标题',
  `description` text COMMENT '作品描述',
  `category` varchar(50) DEFAULT NULL COMMENT '作品类型（如：婚纱、写真、纪实、商业等）',
  `tags` varchar(500) DEFAULT NULL COMMENT '作品标签（多个标签用逗号分隔）',
  `cover_image` varchar(500) DEFAULT NULL COMMENT '封面图片URL',
  `image_urls` text COMMENT '作品图片URLs（JSON数组格式存储多张图片）',
  `shooting_date` date DEFAULT NULL COMMENT '拍摄时间',
  `shooting_location` varchar(255) DEFAULT NULL COMMENT '拍摄地点',
  `equipment` varchar(500) DEFAULT NULL COMMENT '使用的设备信息',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '作品状态（0：草稿，1：已发布，2：已下架）',
  `view_count` int NOT NULL DEFAULT '0' COMMENT '浏览量',
  `like_count` int NOT NULL DEFAULT '0' COMMENT '点赞数',
  `is_featured` tinyint NOT NULL DEFAULT '0' COMMENT '是否设为精选作品（0：否，1：是）',
  `sort_weight` int NOT NULL DEFAULT '0' COMMENT '排序权重（数字越大越靠前）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_photographer_id` (`photographer_id`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`),
  KEY `idx_is_featured` (`is_featured`),
  KEY `idx_sort_weight` (`sort_weight`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_portfolio_photographer` FOREIGN KEY (`photographer_id`) REFERENCES `photographer` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='摄影师作品表';

-- 插入示例数据
INSERT INTO `portfolio` (
  `photographer_id`, 
  `title`, 
  `description`, 
  `category`, 
  `tags`, 
  `cover_image`, 
  `image_urls`, 
  `shooting_date`, 
  `shooting_location`, 
  `equipment`, 
  `status`, 
  `view_count`, 
  `like_count`, 
  `is_featured`, 
  `sort_weight`
) VALUES 
(1, '浪漫婚纱摄影', '这是一组浪漫的婚纱摄影作品，展现了新人的甜蜜与幸福。', '婚纱', '婚纱,浪漫,甜蜜,幸福', 'https://images.unsplash.com/photo-1542038784456-1ea8e935640e?w=400&h=300&fit=crop', '["https://images.unsplash.com/photo-1542038784456-1ea8e935640e?w=800&h=600&fit=crop","https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=800&h=600&fit=crop","https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?w=800&h=600&fit=crop"]', '2024-01-15', '海边', 'Canon EOS R5, 24-70mm f/2.8', 1, 156, 23, 1, 100),
(1, '时尚写真', '现代时尚写真，展现都市女性的魅力与自信。', '写真', '时尚,都市,女性,魅力', 'https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=400&h=300&fit=crop', '["https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=800&h=600&fit=crop","https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=800&h=600&fit=crop"]', '2024-02-20', '摄影棚', 'Sony A7R IV, 85mm f/1.4', 1, 89, 15, 0, 80),
(2, '纪实摄影', '记录生活中的真实瞬间，展现人性的温暖。', '纪实', '纪实,生活,真实,温暖', 'https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?w=400&h=300&fit=crop', '["https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?w=800&h=600&fit=crop","https://images.unsplash.com/photo-1542038784456-1ea8e935640e?w=800&h=600&fit=crop","https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=800&h=600&fit=crop","https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=800&h=600&fit=crop"]', '2024-03-10', '街头', 'Leica M10, 35mm f/2', 1, 234, 45, 1, 90),
(2, '商业摄影', '专业商业摄影，为品牌提供高质量的视觉素材。', '商业', '商业,专业,品牌,高质量', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&h=300&fit=crop', '["https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=800&h=600&fit=crop","https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=800&h=600&fit=crop"]', '2024-04-05', '商业区', 'Nikon Z9, 70-200mm f/2.8', 1, 67, 12, 0, 70); 