# 退款功能实现说明

## 概述

本文档说明了 PhotoBooker 摄影约拍平台的退款功能实现细节和使用方法。

## 功能特性

1. **用户端**：
   - 提交退款申请
   - 查看退款申请状态
   - 撤销待处理的退款申请
   
2. **管理端**：
   - 查看所有退款申请列表
   - 批准/拒绝退款申请
   - 查看退款申请详情和凭证
   - 处理退款并调用支付宝退款接口

3. **系统自动处理**：
   - 批准后自动更新订单状态为已取消
   - 调用支付宝退款接口进行实际退款
   - 记录处理人和处理时间

## 数据库表结构

### after_sales_service 表

```sql
-- 执行以下SQL脚本创建售后服务表
mysql -u root -p photo < sql/after_sales_service.sql
```

主要字段：
- `service_type`: 服务类型（REFUND：退款）
- `status`: 状态（PENDING/APPROVED/REJECTED/PROCESSING/COMPLETED/CANCELLED）
- `refund_amount`: 退款金额
- `refund_reason`: 退款原因
- `handler_response`: 处理回复

## 后端实现

### 1. 枚举类

**ServiceStatus.java** - 售后服务状态枚举
- PENDING: 待处理
- APPROVED: 已批准
- REJECTED: 已拒绝
- PROCESSING: 处理中
- COMPLETED: 已完成
- CANCELLED: 已撤销

### 2. Service层

**OrderService** 新增方法：
```java
boolean handleRefundOrder(Long orderId);  // 处理退款后的订单状态更新
```

**AfterSalesServiceService** 新增方法：
```java
boolean approveRefund(Long serviceId, Long handlerId);  // 批准退款
boolean rejectRefund(Long serviceId, Long handlerId, String reason);  // 拒绝退款
boolean cancelService(Long serviceId, Long userId);  // 撤销售后申请
```

### 3. Controller层

**AliPayController** 新增接口：
```java
POST /pay/refund  // 支付宝退款接口
参数：orderId, refundAmount, refundReason(可选)
```

**AfterSalesController** 新增接口：
```java
GET  /after-sales/list              // 获取所有退款申请（管理员）
POST /after-sales/{id}/approve-refund  // 批准退款
POST /after-sales/{id}/reject-refund   // 拒绝退款
POST /after-sales/{id}/cancel          // 撤销售后申请
```

## 前端实现

### 1. 用户端 - AfterSalesService.vue

位置：`test/src/views/AfterSalesService.vue`

功能：
- 选择"退款申请"服务类型
- 填写退款原因和详细描述
- 上传凭证图片（最多5张）
- 提交退款申请
- 查看历史记录和状态
- 撤销待处理的申请

### 2. 管理端 - RefundManagement.vue

位置：`test/src/views/admin/RefundManagement.vue`

功能：
- 按状态筛选退款申请
- 查看退款申请详细信息
- 批准退款（调用支付宝退款接口）
- 拒绝退款（需填写拒绝原因）
- 查看凭证图片

## 使用流程

### 用户申请退款

1. 用户在订单详情页点击"售后服务"按钮
2. 选择"退款申请"服务类型
3. 填写退款原因、详细描述
4. 上传相关凭证图片（可选）
5. 填写联系电话
6. 提交申请

### 管理员处理退款

1. 登录管理后台，进入"退款管理"页面
2. 查看所有待处理的退款申请
3. 点击"详情"查看完整信息和凭证
4. 选择"批准"或"拒绝"：
   - **批准**：系统自动调用支付宝退款接口，更新订单状态为已取消
   - **拒绝**：填写拒绝原因，通知用户

### 退款状态流转

```
PENDING (待处理)
    ↓
PROCESSING (处理中) - 管理员开始处理
    ↓
APPROVED (已批准) → COMPLETED (已完成) - 退款成功
或
REJECTED (已拒绝) - 退款被拒绝
或
CANCELLED (已撤销) - 用户主动撤销
```

## API接口说明

### 1. 提交售后申请
```
POST /after-sales
Body: {
  orderId: number,
  userId: number,
  photographerId: number,
  serviceType: "REFUND",
  refundReason: string,
  refundAmount: number,
  description: string,
  contactPhone: string,
  evidenceImages?: string[]
}
```

### 2. 获取订单的售后记录
```
GET /after-sales/order/{orderId}
```

### 3. 获取用户的售后记录
```
GET /after-sales/user/{userId}
```

### 4. 获取所有退款申请（管理员）
```
GET /after-sales/list?serviceType=REFUND
```

### 5. 批准退款
```
POST /after-sales/{id}/approve-refund?handlerId={adminId}
```

### 6. 拒绝退款
```
POST /after-sales/{id}/reject-refund?handlerId={adminId}&reason={拒绝原因}
```

### 7. 撤销售后申请
```
POST /after-sales/{id}/cancel?userId={userId}
```

### 8. 支付宝退款
```
POST /pay/refund
参数：
  - orderId: 订单号
  - refundAmount: 退款金额
  - refundReason: 退款原因（可选）
```

## 配置要求

### 支付宝配置

确保 `application.yml` 中配置了支付宝相关参数：

```yaml
alipay:
  app-id: your_app_id
  private-key: your_private_key
  public-key: alipay_public_key
  gateway-url: https://openapi.alipaydev.com/gateway.do  # 沙箱环境
  return-url: http://localhost:3000/#/payment-success
  notify-url: http://your-domain/pay/notify
```

## 测试步骤

### 1. 初始化数据库
```bash
mysql -u root -p photo < sql/after_sales_service.sql
```

### 2. 启动后端服务
```bash
cd D:\JavaWork\PhotoBooker\PhotoBooker
mvn spring-boot:run
```

### 3. 启动前端服务
```bash
cd D:\JavaWork\PhotoBooker\PhotoBooker\test
npm install
npm run dev
```

### 4. 测试流程

**用户端测试：**
1. 登录用户账号
2. 进入订单详情页
3. 点击"售后服务"
4. 选择"退款申请"
5. 填写信息并提交
6. 查看提交的记录

**管理端测试：**
1. 登录管理员账号
2. 访问 `/admin/refund-management`
3. 查看退款申请列表
4. 点击"批准"或"拒绝"
5. 验证订单状态是否更新

## 注意事项

1. **退款权限**：只有已支付（CONFIRMED）及之后的订单状态可以申请退款
2. **退款金额**：默认全额退款，可根据实际需求调整为部分退款
3. **支付宝退款**：需要配置真实的支付宝商户信息才能进行实际退款
4. **事务处理**：退款操作使用事务保证数据一致性
5. **权限控制**：建议在后端添加管理员权限验证
6. **日志记录**：建议在关键步骤添加日志记录便于排查问题

## 扩展建议

1. **部分退款**：支持用户申请部分金额退款
2. **退款规则**：根据不同订单状态设置不同的退款比例
3. **自动退款**：满足条件时自动批准退款（如拍摄前24小时以上）
4. **消息通知**：退款状态变更时通过WebSocket或短信通知用户
5. **退款统计**：添加退款数据统计和分析功能
6. **导出功能**：支持导出退款记录为Excel

## 故障排查

### 问题1：退款申请提交失败
- 检查数据库表是否创建成功
- 检查后端日志是否有错误信息
- 验证用户是否有权限操作该订单

### 问题2：支付宝退款失败
- 检查支付宝配置是否正确
- 验证订单是否已支付
- 检查退款金额是否超过订单金额
- 查看支付宝返回的错误信息

### 问题3：管理员看不到退款申请
- 检查是否正确调用 `/after-sales/list` 接口
- 验证数据库中是否有退款记录
- 检查前端路由配置是否正确

## 相关文件清单

### 后端文件
- `src/main/java/com/xhxi/photobooker/enums/ServiceStatus.java`
- `src/main/java/com/xhxi/photobooker/service/OrderService.java`
- `src/main/java/com/xhxi/photobooker/service/impl/OrderServiceImpl.java`
- `src/main/java/com/xhxi/photobooker/service/AfterSalesServiceService.java`
- `src/main/java/com/xhxi/photobooker/service/impl/AfterSalesServiceServiceImpl.java`
- `src/main/java/com/xhxi/photobooker/controller/AliPayController.java`
- `src/main/java/com/xhxi/photobooker/controller/AfterSalesController.java`
- `src/main/java/com/xhxi/photobooker/entity/AfterSalesService.java`
- `src/main/java/com/xhxi/photobooker/entity/ServiceData.java`

### 前端文件
- `test/src/views/AfterSalesService.vue`
- `test/src/views/admin/RefundManagement.vue`

### 数据库脚本
- `sql/after_sales_service.sql`

## 更新日期

2026-04-05
