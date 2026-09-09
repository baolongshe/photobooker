# 附近摄影师：前端 UI 入口 + AI Agent 工具接入（设计）

日期：2026-08-04
状态：已批准

## 背景与目标

后端已有完整的"附近摄影师"检索能力（Redis GEO 索引 + `GET /photo/photographer/nearby` 接口 + 5 个测试），但**没有任何业务入口消费它**：前端无页面调用，AI Agent 的 `searchPhotographer` 工具只做文本匹配（`location` 字段 LIKE），无距离概念。

本设计补齐两块消费端：
1. **前端**：新增独立页面 + 列表页按钮 + 导航栏入口，用户可定位并查看附近摄影师（带距离）。
2. **AI Agent**：新增独立工具 `searchNearbyPhotographer`，聊天中用户说"附近"时可查距离排序的摄影师。

原则：后端不新增业务查询逻辑（全部复用 `PhotographerService.searchNearbyPhotographers`），前端不引入新依赖（浏览器原生定位 API）。

## 现状（复用而不改动）

- `GET /photo/photographer/nearby?latitude&longitude&radius&limit` —— 已有，含参数校验（lat [-90,90]、lng [-180,180]、radius (0,100]km、limit [1,50]），返回 `PhotographerNearbyVO`（id/name/phone/avatar/working/orderCount/latitude/longitude/distanceKm）
- `PhotographerService.searchNearbyPhotographers(latitude, longitude, radiusKm, limit)` —— 已有：Redis GEO `GEORADIUS` 取 id+距离 → `selectBatchIds` 一次回表 → 过滤 `location_visible != 1` → 按距离升序组装 VO
- Agent 体系：`AgentTool` 接口（getName/getDescription/getParameterSchema/execute）、`AgentToolFunctionConfig`（@Bean 包装）、`AiChatController`（工具数组 + 系统提示词 + SSE 流式）
- 前端：Vue3 + TS + Element Plus + vue-router（hash 模式），axios 封装在 `utils/api.ts`（baseURL `/api`，统一错误提示），TMap SDK 已全局加载

## 总体架构

```
前端 (Vue3 + Element Plus)
  列表页"附近摄影师"按钮 / 导航栏入口
    → /photographer-nearby 新页面
    → navigator.geolocation 定位（失败弹手动输入，默认 广州天河 23.1289, 113.2771）
    → GET /photo/photographer/nearby?latitude&longitude&radius&limit
    → 卡片列表（距离/工作状态/订单数）→ 点击跳 /photographer/:id

后端 (Spring Boot)
  /nearby 接口 —— 已有，不动
  新增 SearchNearbyPhotographerTool（AgentTool 接口）
    → AgentToolFunctionConfig 注册 @Bean searchNearbyPhotographer
    → AiChatController：工具数组两处 + 系统提示词（登录/未登录两段）
```

## 后端变更

### 1. 新增 `agent/tools/SearchNearbyPhotographerTool.java`

实现 `AgentTool`：

- `getName()`：`search_nearby_photographer`
- `getDescription()`：按经纬度+半径检索附近摄影师，返回按距离升序的列表（含距离）。当用户想找"附近/离我近/周边"的摄影师时使用
- `getParameterSchema()`：`latitude`（number，必填）、`longitude`（number，必填）、`radiusKm`（number，默认 5）、`limit`（number，默认 10）
- `execute(parameters)`：
  1. 坐标校验：lat ∈ [-90,90]、lng ∈ [-180,180]、radiusKm ∈ (0,100]、limit ∈ [1,50]；越界返回友好错误文本（如 `"参数不合法: latitude 需在 [-90, 90]"`），不抛异常，LLM 直接转述
  2. 调 `photographerService.searchNearbyPhotographers(latitude, longitude, radiusKm, limit)`（注意 Service 签名是**纬度在前**，与 GEO 内部经度在前不同）
  3. 返回 `List<Map<String, Object>>`：`id`、`name`、`phone`、`avatar`、`working`、`distanceKm`

依赖注入：`PhotographerService`（不直接碰 Redis，测试可 mock）。

### 2. `config/AgentToolFunctionConfig.java`

- 新增 `@Bean @Description(...) Function<SearchNearbyPhotographerRequest, List<Map<String,Object>>> searchNearbyPhotographer(SearchNearbyPhotographerTool tool)`，复用现有 `executeTool` 辅助方法
- 新增 record `SearchNearbyPhotographerRequest(Double latitude, Double longitude, Double radiusKm, Integer limit)` + `toParamMap()`（沿用 `putIfNotNull` 模式）

### 3. `controller/AiChatController.java`

- `PUBLIC_TOOL_FUNCTIONS` 与 `ALL_TOOL_FUNCTIONS` 数组都追加 `"searchNearbyPhotographer"`（检索类无敏感数据，未登录可用，与 `searchPhotographer` 同层级）
- `buildAgentSystemPrompt`：登录段与未登录段的"可用工具"列表各追加一条：
  `searchNearbyPhotographer - 查找附近摄影师（参数: latitude纬度, longitude经度, radiusKm半径km, limit数量）`（描述文字以实际 Bean 描述为准）
- 追加触发规则（两段都加）：
  - 用户提到"附近/离我近/周边"时优先使用此工具
  - 用户给了城市/地标时，按其大致经纬度调用；用户没说位置时**必须反问**用户所在位置，禁止编造坐标

## 前端变更

### 1. 新增 `views/PhotographerNearby.vue`

页面结构（复用 `PhotographerList.vue` 的卡片风格）：

- 顶部：标题"附近摄影师" + 半径选择（el-select：1/3/5/10/20 km，默认 5）+ "重新定位"按钮
- 进入时自动定位：`navigator.geolocation.getCurrentPosition`，超时 10s
  - 成功：取 `coords.latitude/longitude` 调接口
  - 失败（拒绝授权/超时/非 HTTPS）：弹 el-dialog 手动输入经纬度（预填默认值 `23.1289, 113.2771`，即广州天河），确认后调接口
- 结果：`PhotographerNearbyVO` 卡片列表，展示头像、姓名、**距离（distanceKm，保留 2 位小数，`X.X km`）**、工作状态（el-tag：接单中/休息中）、完成订单数、电话；点击卡片跳 `/photographer/:id`
- 空结果：`el-empty` 提示"附近暂无摄影师"
- 定位失败且用户取消手动输入：停留在空状态并可重试
- 接口调用用 `api.get('/photo/photographer/nearby', { params })`（axios 实例，自动带 token，`res.data` 为 VO 数组）

### 2. `router/index.ts`

- 注册路由 `/photographer-nearby`，name `photographer-nearby`
- 加入 `publicPages` 数组（附近检索无敏感数据，未登录可看）

### 3. `views/PhotographerList.vue`

顶部按钮区（现有"地图查看"旁）加"附近摄影师"按钮，`$router.push('/photographer-nearby')`。

### 4. `components/NavBar.vue`

加 `el-menu-item index="/photographer-nearby"`（Location 图标 + "附近摄影师"），不限制登录态。

## 数据流

**前端场景**：用户进页面 → 定位（成功或手动输入）→ `/nearby` → VO 列表 → 卡片渲染。

**AI 聊天场景**（"帮我找附近 5 公里的摄影师"）：
LLM 识别意图 → 调 `searchNearbyPhotographer(latitude≈, longitude≈, radiusKm=5)` → Tool → `PhotographerService.searchNearbyPhotographers`（Redis GEO → 回表 → 可见性过滤 → 距离排序）→ 带 `distanceKm` 的列表 → LLM 组织自然语言回复。

**AI 聊天场景**（"附近有摄影师吗？"——无位置）：
AI 按提示词反问"可以告诉我你在哪个位置吗（城市/地标都行）？"，不调用工具、不编造坐标。

## 错误处理

- 前端：定位失败 → 手动输入弹窗；接口失败 → `api.ts` 响应拦截器已有统一 ElMessage
- 工具：坐标越界 → 返回错误文本（LLM 转述，不抛异常）；Service 异常 → 由 `AgentToolFunctionConfig.executeTool` 的 try-catch 包装成 RuntimeException（沿用现有行为）
- Redis 不可达：`searchNearby` 返回空列表，AI 回答"附近暂时没有摄影师"，前端显示空状态——现有降级设计自然生效，无需新处理

## 测试

### 后端新增 `src/test/java/com/xhxi/photobooker/agent/tools/SearchNearbyPhotographerToolTest.java`

Mock `PhotographerService`（Mockito），不依赖真实 Redis：

1. 坐标越界（lat=91、lng=181、radius=0、limit=0/51）→ 返回错误文本，不调 Service
2. 合法参数 → 正确调用 `searchNearbyPhotographers(lat, lng, radiusKm, limit)`（断言参数传递与顺序：纬度在前）
3. 返回结构：VO 字段映射（id/name/phone/avatar/working/distanceKm）
4. 空结果 → 返回空列表

### 前端

项目无前端测试框架，跳过自动化测试；完成后用 run skill 手动验证（定位授权、手动输入、半径切换、卡片跳转、AI 聊天"附近"触发）。

## 明确不做（YAGNI）

- 不扩展 `searchPhotographer` 工具（语义隔离，避免 LLM 参数混淆）——已决策
- 不做城市坐标映射表、不查用户地址表取位置（AI 模型先验坐标 + 反问足够）
- 不改 `/nearby` 接口与 GEO 索引本身
- 不引入前端定位 SDK/依赖（浏览器原生 API 足够）
