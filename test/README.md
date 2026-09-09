# 摄影约拍系统前端

这是一个基于 Vue 3 + TypeScript + Element Plus 的摄影约拍系统前端项目。

## 功能特性

- 🏠 **首页展示** - 轮播图、服务特色、热门摄影师展示
- 👤 **用户管理** - 用户登录、个人中心、个人信息管理
- 📸 **摄影师浏览** - 摄影师列表、详情页、作品展示
- 📋 **订单管理** - 订单列表、订单详情、状态跟踪
- 💬 **在线聊天** - 用户与摄影师实时沟通
- 💳 **支付功能** - 订单支付、取消订单
- ⭐ **评价系统** - 订单评价、评分功能

## 技术栈

- **前端框架**: Vue 3
- **开发语言**: TypeScript
- **UI 组件库**: Element Plus
- **状态管理**: Pinia
- **路由管理**: Vue Router
- **HTTP 客户端**: Axios
- **构建工具**: Vite
- **日期处理**: Day.js

## 项目结构

```
src/
├── components/          # 公共组件
│   └── NavBar.vue      # 导航栏组件
├── views/              # 页面组件
│   ├── Home.vue        # 首页
│   ├── Login.vue       # 登录页
│   ├── PhotographerList.vue    # 摄影师列表
│   ├── PhotographerDetail.vue  # 摄影师详情
│   ├── OrderList.vue   # 订单列表
│   ├── OrderDetail.vue # 订单详情
│   ├── Chat.vue        # 聊天页面
│   └── Profile.vue     # 个人中心
├── stores/             # 状态管理
│   └── user.ts         # 用户状态
├── router/             # 路由配置
│   └── index.ts        # 路由定义
├── utils/              # 工具函数
│   └── api.ts          # API 配置
└── assets/             # 静态资源
```

## 快速开始

### 环境要求

- Node.js >= 16.0.0
- npm >= 8.0.0

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

项目将在 `http://localhost:3000` 启动。

### 构建生产版本

```bash
npm run build
```

### 预览生产版本

```bash
npm run preview
```

## 后端接口

项目配置的后端服务器地址为 `http://localhost:8099`，主要接口包括：

### 用户相关
- `POST /photo/user/login` - 用户登录

### 订单相关
- `POST /api/orders` - 创建订单
- `GET /api/orders/user/{userId}` - 获取用户订单列表
- `PUT /api/orders/{id}/cancel` - 取消订单
- `POST /api/orders/{orderId}/chat` - 开始聊天

## 主要功能说明

### 1. 用户认证
- 支持用户名密码登录
- JWT Token 认证
- 路由守卫保护

### 2. 摄影师浏览
- 摄影师列表展示
- 筛选功能（风格、年限、认证状态）
- 摄影师详情页
- 作品展示和套餐选择

### 3. 订单管理
- 订单创建和支付
- 订单状态跟踪
- 订单详情查看
- 订单取消功能

### 4. 在线聊天
- 实时消息通信
- 消息历史记录
- 支持 WebSocket 连接

### 5. 个人中心
- 个人信息管理
- 密码修改
- 账户安全设置
- 订单统计

## 开发说明

### 状态管理
使用 Pinia 进行状态管理，主要管理用户登录状态和用户信息。

### 路由配置
使用 Vue Router 进行路由管理，包含路由守卫进行登录状态检查。

### API 配置
使用 Axios 进行 HTTP 请求，配置了请求拦截器和响应拦截器。

### 样式设计
采用 Element Plus 组件库，结合自定义 CSS 实现现代化的 UI 设计。

## 注意事项

1. 确保后端服务器在 `localhost:8099` 运行
2. 部分功能使用模拟数据，实际使用时需要连接真实的后端 API
3. WebSocket 聊天功能需要后端支持
4. 支付功能需要集成第三方支付接口

## 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 联系方式

如有问题或建议，请通过以下方式联系：

- 邮箱: your-email@example.com
- 项目地址: https://github.com/your-username/photobooker-frontend
