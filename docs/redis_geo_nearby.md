# Redis GEO 附近摄影师检索功能说明

## 概述

本文档说明 PhotoBooker 摄影约拍平台基于 Redis GEO 的"附近摄影师"检索功能实现细节和使用方法。该功能允许用户按当前经纬度查询一定半径内可约拍的摄影师，并按距离升序返回，同时附带每位摄影师与查询点的距离（公里）。

## 数据模型：Redis GEO key

- **key**：`photographer:geo`
- **member**：摄影师 id（字符串形式）
- **坐标**：经度（longitude）、纬度（latitude）——注意 Redis GEOADD 的坐标顺序是「经度、纬度」

## 接口

### 附近摄影师检索

```
GET /photo/photographer/nearby?latitude=&longitude=&radius=&limit=
```

| 参数 | 类型 | 必填 | 默认 | 校验范围 |
|------|------|------|------|----------|
| `latitude` | Double | 是 | - | [-90, 90] |
| `longitude` | Double | 是 | - | [-180, 180] |
| `radius` | Double | 否 | 5 | (0, 100] km |
| `limit` | Integer | 否 | 20 | [1, 50] |

**返回**（统一 `Result` 包装，`code=1` 成功）：

```json
{
  "code": 1,
  "msg": null,
  "data": [
    {
      "id": 1,
      "name": "张三",
      "latitude": 23.1289,
      "longitude": 113.2771,
      "distanceKm": 0.0
    }
  ]
}
```

- 结果按距离升序排列（`GEOSEARCH/GEORADIUS SORT ASC`）
- `distanceKm` 单位为公里
- 参数不合法时返回 `code=0`，`msg` 形如 `参数不合法: latitude 需在 [-90, 90]`

## 双写链路

为保证 DB 与 Redis GEO 索引一致，所有数据变更走统一入口，同步维护 GEO：

1. **更新摄影师信息 / 更新位置**（`PhotographerServiceImpl.updatePhotographer`）：
   - 更新 DB 后，若坐标有效则 `GEOADD` 写入 `photographer:geo`
   - 若坐标被清空（latitude/longitude 为 null），则 `GEOREMOVE` 移除
   - 覆盖场景：管理端更新资料、`update-location` 更新位置、`/toggle-location-visible` 切换可见性等
2. **删除摄影师**（`PhotographerServiceImpl.deletePhotographer`，事务内）：
   - DB 删除成功后同步 `GEOREMOVE`，避免孤儿成员
3. **启动全量同步**（`GeoDataInitializer`，`CommandLineRunner`）：
   - 应用启动时从 DB 全量重建 GEO 索引：先 `DEL photographer:geo`，再对坐标合法的摄影师逐个 `GEOADD`
   - 兜底历史数据与绕过接口的 DB 直改
   - 完成日志：`摄影师地理位置索引初始化完成，共同步 N 个摄影师`

## 可见性过滤

- 服务层在 `PhotographerServiceImpl.searchNearbyPhotographers` 中对 GEO 命中的摄影师做 `IN` 回表查询，过滤 `location_visible = 0`（不可见）的摄影师
- 即：GEO 索引中保留全部有坐标的摄影师，但接口只返回可见的；不可见摄影师不会出现在附近检索结果中

## 代码结构

| 文件 | 职责 |
|------|------|
| `service/PhotographerGeoService.java` | GEO 服务接口（add / remove / searchNearby / syncFromDb） |
| `service/impl/PhotographerGeoServiceImpl.java` | 基于 `StringRedisTemplate.opsForGeo()` 的实现，`GEORADIUS` 带距离、升序、limit |
| `service/GeoDistanceResult.java` | 距离结果 DTO（摄影师 id + 距离公里数） |
| `config/GeoDataInitializer.java` | 启动全量同步（CommandLineRunner） |
| `controller/PhotographerController.java` | `GET /photo/photographer/nearby` 接口 + 参数校验 |
| `service/impl/PhotographerServiceImpl.java` | 双写（update / delete）与可见性过滤组装 |

## 测试

5 个测试类覆盖核心链路（`mvn test -Dtest=...`）：

- `PhotographerGeoServiceImplTest`：GEO 读写、距离、排序
- `PhotographerNearbySearchTest`：附近检索组装与可见性过滤
- `GeoDataInitializerTest`：启动同步
- `PhotographerGeoWriteTest`：DB 与 GEO 双写（更新/删除）
- `PhotographerNearbyControllerTest`：接口参数校验与返回结构

## 使用示例

```bash
# 查询广州天河体育中心 (113.2771, 23.1289) 附近 5km 内的摄影师
curl "http://localhost:8080/photo/photographer/nearby?latitude=23.1289&longitude=113.2771&radius=5&limit=20"

# 无摄影师区域（如北京）返回空列表
curl "http://localhost:8080/photo/photographer/nearby?latitude=39.90&longitude=116.40&radius=5"
```
