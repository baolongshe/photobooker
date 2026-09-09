# 设计文档：Redis GEO 附近摄影师检索

日期：2026-08-03
范围：在现有 PhotoBooker 项目（Spring Boot 3.2.5 + MyBatis-Plus + Redis）上新增"附近摄影师检索"能力

## 背景与目标

现有 `/photo/photographer/map` 接口全量返回摄影师坐标（纯 DB 查询），没有附近检索能力。
本次改造引入 **Redis GEO 索引**，实现按经纬度 + 半径检索附近摄影师，替换"纯 DB 查询 + 三方地图 API"的弱方案。

目标：
1. 新增 `GET /photo/photographer/nearby` 接口，支持经纬度 + 半径检索，返回距离
2. 位置更新走 DB + GEO 双写，保证索引一致性
3. 启动时全量同步 DB → GEO，兜底历史数据与绕过接口的 DB 直改
4. 原 `/map`、`/list`、`update-location` 接口行为不变

## 架构与数据流

```
写链路（双写）:
  update-location 接口 / 管理端更新
    → DB 更新成功
    → GEOADD photographer:geo <photographerId> <lng> <lat>
  deletePhotographer
    → DB 删除
    → GEOREMOVE photographer:geo <photographerId>
  启动时 GeoDataInitializer (CommandLineRunner)
    → 全量遍历 DB 中有有效坐标的摄影师 → GEOADD（幂等，覆盖式）

读链路:
  GET /nearby?latitude=&longitude=&radius=&limit=
    → 参数校验
    → GEORADIUS photographer:geo <lng> <lat> <radius> km WITHDIST ASC COUNT <limit>
    → GeoResults: photographerId + distanceKm（升序）
    → selectBatchIds 批量回表（一次 IN 查询，避免 N+1）
    → 过滤 locationVisible != 0（null 视为可见，与 /map 逻辑一致）
    → 手动按 GEO 返回顺序重排（保证距离排序）
    → 组装 PhotographerNearbyVO 返回
```

## 组件清单

| 文件 | 动作 | 职责 |
|------|------|------|
| `service/PhotographerGeoService.java` + `service/impl/PhotographerGeoServiceImpl.java` | 新增 | GEO 读写封装：`add` / `remove` / `searchNearby` / `syncFromDb`；key 常量 `photographer:geo`；member = photographerId（String） |
| `vo/PhotographerNearbyVO.java` | 新增 | 摄影师展示字段（id、name、phone、avatar、working、orderCount、latitude、longitude）+ `distanceKm` |
| `config/GeoDataInitializer.java` | 新增 | `CommandLineRunner`，启动时调用 `syncFromDb()`，过滤 lat/lng 为 null 或越界的记录 |
| `PhotographerServiceImpl` | 修改 | `updatePhotographer` 更新成功后 GEO 双写；`deletePhotographer` 删除后 GEOREMOVE |
| `PhotographerController` | 修改 | 新增 `/nearby` 端点 + 参数校验 |

## 关键决策

1. **可见性策略**：GEO 存全量（含不可见摄影师），`locationVisible=0` 在服务层过滤。索引与业务解耦；查询链路反正要回表取详情，过滤是零成本。不改 `toggle-location-visible` 接口行为。
2. **GEO 同步挂点**：挂在 `PhotographerServiceImpl.updatePhotographer`（服务层）而非 controller，管理端改位置同样生效。
3. **坐标语义**：member 用摄影师主键 `photographerId`；仅 lat/lng 均非空且合法的摄影师入 GEO（lat∈[-90,90]，lng∈[-180,180]）。
4. **排序正确性**：GEORADIUS 返回按距离升序，但 `selectBatchIds` 不保证顺序，必须按 GEO 结果顺序手动重排。
5. **缓存策略**：附近检索不走 `photographer:detail:{id}` 单点缓存，批量场景一次 IN 查询更优；查询后不写详情缓存。
6. **兼容既有坑**：`update-location` 接口的 id 参数实际是 userId（内部 `findByUserId(id)` 取真实摄影师记录），GEO 双写必须用该记录的真实 `photographerId`，不改变现有调用语义。
7. **命令兼容性**：使用 `GEORADIUS`（Redis 3.2+ 全版本可用），不依赖 6.2+ 的 `GEOSEARCH`。

## 接口定义

```
GET /photo/photographer/nearby
参数:
  latitude  Double 必填 [-90, 90]
  longitude Double 必填 [-180, 180]
  radius    Double 可选，单位 km，默认 5，范围 (0, 100]
  limit     Integer 可选，默认 20，范围 [1, 50]
返回: Result<List<PhotographerNearbyVO>> 按距离升序，distanceKm 保留 2 位小数
失败: Result.error("参数不合法: ...")
```

## 错误处理与边界

- 参数校验失败 → `Result.error`（保持项目现有返回风格）
- GEO key 不存在 / 空 → 返回空列表
- 传入坐标非法 → 校验拦截
- 摄影师无坐标 → 不入 GEO，自然不参与检索
- GEO 数据与 DB 不一致（如 DB 直改、Redis 被清）→ 启动同步兜底；Redis 崩溃恢复后重启服务即可重建索引
- 并发：GEOADD/GEOREMOVE 是单命令原子操作，无并发问题

## 测试

集成测试（连接真实 Redis 192.168.100.129）：
1. 构造 3 个摄影师：距查询点 1km / 3km / 20km，断言 `/nearby` 返回顺序为 1km → 3km，且 20km 的不在 5km 半径结果中
2. 断言 `distanceKm` 数值正确（误差 < 1km）
3. 断言 `locationVisible=0` 的摄影师被过滤
4. 断言参数校验（radius=0、lat=200）返回 `Result.error`
5. `@AfterEach` 删除测试写入的 GEO member 与 DB 记录，不污染生产数据
6. 手动验证：启动服务，向两个摄影师写入坐标，调用 `/nearby` 观察结果

## 不做的事（YAGNI）

- 不做定时增量同步任务（启动全量 + 双写已覆盖；生产需要时可后续加）
- 不改 `/map`、`/list` 现有行为
- 不引入新依赖（Spring Data Redis 原生支持 opsForGeo）
- 不做前端改造
