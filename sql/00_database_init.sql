-- =====================================================
-- PhotoBooker 摄影约拍平台数据库初始化脚本
-- 版本: 1.0
-- 创建时间: 2024-01-15
-- 说明: 完整的数据库结构初始化脚本
-- =====================================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS `photo` 
DEFAULT CHARACTER SET utf8mb4 
DEFAULT COLLATE utf8mb4_unicode_ci;

USE `photo`;

-- 设置SQL模式
SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET AUTOCOMMIT = 0;
START TRANSACTION;
SET time_zone = "+00:00";

-- =====================================================
-- 1. 用户表 (user)
-- =====================================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '加密密码',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `gender` tinyint DEFAULT '0' COMMENT '性别（0：女，1：男，2：其他）',
  `birthday` date DEFAULT NULL COMMENT '生日',
  `avatar` varchar(500) DEFAULT NULL COMMENT '头像地址',
  `register_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态（0：禁用，1：正常）',
  `openid` varchar(100) DEFAULT NULL COMMENT '微信登录唯一标识',
  `role` tinyint NOT NULL DEFAULT '0' COMMENT '角色（0：普通用户，1：管理员）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_openid` (`openid`),
  KEY `idx_phone` (`phone`),
  KEY `idx_status` (`status`),
  KEY `idx_role` (`role`),
  KEY `idx_register_time` (`register_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- =====================================================
-- 2. 管理员表 (admin)
-- =====================================================
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '关联user表id',
  `username` varchar(50) NOT NULL COMMENT '管理员账号',
  `password` varchar(100) NOT NULL COMMENT '加密后的密码',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `role` varchar(20) NOT NULL DEFAULT 'ADMIN' COMMENT '角色',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用（0：禁用，1：启用）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_admin_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员表';

-- =====================================================
-- 3. 摄影师表 (photographer)
-- =====================================================
DROP TABLE IF EXISTS `photographer`;
CREATE TABLE `photographer` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '关联用户ID',
  `name` varchar(50) NOT NULL COMMENT '摄影师姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `style` varchar(200) DEFAULT NULL COMMENT '擅长风格',
  `intro` text COMMENT '个人简介',
  `work_years` int DEFAULT '0' COMMENT '工作年限',
  `avatar` varchar(500) DEFAULT NULL COMMENT '头像地址',
  `auth_status` tinyint NOT NULL DEFAULT '0' COMMENT '认证状态（0：未认证，1：已认证，2：认证失败）',
  `available_schedule` text COMMENT '可用档期（JSON格式）',
  `rating` decimal(3,2) DEFAULT '5.00' COMMENT '评分',
  `order_count` int NOT NULL DEFAULT '0' COMMENT '完成订单数',
  `location` varchar(200) DEFAULT NULL COMMENT '地址',
  `latitude` decimal(10,8) DEFAULT NULL COMMENT '纬度',
  `longitude` decimal(11,8) DEFAULT NULL COMMENT '经度',
  `packages` text COMMENT '套餐信息（JSON格式）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_auth_status` (`auth_status`),
  KEY `idx_rating` (`rating`),
  KEY `idx_order_count` (`order_count`),
  KEY `idx_location` (`latitude`,`longitude`),
  CONSTRAINT `fk_photographer_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='摄影师表';

-- =====================================================
-- 4. 摄影师申请表 (application)
-- =====================================================
DROP TABLE IF EXISTS `application`;
CREATE TABLE `application` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '申请人用户ID',
  `real_name` varchar(50) NOT NULL COMMENT '真实姓名',
  `phone` varchar(20) NOT NULL COMMENT '联系电话',
  `intro` text COMMENT '个人简介/申请理由',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态（PENDING：待审核，APPROVED：通过，REJECTED：拒绝）',
  `remark` text COMMENT '管理员审核备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_application_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='摄影师申请表';

-- =====================================================
-- 5. 订单表 (booking_order)
-- =====================================================
DROP TABLE IF EXISTS `booking_order`;
CREATE TABLE `booking_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `photographer_id` bigint NOT NULL COMMENT '摄影师ID',
  `package_name` varchar(100) DEFAULT NULL COMMENT '套餐名称',
  `total_price` decimal(10,2) NOT NULL COMMENT '总价格',
  `shooting_time` datetime NOT NULL COMMENT '拍摄时间',
  `shooting_location` varchar(200) DEFAULT NULL COMMENT '拍摄地点',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '订单状态',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_photographer_id` (`photographer_id`),
  KEY `idx_status` (`status`),
  KEY `idx_shooting_time` (`shooting_time`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_photographer_status` (`photographer_id`,`status`),
  CONSTRAINT `fk_order_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_order_photographer` FOREIGN KEY (`photographer_id`) REFERENCES `photographer` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- =====================================================
-- 6. 作品表 (portfolio)
-- =====================================================
DROP TABLE IF EXISTS `portfolio`;
CREATE TABLE `portfolio` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `photographer_id` bigint NOT NULL COMMENT '摄影师ID',
  `title` varchar(255) NOT NULL COMMENT '作品标题',
  `description` text COMMENT '作品描述',
  `category` varchar(50) DEFAULT NULL COMMENT '作品类型',
  `tags` varchar(500) DEFAULT NULL COMMENT '作品标签（逗号分隔）',
  `cover_image` varchar(500) DEFAULT NULL COMMENT '封面图片URL',
  `image_urls` text COMMENT '作品图片URLs（JSON数组格式）',
  `shooting_date` date DEFAULT NULL COMMENT '拍摄时间',
  `shooting_location` varchar(255) DEFAULT NULL COMMENT '拍摄地点',
  `equipment` varchar(500) DEFAULT NULL COMMENT '使用设备信息',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '作品状态（0：草稿，1：已发布，2：已下架）',
  `view_count` int NOT NULL DEFAULT '0' COMMENT '浏览量',
  `like_count` int NOT NULL DEFAULT '0' COMMENT '点赞数',
  `is_featured` tinyint NOT NULL DEFAULT '0' COMMENT '是否精选作品（0：否，1：是）',
  `sort_weight` int NOT NULL DEFAULT '0' COMMENT '排序权重',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_photographer_id` (`photographer_id`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`),
  KEY `idx_is_featured` (`is_featured`),
  KEY `idx_sort_weight` (`sort_weight`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_view_count` (`view_count`),
  KEY `idx_like_count` (`like_count`),
  CONSTRAINT `fk_portfolio_photographer` FOREIGN KEY (`photographer_id`) REFERENCES `photographer` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='摄影师作品表';

-- =====================================================
-- 7. 作品评论表 (portfolio_comment)
-- =====================================================
DROP TABLE IF EXISTS `portfolio_comment`;
CREATE TABLE `portfolio_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `portfolio_id` bigint NOT NULL COMMENT '作品ID',
  `user_id` bigint NOT NULL COMMENT '评论用户ID',
  `user_name` varchar(64) NOT NULL COMMENT '评论用户昵称',
  `content` text NOT NULL COMMENT '评论内容',
  `parent_id` bigint DEFAULT NULL COMMENT '父评论ID',
  `parent_user_name` varchar(64) DEFAULT NULL COMMENT '被回复用户昵称',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态（0：正常，1：删除）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_portfolio_id` (`portfolio_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_comment_portfolio` FOREIGN KEY (`portfolio_id`) REFERENCES `portfolio` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作品评论表';

-- =====================================================
-- 8. 消息表 (message)
-- =====================================================
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id` varchar(100) DEFAULT NULL COMMENT '会话ID',
  `sender_id` bigint NOT NULL COMMENT '发送者ID',
  `sender_role` varchar(20) NOT NULL COMMENT '发送者角色',
  `receiver_id` bigint NOT NULL COMMENT '接收者ID',
  `receiver_role` varchar(20) NOT NULL COMMENT '接收者角色',
  `order_id` bigint DEFAULT NULL COMMENT '关联订单ID',
  `content` text COMMENT '文本内容',
  `type` varchar(20) NOT NULL DEFAULT 'text' COMMENT '消息类型（text/image/file）',
  `file_url` varchar(500) DEFAULT NULL COMMENT '图片/文件URL',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_sender_id` (`sender_id`),
  KEY `idx_receiver_id` (`receiver_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_sender_receiver` (`sender_id`,`receiver_id`),
  CONSTRAINT `fk_message_order` FOREIGN KEY (`order_id`) REFERENCES `booking_order` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- =====================================================
-- 9. 地址表 (address)
-- =====================================================
DROP TABLE IF EXISTS `address`;
CREATE TABLE `address` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `receiver` varchar(50) NOT NULL COMMENT '收件人',
  `phone` varchar(20) NOT NULL COMMENT '联系电话',
  `province` varchar(50) NOT NULL COMMENT '省份',
  `city` varchar(50) NOT NULL COMMENT '城市',
  `district` varchar(50) NOT NULL COMMENT '区县',
  `detail` varchar(200) NOT NULL COMMENT '详细地址',
  `is_default` tinyint NOT NULL DEFAULT '0' COMMENT '是否默认地址（0：否，1：是）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_is_default` (`is_default`),
  CONSTRAINT `fk_address_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='地址表';

-- =====================================================
-- 10. 头像表 (avatar)
-- =====================================================
DROP TABLE IF EXISTS `avatar`;
CREATE TABLE `avatar` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `url` varchar(500) NOT NULL COMMENT '头像URL',
  `is_current` tinyint NOT NULL DEFAULT '0' COMMENT '是否当前头像（0：否，1：是）',
  `upload_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态（0：禁用，1：正常）',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_is_current` (`is_current`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_avatar_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='头像表';

-- =====================================================
-- 11. 公告表 (announcement)
-- =====================================================
DROP TABLE IF EXISTS `announcement`;
CREATE TABLE `announcement` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(200) NOT NULL COMMENT '公告标题',
  `content` text NOT NULL COMMENT '公告内容',
  `time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态（0：禁用，1：启用）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_time` (`time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告表';

COMMIT;
