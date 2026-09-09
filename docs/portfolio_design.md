# 摄影师作品表设计文档

## 概述

`portfolio` 表用于存储摄影师的作品信息，支持多种作品类型、标签分类、状态管理等功能。

## 表结构

### 主要字段

| 字段名 | 类型 | 说明 | 示例 |
|--------|------|------|------|
| id | bigint | 主键ID，自增 | 1 |
| photographer_id | bigint | 摄影师ID，关联photographer表 | 1 |
| title | varchar(255) | 作品标题 | "浪漫婚纱摄影" |
| description | text | 作品描述 | "这是一组浪漫的婚纱摄影作品..." |
| category | varchar(50) | 作品类型 | "婚纱"、"写真"、"纪实"、"商业" |
| tags | varchar(500) | 作品标签，逗号分隔 | "婚纱,浪漫,甜蜜,幸福" |
| cover_image | varchar(500) | 封面图片URL | "https://example.com/cover1.jpg" |
| image_urls | text | 作品图片URLs，JSON数组格式 | `["url1","url2","url3"]` |
| shooting_date | date | 拍摄时间 | 2024-01-15 |
| shooting_location | varchar(255) | 拍摄地点 | "海边" |
| equipment | varchar(500) | 使用的设备信息 | "Canon EOS R5, 24-70mm f/2.8" |
| status | tinyint | 作品状态 | 0:草稿, 1:已发布, 2:已下架 |
| view_count | int | 浏览量 | 156 |
| like_count | int | 点赞数 | 23 |
| is_featured | tinyint | 是否精选作品 | 0:否, 1:是 |
| sort_weight | int | 排序权重，数字越大越靠前 | 100 |
| create_time | datetime | 创建时间 | 2024-01-15 10:30:00 |
| update_time | datetime | 更新时间 | 2024-01-15 10:30:00 |

## 功能特性

### 1. 作品分类管理
- 支持多种作品类型：婚纱、写真、纪实、商业等
- 标签系统：支持多个标签，便于搜索和分类
- 自定义分类：可根据业务需求扩展

### 2. 图片管理
- 封面图片：每个作品都有独立的封面
- 多图支持：一个作品可以包含多张图片
- JSON格式存储：灵活管理图片URL数组

### 3. 状态管理
- 草稿状态：摄影师可以保存未完成的作品
- 发布状态：作品对外可见
- 下架状态：临时隐藏作品

### 4. 数据统计
- 浏览量统计：记录作品被查看的次数
- 点赞功能：用户可以对作品进行点赞
- 精选作品：摄影师可以设置精选作品

### 5. 排序功能
- 权重排序：通过sort_weight字段控制显示顺序
- 时间排序：支持按创建时间排序
- 热度排序：支持按浏览量、点赞数排序

## API接口

### 基础CRUD操作
- `POST /portfolio` - 创建作品
- `GET /portfolio/{id}` - 查询作品详情
- `PUT /portfolio/{id}` - 更新作品
- `DELETE /portfolio/{id}` - 删除作品

### 查询接口
- `GET /portfolio/photographer/{photographerId}` - 分页查询摄影师作品
- `GET /portfolio/photographer/{photographerId}/featured` - 查询精选作品
- `GET /portfolio/category/{category}` - 按分类查询作品
- `GET /portfolio/search/tag` - 按标签搜索作品

### 互动接口
- `POST /portfolio/{id}/like` - 点赞作品
- `DELETE /portfolio/{id}/like` - 取消点赞
- `PUT /portfolio/{id}/featured` - 设置精选状态

### 管理接口
- `PUT /portfolio/{id}/status` - 更新作品状态
- `PUT /portfolio/{id}/sort` - 更新排序权重

## 数据库索引

为了提高查询性能，设置了以下索引：

```sql
KEY `idx_photographer_id` (`photographer_id`)  -- 摄影师ID索引
KEY `idx_category` (`category`)                -- 分类索引
KEY `idx_status` (`status`)                    -- 状态索引
KEY `idx_is_featured` (`is_featured`)          -- 精选状态索引
KEY `idx_sort_weight` (`sort_weight`)          -- 排序权重索引
KEY `idx_create_time` (`create_time`)          -- 创建时间索引
```

## 外键约束

```sql
CONSTRAINT `fk_portfolio_photographer` 
FOREIGN KEY (`photographer_id`) REFERENCES `photographer` (`id`) 
ON DELETE CASCADE
```

当摄影师被删除时，其所有作品也会被级联删除。

## 前端页面

### 1. 摄影师详情页 (`PhotographerDetail.vue`)
- 展示摄影师的精选作品
- 支持点击跳转到作品详情页
- 显示作品分类、标签、浏览量、点赞数等信息

### 2. 作品详情页 (`PortfolioDetail.vue`)
- 展示单个作品的详细信息
- 支持多图浏览（主图+缩略图）
- 显示作品元数据（拍摄时间、地点、设备等）
- 支持点赞和分享功能

### 3. 摄影师作品管理页 (`PhotographerPortfolio.vue`)
- 摄影师管理自己的作品
- 支持添加、编辑、删除作品
- 支持筛选和分页
- 支持设置精选状态和发布状态

## 使用示例

### 1. 创建作品
```json
POST /portfolio
{
  "photographerId": 1,
  "title": "浪漫婚纱摄影",
  "description": "这是一组浪漫的婚纱摄影作品",
  "category": "婚纱",
  "tags": "婚纱,浪漫,甜蜜",
  "coverImage": "https://example.com/cover.jpg",
  "imageUrls": "[\"https://example.com/img1.jpg\",\"https://example.com/img2.jpg\"]",
  "shootingDate": "2024-01-15",
  "shootingLocation": "海边",
  "equipment": "Canon EOS R5, 24-70mm f/2.8"
}
```

### 2. 查询摄影师作品
```
GET /portfolio/photographer/1?page=1&size=10
```

### 3. 点赞作品
```
POST /portfolio/1/like
```

## 扩展建议

1. **评论功能**：可以添加作品评论表，支持用户对作品进行评论
2. **收藏功能**：可以添加用户收藏表，支持用户收藏喜欢的作品
3. **分享功能**：可以添加分享统计字段，记录作品被分享的次数
4. **版权信息**：可以添加版权相关字段，如版权声明、使用许可等
5. **价格信息**：可以添加价格字段，支持作品销售功能

## 注意事项

1. **图片存储**：建议使用CDN或对象存储服务存储图片，提高访问速度
2. **数据备份**：定期备份作品数据，防止数据丢失
3. **性能优化**：对于大量图片的作品，建议使用懒加载技术
4. **安全考虑**：上传图片时需要进行格式验证和大小限制
5. **SEO优化**：作品标题和描述要有利于搜索引擎优化 