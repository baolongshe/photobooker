# 摄影预约智能系统 - AI Agent模块
   已创建本项目的通用ReAct循环，开箱即可使用，只需在 pom.xml 中添加依赖，实现 AgentTool 接口定义自己的工具，Spring Boot 会自动装配整个 ReAct 循环。 项目链接：https://github.com/baolongshe/react-agent-spring-boot-starter.git
   
> 基于 SpringBoot + Spring AI + Vue3，实现基于 ReAct(Reason+Act) 架构的AI智能预约Agent。
> 本Agent复用SpringAI工具元注册、JSON Schema生成、ToolCall协议解析，关闭框架内置自动工具循环，自研ReAct多轮推理调度。

## 核心特性

- **ReAct 智能Agent**：手动驱动「推理-行动-观察」循环，支持多步骤自然语言预约业务 
- **工具注册中心**：7个业务工具，区分公开工具/登录鉴权工具池
- **Agent防护策略**：最大迭代熔断、重复调用死循环检测、同参数调用次数限制，防止token无限消耗
- **安全机制**：工具返回结果XML封装隔离，降低Prompt注入风险
- **业务工具**：摄影师检索、附近摄影师查询、订单创建等业务能力

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 17, SpringBoot 3.2.5, Spring AI 1.0.0-M6, MyBatis-Plus 3.5.5 |
| 前端 | Vue3, Vite, Element Plus |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis (Lettuce) |
| 向量检索 | Qdrant, Elasticsearch |
| 本地模型 | Ollama (qwen2, bge-large-zh) |

## 项目结构

```
PhotoBooker/
├── src/main/java/com/xhxi/photobooker/
│   ├── agent/              # AI Agent 核心
│   │   ├── react/          # ReAct 推理调度
│   │   └── tools/          # 业务工具注册
│   ├── config/             # 配置类
│   ├── controller/         # 接口层
│   ├── service/            # 业务逻辑层
│   │   ├── rag/            # RAG 检索增强生成
│   │   └── ollama/         # Ollama 本地模型
│   ├── entity/             # 实体类
│   ├── mapper/             # MyBatis Mapper
│   └── security/           # 安全认证
├── src/main/resources/
│   ├── application.yml     # 主配置
│   ├── application-local.yml
│   └── application-prod.yml
├── test/                   # Vue3 前端工程
│   ├── src/
│   │   ├── views/          # 页面组件
│   │   ├── components/     # 公共组件
│   │   └── router/         # 路由
│   └── package.json
└── sql/                    # 数据库脚本
```

## 启动指南

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0
- Redis
- Node.js 18+ (前端)

### 后端启动

```bash
# 打包
mvn clean package -DskipTests

# 运行
java -jar target/PhotoBooker-0.0.1-SNAPSHOT.jar
```

### 前端启动

```bash
cd test
npm install
npm run dev
```

### 配置说明

启动前需配置以下环境变量（或在 `application-local.yml` 中配置）：

| 环境变量 | 说明 |
|---------|------|
| `DB_URL` | MySQL 连接地址 |
| `DB_USERNAME` | 数据库用户名 |
| `DB_PASSWORD` | 数据库密码 |
| `REDIS_HOST` | Redis 地址 |
| `REDIS_PASSWORD` | Redis 密码 |
| `AI_API_KEY` | DeepSeek API Key |
| `ALIPAY_APP_ID` | 支付宝 AppID |
| `ALIPAY_PRIVATE_KEY` | 支付宝私钥 |
| `ALIPAY_PUBLIC_KEY` | 支付宝公钥 |
| `COS_SECRET_ID` | 腾讯云 COS SecretID |
| `COS_SECRET_KEY` | 腾讯云 COS SecretKey |
