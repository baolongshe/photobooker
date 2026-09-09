CREATE TABLE portfolio_comment (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  portfolio_id BIGINT NOT NULL COMMENT '作品ID',
  user_id BIGINT NOT NULL COMMENT '评论用户ID',
  user_name VARCHAR(64) NOT NULL COMMENT '评论用户昵称',
  content TEXT NOT NULL COMMENT '评论内容',
  parent_id BIGINT DEFAULT NULL COMMENT '父评论ID，支持回复',
  parent_user_name VARCHAR(64) DEFAULT NULL COMMENT '被回复用户昵称',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0正常 1删除',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_portfolio_id (portfolio_id),
  KEY idx_user_id (user_id),
  KEY idx_parent_id (parent_id),
  CONSTRAINT fk_comment_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作品评论表'; 