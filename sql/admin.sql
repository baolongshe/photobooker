-- 管理员表
CREATE TABLE IF NOT EXISTS admin (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '关联user表id',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '管理员账号',
    password VARCHAR(100) NOT NULL COMMENT '加密后的密码',
    real_name VARCHAR(50) COMMENT '真实姓名',
    phone VARCHAR(20) COMMENT '联系电话',
    role VARCHAR(20) DEFAULT 'ADMIN' COMMENT '角色',
    status TINYINT DEFAULT 1 COMMENT '是否启用（0禁用 1启用）',
    CONSTRAINT fk_admin_user FOREIGN KEY (user_id) REFERENCES user(id)
);

-- 示例用户（如user表无数据请先插入）
INSERT INTO user (id, username, password, real_name, phone, gender, status, role)
VALUES (1001, 'adminuser', '$2a$10$7QJ8QwQwQwQwQwQwQwQwQeQwQwQwQwQwQwQwQwQwQwQwQwQwQwQw', '超级管理员', '13800000000', 1, 1, 1)
ON DUPLICATE KEY UPDATE username=username;

-- 示例管理员
INSERT INTO admin (user_id, username, password, real_name, phone, role, status)
VALUES (1001, 'admin', '$2a$10$7QJ8QwQwQwQwQwQwQwQwQeQwQwQwQwQwQwQwQwQwQwQwQwQwQw', '超级管理员', '13800000000', 'ADMIN', 1)
ON DUPLICATE KEY UPDATE username=username; 