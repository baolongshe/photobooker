# 完成订单数功能实现说明

## 功能概述

本功能实现了摄影师完成订单数的动态统计和展示，包括：

1. 后端动态计算摄影师的完成订单数
2. 前端在相关页面展示完成订单数
3. 订单完成时自动更新摄影师的订单数
4. 管理员可以查看和管理订单

## 实现细节

### 后端实现

#### 1. 数据库层面
- 在 `photographer` 表中已有 `order_count` 字段
- 添加了统计完成订单数的SQL查询
- 创建了相关索引以提高查询性能

#### 2. 服务层
- `OrderMapper`: 添加了 `countCompletedOrdersByPhotographerId` 方法
- `OrderService`: 添加了 `getCompletedOrderCount` 和 `getAllOrders` 方法
- `OrderServiceImpl`: 实现了动态计算完成订单数的逻辑
- `PhotographerService`: 在获取摄影师信息时动态计算订单数

#### 3. 控制器层
- `OrderController`: 添加了完成订单和获取所有订单的接口
- `PhotographerController`: 移除了硬编码的订单数设置

### 前端实现

#### 1. 摄影师详情页面 (`PhotographerDetail.vue`)
- 展示摄影师的完成订单数
- 数据由后端动态计算

#### 2. 订单详情页面 (`OrderDetail.vue`)
- 添加了完成订单的按钮（仅摄影师可见）
- 订单完成后自动刷新页面

#### 3. 摄影师订单管理页面 (`PhotographerOrders.vue`)
- 添加了完成订单的功能
- 支持批量操作和状态管理

#### 4. 管理员页面 (`Admin.vue`)
- 添加了订单管理功能
- 显示订单总数和完成订单数统计
- 支持订单状态筛选和管理

## API接口

### 1. 完成订单
```
PUT /orders/{id}/complete
```

### 2. 获取所有订单
```
GET /orders/list
```

### 3. 获取摄影师完成订单数
```
GET /orders/stats/photographer/{photographerId}
```

## 数据库迁移

运行以下SQL脚本更新现有数据：

```sql
-- 更新摄影师完成订单数
UPDATE photographer p 
SET order_count = (
    SELECT COUNT(*) 
    FROM booking_order bo 
    WHERE bo.photographer_id = p.id 
    AND bo.status = 'COMPLETED'
);

-- 为没有订单的摄影师设置默认值
UPDATE photographer SET order_count = 0 WHERE order_count IS NULL;
```

## 使用说明

### 摄影师
1. 在摄影师详情页面可以看到自己的完成订单数
2. 在订单管理页面可以标记订单为完成
3. 完成订单后，订单数会自动更新

### 用户
1. 在摄影师详情页面可以看到摄影师的完成订单数
2. 在订单详情页面可以看到订单状态变化

### 管理员
1. 在管理后台可以看到订单总数和完成订单数统计
2. 在订单管理页面可以查看所有订单
3. 可以手动完成订单或查看订单详情

## 注意事项

1. 完成订单数只统计状态为 `COMPLETED` 的订单
2. 订单数会在订单状态变更时自动更新
3. 前端展示的数据由后端实时计算，确保数据准确性
4. 建议定期运行数据库迁移脚本以保持数据一致性 