# Redis GEO 附近摄影师检索 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有 PhotoBooker 项目上新增 Redis GEO 附近摄影师检索能力：位置双写 GEO 索引、启动全量同步、`GET /photo/photographer/nearby` 按经纬度+半径返回附近摄影师及距离。

**Architecture:** 新增 `PhotographerGeoService`（GEO 读写封装，key=`photographer:geo`，member=photographerId），在 `PhotographerServiceImpl` 的更新/删除路径上双写 GEO；启动时 `GeoDataInitializer` 全量同步 DB→GEO；`/nearby` 接口先 GEORADIUS 拿 id+距离（升序），再 `selectBatchIds` 一次回表，过滤 `locationVisible=0`，按距离重排后返回 VO。

**Tech Stack:** Spring Boot 3.2.5 / Java 17 / MyBatis-Plus 3.5.5 / Spring Data Redis（`StringRedisTemplate.opsForGeo()`，GEORADIUS 命令，Redis 3.2+ 兼容）/ JUnit5 + Mockito（测试风格与现有 `OrderSchedulerTest` 一致，不连真实 Redis）

## Global Constraints

- Redis 连接 `192.168.100.129:6379`（无密码）；**测试一律 Mockito mock，不连真实 Redis、不碰 MySQL**
- GEO key 常量：`photographer:geo`；member = `String.valueOf(photographerId)`
- 只使用 `GEORADIUS`（`geoRadius` + `GeoRadiusCommandArgs`），不依赖 GEOSEARCH
- 坐标合法范围：lat∈[-90,90]，lng∈[-180,180]；`syncFromDb` 先 `delete(GEO_KEY)` 再全量重建
- 现有接口行为不变：`/map`、`/list`、`update-location` 语义不动；`update-location` 的 id 参数语义是 userId（内部 `findByUserId` 取真实记录），GEO 双写必须用该记录的 `photographerId`
- `Result`：code=1 成功 / 0 失败，`Result.success(data)` / `Result.error(msg)`
- 不新增任何 Maven 依赖；不修改 pom.xml
- distanceKm 保留两位小数：`Math.round(d * 100) / 100.0`
- 每个任务以 commit 收尾（遵循项目中文 commit 风格，如 `第一版，实现附近摄影师检索模块`）

---

### Task 1: PhotographerGeoService —— GEO 读写核心

**Files:**
- Create: `src/main/java/com/xhxi/photobooker/service/GeoDistanceResult.java`
- Create: `src/main/java/com/xhxi/photobooker/service/PhotographerGeoService.java`
- Create: `src/main/java/com/xhxi/photobooker/service/impl/PhotographerGeoServiceImpl.java`
- Test: `src/test/java/com/xhxi/photobooker/service/impl/PhotographerGeoServiceImplTest.java`

**Interfaces:**
- Produces:
  - `class GeoDistanceResult { Long photographerId; Double distanceKm; }`（@Data @AllArgsConstructor @NoArgsConstructor）
  - `interface PhotographerGeoService { void add(Long photographerId, Double longitude, Double latitude); void remove(Long photographerId); List<GeoDistanceResult> searchNearby(double longitude, double latitude, double radiusKm, int limit); int syncFromDb(List<Photographer> photographers); }`
  - 后续 Task 2/3/4 依赖上述签名

- [ ] **Step 1: 写失败测试** `PhotographerGeoServiceImplTest.java`

```java
package com.xhxi.photobooker.service.impl;

import com.xhxi.photobooker.entity.Photographer;
import com.xhxi.photobooker.service.GeoDistanceResult;
import com.xhxi.photobooker.service.PhotographerGeoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoLocation;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.GeoRadiusCommandArgs;
import org.springframework.data.redis.core.GeoResult;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhotographerGeoServiceImplTest {

    private static final String GEO_KEY = "photographer:geo";

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private GeoOperations<String, String> geoOps;

    @InjectMocks
    private PhotographerGeoServiceImpl geoService;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForGeo()).thenReturn(geoOps);
    }

    @Test
    void add_writesPointToGeo() {
        geoService.add(100L, 113.280637, 23.125178);

        verify(geoOps).add(eq(GEO_KEY), any(Point.class), eq("100"));
    }

    @Test
    void add_skipsNullCoordinate() {
        geoService.add(100L, null, null);

        verify(geoOps, never()).add(anyString(), any(Point.class), anyString());
    }

    @Test
    void remove_deletesMember() {
        geoService.remove(100L);

        verify(geoOps).remove(GEO_KEY, "100");
    }

    @Test
    void searchNearby_returnsSortedWithDistance() {
        List<GeoResult<GeoLocation<String>>> mockResults = new ArrayList<>();
        mockResults.add(new GeoResult<>(new GeoLocation<>("5", new Point(0, 0)), new Distance(1.0, Metrics.KILOMETERS), null));
        mockResults.add(new GeoResult<>(new GeoLocation<>("9", new Point(0, 0)), new Distance(3.5, Metrics.KILOMETERS), null));
        when(geoOps.geoRadius(eq(GEO_KEY), any(Circle.class), any(GeoRadiusCommandArgs.class))).thenReturn(mockResults);

        List<GeoDistanceResult> result = geoService.searchNearby(113.28, 23.12, 5, 20);

        assertEquals(2, result.size());
        assertEquals(5L, result.get(0).getPhotographerId());
        assertEquals(1.0, result.get(0).getDistanceKm());
        assertEquals(9L, result.get(1).getPhotographerId());
        assertEquals(3.5, result.get(1).getDistanceKm());
    }

    @Test
    void searchNearby_returnsEmptyWhenKeyMissing() {
        when(geoOps.geoRadius(eq(GEO_KEY), any(Circle.class), any(GeoRadiusCommandArgs.class))).thenReturn(new ArrayList<>());

        List<GeoDistanceResult> result = geoService.searchNearby(113.28, 23.12, 5, 20);

        assertTrue(result.isEmpty());
    }

    @Test
    void syncFromDb_rebuildsIndexAndFiltersInvalidCoordinates() {
        Photographer valid = Photographer.builder().id(1L).longitude(113.28).latitude(23.12).build();
        Photographer noCoord = Photographer.builder().id(2L).longitude(null).latitude(null).build();
        Photographer outOfRange = Photographer.builder().id(3L).longitude(200.0).latitude(23.0).build();
        List<Photographer> photographers = List.of(valid, noCoord, outOfRange);

        int count = geoService.syncFromDb(photographers);

        assertEquals(1, count);
        verify(stringRedisTemplate).delete(GEO_KEY);
        verify(geoOps).add(GEO_KEY, new Point(113.28, 23.12), "1");
        verify(geoOps, never()).add(GEO_KEY, new Point(200.0, 23.0), "3");
    }

    @Test
    void syncFromDb_usesLongitudeAsXLatitudeAsY() {
        Photographer p = Photographer.builder().id(1L).longitude(113.280637).latitude(23.125178).build();

        geoService.syncFromDb(List.of(p));

        verify(geoOps).add(GEO_KEY, new Point(113.280637, 23.125178), "1");
    }
}
```

注意：`Point(double x, double y)` —— x=经度(longitude)，y=纬度(latitude)。`verify` 里 mock 的 `add` 期望值必须与实际实现一致。

- [ ] **Step 2: 跑测试确认失败**

Run: `mvn -f "D:\JavaWork\PhotoBooker\PhotoBooker\pom.xml" test -Dtest=PhotographerGeoServiceImplTest`
Expected: FAIL —— 编译错误（`PhotographerGeoService`、`GeoDistanceResult`、`PhotographerGeoServiceImpl` 不存在）

- [ ] **Step 3: 实现三个类**

`GeoDistanceResult.java`:

```java
package com.xhxi.photobooker.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** GEO 检索中间结果：摄影师 id + 距查询点距离(km) */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeoDistanceResult {
    private Long photographerId;
    private Double distanceKm;
}
```

`PhotographerGeoService.java`:

```java
package com.xhxi.photobooker.service;

import com.xhxi.photobooker.entity.Photographer;

import java.util.List;

public interface PhotographerGeoService {
    /** 写入/覆盖摄影师坐标到 GEO 索引（member=photographerId）；坐标为 null 时忽略 */
    void add(Long photographerId, Double longitude, Double latitude);

    /** 从 GEO 索引移除摄影师 */
    void remove(Long photographerId);

    /** 按经纬度+半径(km)检索附近摄影师，按距离升序，最多 limit 个 */
    List<GeoDistanceResult> searchNearby(double longitude, double latitude, double radiusKm, int limit);

    /** 全量重建索引：先清空 GEO key，再写入所有坐标合法的摄影师，返回写入数 */
    int syncFromDb(List<Photographer> photographers);
}
```

`PhotographerGeoServiceImpl.java`:

```java
package com.xhxi.photobooker.service.impl;

import com.xhxi.photobooker.entity.Photographer;
import com.xhxi.photobooker.service.GeoDistanceResult;
import com.xhxi.photobooker.service.PhotographerGeoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoLocation;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.GeoRadiusCommandArgs;
import org.springframework.data.redis.core.GeoResult;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PhotographerGeoServiceImpl implements PhotographerGeoService {

    private static final String GEO_KEY = "photographer:geo";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void add(Long photographerId, Double longitude, Double latitude) {
        if (photographerId == null || longitude == null || latitude == null) {
            return;
        }
        stringRedisTemplate.opsForGeo().add(GEO_KEY, new Point(longitude, latitude), String.valueOf(photographerId));
    }

    @Override
    public void remove(Long photographerId) {
        if (photographerId == null) {
            return;
        }
        stringRedisTemplate.opsForGeo().remove(GEO_KEY, String.valueOf(photographerId));
    }

    @Override
    public List<GeoDistanceResult> searchNearby(double longitude, double latitude, double radiusKm, int limit) {
        Distance distance = new Distance(radiusKm, Metrics.KILOMETERS);
        Circle circle = new Circle(new Point(longitude, latitude), distance);
        GeoRadiusCommandArgs args = GeoRadiusCommandArgs.newGeoRadiusArgs()
                .includeDistance()
                .sortAscending()
                .limit(limit);
        List<GeoResult<GeoLocation<String>>> results =
                stringRedisTemplate.opsForGeo().geoRadius(GEO_KEY, circle, args);

        List<GeoDistanceResult> nearby = new ArrayList<>();
        if (results != null) {
            for (GeoResult<GeoLocation<String>> r : results) {
                nearby.add(new GeoDistanceResult(
                        Long.valueOf(r.getContent().getName()),
                        r.getDistance().getValue()));
            }
        }
        return nearby;
    }

    @Override
    public int syncFromDb(List<Photographer> photographers) {
        stringRedisTemplate.delete(GEO_KEY);
        int count = 0;
        for (Photographer p : photographers) {
            if (p.getId() != null && isValidCoordinate(p.getLongitude(), p.getLatitude())) {
                add(p.getId(), p.getLongitude(), p.getLatitude());
                count++;
            }
        }
        return count;
    }

    private boolean isValidCoordinate(Double longitude, Double latitude) {
        return longitude != null && latitude != null
                && latitude >= -90 && latitude <= 90
                && longitude >= -180 && longitude <= 180;
    }
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `mvn -f "D:\JavaWork\PhotoBooker\PhotoBooker\pom.xml" test -Dtest=PhotographerGeoServiceImplTest`
Expected: PASS（6 个测试全绿）

- [ ] **Step 5: Commit**

```bash
git -C "D:\JavaWork\PhotoBooker\PhotoBooker" add src/main/java/com/xhxi/photobooker/service/GeoDistanceResult.java src/main/java/com/xhxi/photobooker/service/PhotographerGeoService.java src/main/java/com/xhxi/photobooker/service/impl/PhotographerGeoServiceImpl.java src/test/java/com/xhxi/photobooker/service/impl/PhotographerGeoServiceImplTest.java
git -C "D:\JavaWork\PhotoBooker\PhotoBooker" commit -m "第一版，实现Redis GEO索引读写模块"
```

---

### Task 2: 附近检索业务组装 —— 回表、过滤、排序

**Files:**
- Create: `src/main/java/com/xhxi/photobooker/vo/PhotographerNearbyVO.java`
- Modify: `src/main/java/com/xhxi/photobooker/service/PhotographerService.java`（新增 `searchNearbyPhotographers` 方法声明）
- Modify: `src/main/java/com/xhxi/photobooker/service/impl/PhotographerServiceImpl.java`（新增方法实现 + 注入 `PhotographerGeoService`）
- Test: `src/test/java/com/xhxi/photobooker/service/impl/PhotographerNearbySearchTest.java`

**Interfaces:**
- Consumes: `PhotographerGeoService.searchNearby(double, double, double, int)` → `List<GeoDistanceResult>`（Task 1）
- Produces: `List<PhotographerNearbyVO> searchNearbyPhotographers(double latitude, double longitude, double radiusKm, int limit)`（`PhotographerService` 新方法，Task 5 的 Controller 依赖）

- [ ] **Step 1: 写失败测试** `PhotographerNearbySearchTest.java`

```java
package com.xhxi.photobooker.service.impl;

import com.xhxi.photobooker.entity.Photographer;
import com.xhxi.photobooker.mapper.PhotographerMapper;
import com.xhxi.photobooker.service.GeoDistanceResult;
import com.xhxi.photobooker.service.PhotographerGeoService;
import com.xhxi.photobooker.vo.PhotographerNearbyVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhotographerNearbySearchTest {

    @Mock
    private PhotographerMapper photographerMapper;

    @Mock
    private PhotographerGeoService photographerGeoService;

    @InjectMocks
    private PhotographerServiceImpl service;

    @Test
    void searchNearbyPhotographers_returnsSortedVisiblePhotographersWithDistance() {
        when(photographerGeoService.searchNearby(113.28, 23.12, 5, 20))
                .thenReturn(List.of(
                        new GeoDistanceResult(3L, 1.2),
                        new GeoDistanceResult(1L, 2.7)));

        Photographer p3 = Photographer.builder().id(3L).name("张三").phone("13800000001")
                .avatar("a.jpg").working(1).orderCount(10)
                .latitude(23.11).longitude(113.27).locationVisible(1).build();
        Photographer p1 = Photographer.builder().id(1L).name("李四").phone("13800000002")
                .avatar("b.jpg").working(1).orderCount(5)
                .latitude(23.10).longitude(113.25).locationVisible(1).build();
        Photographer pHidden = Photographer.builder().id(9L).name("隐藏").phone("13800000009")
                .working(1).orderCount(0)
                .latitude(23.10).longitude(113.25).locationVisible(0).build();
        // GEO 返回了 1、3、9 三个，DB 批量查出三个（含不可见的 9）
        when(photographerMapper.selectBatchIds(anyCollection())).thenReturn(List.of(p1, p3, pHidden));

        List<PhotographerNearbyVO> result = service.searchNearbyPhotographers(23.12, 113.28, 5, 20);

        // 顺序按 GEO 距离升序：3(1.2km) → 1(2.7km)；不可见的 9 被过滤
        assertEquals(2, result.size());
        assertEquals(3L, result.get(0).getId());
        assertEquals(1.2, result.get(0).getDistanceKm());
        assertEquals(1L, result.get(1).getId());
        assertEquals(2.7, result.get(1).getDistanceKm());
        assertEquals("张三", result.get(0).getName());
    }

    @Test
    void searchNearbyPhotographers_returnsEmptyWhenGeoEmpty() {
        when(photographerGeoService.searchNearby(113.28, 23.12, 5, 20)).thenReturn(List.of());

        List<PhotographerNearbyVO> result = service.searchNearbyPhotographers(23.12, 113.28, 5, 20);

        assertTrue(result.isEmpty());
    }

    @Test
    void searchNearbyPhotographers_roundsDistanceToTwoDecimals() {
        when(photographerGeoService.searchNearby(113.28, 23.12, 5, 20))
                .thenReturn(List.of(new GeoDistanceResult(1L, 1.2367)));
        Photographer p1 = Photographer.builder().id(1L).name("张三")
                .latitude(23.10).longitude(113.25).locationVisible(1).build();
        when(photographerMapper.selectBatchIds(anyCollection())).thenReturn(List.of(p1));

        List<PhotographerNearbyVO> result = service.searchNearbyPhotographers(23.12, 113.28, 5, 20);

        assertEquals(1.24, result.get(0).getDistanceKm());
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `mvn -f "D:\JavaWork\PhotoBooker\PhotoBooker\pom.xml" test -Dtest=PhotographerNearbySearchTest`
Expected: FAIL —— 编译错误（`PhotographerNearbyVO` 不存在、`searchNearbyPhotographers` 方法不存在）

- [ ] **Step 3: 实现 VO 与业务方法**

`PhotographerNearbyVO.java`:

```java
package com.xhxi.photobooker.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 附近摄影师返回 VO：展示字段 + 距离 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PhotographerNearbyVO {
    private Long id;
    private String name;
    private String phone;
    private String avatar;
    private int working;
    private Integer orderCount;
    private Double latitude;
    private Double longitude;
    /** 距查询点距离(km)，保留两位小数 */
    private Double distanceKm;
}
```

`PhotographerService.java` 接口中新增（`getAllPhotographers` 之后）：

```java
    List<Photographer> getAllPhotographers();

    // 附近摄影师检索：GEO 索引取 id+距离 → 批量回表 → 过滤不可见 → 按距离升序
    List<PhotographerNearbyVO> searchNearbyPhotographers(double latitude, double longitude, double radiusKm, int limit);
```

（补 import：`import com.xhxi.photobooker.vo.PhotographerNearbyVO;`）

`PhotographerServiceImpl.java` 修改：
1. 新增字段注入（现有 `@Autowired RedisTemplate` 之后）：

```java
    @Autowired
    private PhotographerGeoService photographerGeoService;
```

2. 新增 import：`com.xhxi.photobooker.service.GeoDistanceResult`、`com.xhxi.photobooker.service.PhotographerGeoService`、`com.xhxi.photobooker.vo.PhotographerNearbyVO`、`java.util.Map`、`java.util.stream.Collectors`（`ArrayList` 若未导入则补）

3. 类尾部新增实现：

```java
    @Override
    public List<PhotographerNearbyVO> searchNearbyPhotographers(double latitude, double longitude, double radiusKm, int limit) {
        List<GeoDistanceResult> nearby = photographerGeoService.searchNearby(longitude, latitude, radiusKm, limit);
        if (nearby.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> ids = nearby.stream()
                .map(GeoDistanceResult::getPhotographerId)
                .collect(Collectors.toList());
        // 一次 IN 查询回表，避免 N+1
        Map<Long, Photographer> visibleById = photographerMapper.selectBatchIds(ids).stream()
                .filter(p -> p.getLocationVisible() == null || p.getLocationVisible() == 1)
                .collect(Collectors.toMap(Photographer::getId, p -> p, (a, b) -> a));

        List<PhotographerNearbyVO> result = new ArrayList<>();
        for (GeoDistanceResult g : nearby) { // 按 GEO 距离升序重排
            Photographer p = visibleById.get(g.getPhotographerId());
            if (p == null) {
                continue;
            }
            result.add(PhotographerNearbyVO.builder()
                    .id(p.getId())
                    .name(p.getName())
                    .phone(p.getPhone())
                    .avatar(p.getAvatar())
                    .working(p.getWorking())
                    .orderCount(p.getOrderCount())
                    .latitude(p.getLatitude())
                    .longitude(p.getLongitude())
                    .distanceKm(Math.round(g.getDistanceKm() * 100) / 100.0)
                    .build());
        }
        return result;
    }
```

注意：`selectBatchIds` 接收 `Collection<? extends Serializable>`，传 `List<Long>` 即可；`(a, b) -> a` 处理重复 id（正常不会出现，防御性写法）。

- [ ] **Step 4: 跑测试确认通过**

Run: `mvn -f "D:\JavaWork\PhotoBooker\PhotoBooker\pom.xml" test -Dtest=PhotographerNearbySearchTest`
Expected: PASS（3 个测试全绿）

- [ ] **Step 5: Commit**

```bash
git -C "D:\JavaWork\PhotoBooker\PhotoBooker" add src/main/java/com/xhxi/photobooker/vo/PhotographerNearbyVO.java src/main/java/com/xhxi/photobooker/service/PhotographerService.java src/main/java/com/xhxi/photobooker/service/impl/PhotographerServiceImpl.java src/test/java/com/xhxi/photobooker/service/impl/PhotographerNearbySearchTest.java
git -C "D:\JavaWork\PhotoBooker\PhotoBooker" commit -m "第一版，实现附近摄影师检索业务组装"
```

---

### Task 3: GeoDataInitializer —— 启动全量同步

**Files:**
- Create: `src/main/java/com/xhxi/photobooker/config/GeoDataInitializer.java`
- Test: `src/test/java/com/xhxi/photobooker/config/GeoDataInitializerTest.java`

**Interfaces:**
- Consumes: `PhotographerService.getAllPhotographers()`（已有）、`PhotographerGeoService.syncFromDb(List<Photographer>)`（Task 1）

- [ ] **Step 1: 写失败测试** `GeoDataInitializerTest.java`

```java
package com.xhxi.photobooker.config;

import com.xhxi.photobooker.entity.Photographer;
import com.xhxi.photobooker.service.PhotographerGeoService;
import com.xhxi.photobooker.service.PhotographerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeoDataInitializerTest {

    @Mock
    private PhotographerService photographerService;

    @Mock
    private PhotographerGeoService photographerGeoService;

    @InjectMocks
    private GeoDataInitializer initializer;

    @Test
    void run_syncsAllPhotographersToGeoIndex() {
        List<Photographer> all = List.of(
                Photographer.builder().id(1L).longitude(113.28).latitude(23.12).build(),
                Photographer.builder().id(2L).longitude(113.30).latitude(23.14).build());
        when(photographerService.getAllPhotographers()).thenReturn(all);
        when(photographerGeoService.syncFromDb(all)).thenReturn(2);

        initializer.run();

        verify(photographerService).getAllPhotographers();
        verify(photographerGeoService).syncFromDb(all);
        assertEquals(2, initializer.getSyncedCount());
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `mvn -f "D:\JavaWork\PhotoBooker\PhotoBooker\pom.xml" test -Dtest=GeoDataInitializerTest`
Expected: FAIL —— 编译错误（`GeoDataInitializer` 不存在）

- [ ] **Step 3: 实现 GeoDataInitializer**

```java
package com.xhxi.photobooker.config;

import com.xhxi.photobooker.entity.Photographer;
import com.xhxi.photobooker.service.PhotographerGeoService;
import com.xhxi.photobooker.service.PhotographerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/** 启动时全量同步 DB → Redis GEO，兜底历史数据与绕过接口的 DB 直改 */
@Component
public class GeoDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(GeoDataInitializer.class);

    @Autowired
    private PhotographerService photographerService;

    @Autowired
    private PhotographerGeoService photographerGeoService;

    private int syncedCount;

    @Override
    public void run(String... args) {
        List<Photographer> all = photographerService.getAllPhotographers();
        syncedCount = photographerGeoService.syncFromDb(all);
        log.info("摄影师地理位置索引初始化完成，共同步 {} 个摄影师", syncedCount);
    }

    /** 测试观察用：本次同步的摄影师数 */
    public int getSyncedCount() {
        return syncedCount;
    }
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `mvn -f "D:\JavaWork\PhotoBooker\PhotoBooker\pom.xml" test -Dtest=GeoDataInitializerTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git -C "D:\JavaWork\PhotoBooker\PhotoBooker" add src/main/java/com/xhxi/photobooker/config/GeoDataInitializer.java src/test/java/com/xhxi/photobooker/config/GeoDataInitializerTest.java
git -C "D:\JavaWork\PhotoBooker\PhotoBooker" commit -m "第一版，实现启动时摄影师GEO索引全量同步"
```

---

### Task 4: 双写改造 —— updatePhotographer / deletePhotographer 同步 GEO

**Files:**
- Modify: `src/main/java/com/xhxi/photobooker/service/impl/PhotographerServiceImpl.java`（`updatePhotographer`、`deletePhotographer`）
- Test: `src/test/java/com/xhxi/photobooker/service/impl/PhotographerGeoWriteTest.java`

**Interfaces:**
- Consumes: `PhotographerGeoService.add(Long, Double, Double)`、`PhotographerGeoService.remove(Long)`（Task 1）
- 不改变任何方法签名与现有调用语义（`updatePhotographer` 仍返回 null，`deletePhotographer` 仍返回 boolean）

- [ ] **Step 1: 写失败测试** `PhotographerGeoWriteTest.java`

```java
package com.xhxi.photobooker.service.impl;

import com.xhxi.photobooker.entity.Photographer;
import com.xhxi.photobooker.entity.PhotographerPackage;
import com.xhxi.photobooker.mapper.PhotographerMapper;
import com.xhxi.photobooker.service.PhotographerGeoService;
import com.xhxi.photobooker.service.PhotographerPackageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhotographerGeoWriteTest {

    @Mock
    private PhotographerMapper photographerMapper;

    @Mock
    private PhotographerGeoService photographerGeoService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private PhotographerPackageService photographerPackageService;

    @InjectMocks
    private PhotographerServiceImpl service;

    @Test
    void updateLocation_writesGeoIndex() {
        // update-location 场景：id 参数是 userId，photographer 来自 findByUserId（含真实 id 与新坐标）
        Photographer existing = Photographer.builder().id(7L).userId(100L).build();
        when(photographerMapper.selectByUserId(100L)).thenReturn(existing);
        Photographer update = Photographer.builder().id(7L).userId(100L)
                .latitude(23.125178).longitude(113.280637)
                .lastLocationUpdateTime(new Date()).build();

        service.updatePhotographer(100L, update);

        verify(photographerMapper).updateById(update);
        verify(photographerGeoService).add(7L, 113.280637, 23.125178);
    }

    @Test
    void updatePhotographer_removesGeoIndexWhenCoordinatesCleared() {
        Photographer existing = Photographer.builder().id(7L).userId(100L).build();
        when(photographerMapper.selectByUserId(100L)).thenReturn(existing);
        // 坐标被清空（latitude/longitude 为 null）→ 从 GEO 移除
        Photographer update = Photographer.builder().id(7L).userId(100L)
                .latitude(null).longitude(null).build();

        service.updatePhotographer(100L, update);

        verify(photographerGeoService).remove(7L);
        verify(photographerGeoService, never()).add(any(), any(), any());
    }

    @Test
    void updatePhotographer_doesNothingWhenUserNotPhotographer() {
        when(photographerMapper.selectByUserId(100L)).thenReturn(null);

        service.updatePhotographer(100L, Photographer.builder().id(7L).build());

        verify(photographerMapper, never()).updateById(any());
        verify(photographerGeoService, never()).add(any(), any(), any());
        verify(photographerGeoService, never()).remove(any());
    }

    @Test
    void deletePhotographer_removesGeoIndexAfterDbDelete() {
        when(photographerMapper.deleteById(7L)).thenReturn(1);

        boolean result = service.deletePhotographer(7L);

        assertTrue(result);
        verify(photographerGeoService).remove(7L);
    }

    @Test
    void deletePhotographer_skipsGeoRemoveWhenDbDeleteFails() {
        when(photographerMapper.deleteById(7L)).thenReturn(0);

        boolean result = service.deletePhotographer(7L);

        assertFalse(result);
        verify(photographerGeoService, never()).remove(any());
    }
}
```

注意：`deletePhotographer` 内部会调用 `photographerPackageService.remove(any())`，mock 默认返回 null 即可，无影响。

- [ ] **Step 2: 跑测试确认失败**

Run: `mvn -f "D:\JavaWork\PhotoBooker\PhotoBooker\pom.xml" test -Dtest=PhotographerGeoWriteTest`
Expected: FAIL —— 断言不通过（`updatePhotographer` 目前不调用 GEO 服务）

- [ ] **Step 3: 实现双写**

`PhotographerServiceImpl.java` 中（`searchNearbyPhotographers` 不改，只改这两个方法）：

```java
    @Override
    public Photographer updatePhotographer(Long id, Photographer photographer) {
        Photographer photographer1 = photographerMapper.selectByUserId(id);
        if (photographer1 != null) {
            photographerMapper.updateById(photographer);
            // GEO 双写：有有效坐标则写入，坐标被清空则移除（与 DB 保持一致）
            if (photographer.getId() != null && photographer.getLatitude() != null && photographer.getLongitude() != null) {
                photographerGeoService.add(photographer.getId(), photographer.getLongitude(), photographer.getLatitude());
            } else if (photographer.getId() != null) {
                photographerGeoService.remove(photographer.getId());
            }
        }
        return null;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deletePhotographer(Long id) {
        // 先删除该摄影师的所有套餐
        QueryWrapper<PhotographerPackage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("photographer_id", id);
        photographerPackageService.remove(queryWrapper);

        // 再删除摄影师
        int result = photographerMapper.deleteById(id);
        if (result > 0) {
            // 同步从 GEO 索引移除
            photographerGeoService.remove(id);
        }
        return result > 0;
    }
```

- [ ] **Step 4: 跑测试确认通过**

Run: `mvn -f "D:\JavaWork\PhotoBooker\PhotoBooker\pom.xml" test -Dtest=PhotographerGeoWriteTest`
Expected: PASS（5 个测试全绿）

- [ ] **Step 5: 回归跑全部相关测试**

Run: `mvn -f "D:\JavaWork\PhotoBooker\PhotoBooker\pom.xml" test -Dtest=PhotographerGeoWriteTest,PhotographerGeoServiceImplTest,PhotographerNearbySearchTest,GeoDataInitializerTest`
Expected: PASS（全部通过，确认无相互影响）

- [ ] **Step 6: Commit**

```bash
git -C "D:\JavaWork\PhotoBooker\PhotoBooker" add src/main/java/com/xhxi/photobooker/service/impl/PhotographerServiceImpl.java src/test/java/com/xhxi/photobooker/service/impl/PhotographerGeoWriteTest.java
git -C "D:\JavaWork\PhotoBooker\PhotoBooker" commit -m "第一版，摄影师位置DB与GEO双写同步"
```

---

### Task 5: /nearby 接口 —— 参数校验与返回

**Files:**
- Modify: `src/main/java/com/xhxi/photobooker/controller/PhotographerController.java`
- Test: `src/test/java/com/xhxi/photobooker/controller/PhotographerNearbyControllerTest.java`

**Interfaces:**
- Consumes: `PhotographerService.searchNearbyPhotographers(double, double, double, int)`（Task 2）

- [ ] **Step 1: 写失败测试** `PhotographerNearbyControllerTest.java`

```java
package com.xhxi.photobooker.controller;

import com.xhxi.photobooker.result.Result;
import com.xhxi.photobooker.service.PhotographerService;
import com.xhxi.photobooker.vo.PhotographerNearbyVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhotographerNearbyControllerTest {

    private PhotographerController controller;
    private PhotographerService photographerService;

    @BeforeEach
    void setUp() {
        controller = new PhotographerController();
        photographerService = Mockito.mock(PhotographerService.class);
        // 通过反射注入（controller 字段是 @Autowired private，无 setter）
        try {
            java.lang.reflect.Field field = PhotographerController.class.getDeclaredField("photographerService");
            field.setAccessible(true);
            field.set(controller, photographerService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void nearby_rejectsInvalidLatitude() {
        Result<?> result = controller.nearby(200.0, 113.28, 5.0, 20);

        assertEquals(0, result.getCode());
        assertTrue(result.getMsg().contains("latitude"));
    }

    @Test
    void nearby_rejectsInvalidLongitude() {
        Result<?> result = controller.nearby(23.12, 200.0, 5.0, 20);

        assertEquals(0, result.getCode());
        assertTrue(result.getMsg().contains("longitude"));
    }

    @Test
    void nearby_rejectsInvalidRadius() {
        Result<?> result = controller.nearby(23.12, 113.28, 0.0, 20);

        assertEquals(0, result.getCode());
        assertTrue(result.getMsg().contains("radius"));
    }

    @Test
    void nearby_rejectsInvalidLimit() {
        Result<?> result = controller.nearby(23.12, 113.28, 5.0, 100);

        assertEquals(0, result.getCode());
        assertTrue(result.getMsg().contains("limit"));
    }

    @Test
    void nearby_returnsPhotographerListOnValidParams() {
        Mockito.when(photographerService.searchNearbyPhotographers(23.12, 113.28, 5.0, 20))
                .thenReturn(List.of(PhotographerNearbyVO.builder().id(1L).name("张三").distanceKm(1.5).build()));

        Result<List<PhotographerNearbyVO>> result = controller.nearby(23.12, 113.28, 5.0, 20);

        assertEquals(1, result.getCode());
        assertEquals(1, result.getData().size());
        assertEquals("张三", result.getData().get(0).getName());
        assertNull(result.getMsg());
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `mvn -f "D:\JavaWork\PhotoBooker\PhotoBooker\pom.xml" test -Dtest=PhotographerNearbyControllerTest`
Expected: FAIL —— 编译错误（`nearby` 方法不存在）

- [ ] **Step 3: 实现接口**

`PhotographerController.java` 中新增（放在 `getPhotographerMap()` 之后）：

```java
    //附近摄影师检索：基于 Redis GEO 索引，按经纬度+半径返回附近摄影师及距离
    @GetMapping("/nearby")
    public Result<List<PhotographerNearbyVO>> nearby(@RequestParam Double latitude,
                                                     @RequestParam Double longitude,
                                                     @RequestParam(defaultValue = "5") Double radius,
                                                     @RequestParam(defaultValue = "20") Integer limit) {
        if (latitude == null || latitude < -90 || latitude > 90) {
            return Result.error("参数不合法: latitude 需在 [-90, 90]");
        }
        if (longitude == null || longitude < -180 || longitude > 180) {
            return Result.error("参数不合法: longitude 需在 [-180, 180]");
        }
        if (radius == null || radius <= 0 || radius > 100) {
            return Result.error("参数不合法: radius 需在 (0, 100] km");
        }
        if (limit == null || limit < 1 || limit > 50) {
            return Result.error("参数不合法: limit 需在 [1, 50]");
        }
        List<PhotographerNearbyVO> list = photographerService.searchNearbyPhotographers(latitude, longitude, radius, limit);
        return Result.success(list);
    }
```

补 import：`com.xhxi.photobooker.vo.PhotographerNearbyVO`（`List`、`Result` 已导入）。

- [ ] **Step 4: 跑测试确认通过**

Run: `mvn -f "D:\JavaWork\PhotoBooker\PhotoBooker\pom.xml" test -Dtest=PhotographerNearbyControllerTest`
Expected: PASS（5 个测试全绿）

- [ ] **Step 5: Commit**

```bash
git -C "D:\JavaWork\PhotoBooker\PhotoBooker" add src/main/java/com/xhxi/photobooker/controller/PhotographerController.java src/test/java/com/xhxi/photobooker/controller/PhotographerNearbyControllerTest.java
git -C "D:\JavaWork\PhotoBooker\PhotoBooker" commit -m "第一版，实现附近摄影师检索接口"
```

---

### Task 6: 全量编译 + 端到端手动验证

**Files:**
- 无新增；验证为主
- 文档: `docs/superpowers/plans/2026-08-03-redis-geo-nearby.md` 不动

**Interfaces:**
- Consumes: Task 1–5 全部产物

- [ ] **Step 1: 全量编译 + 全部测试**

Run: `mvn -f "D:\JavaWork\PhotoBooker\PhotoBooker\pom.xml" test -Dtest=PhotographerGeoServiceImplTest,PhotographerNearbySearchTest,GeoDataInitializerTest,PhotographerGeoWriteTest,PhotographerNearbyControllerTest`
Expected: PASS（全部通过）

- [ ] **Step 2: 准备测试数据（直接 SQL，避免登录流程）**

连 `192.168.100.129:3306` 的 `photo` 库（root/1234），选取两个摄影师写入坐标（用真实存在的 id，先查再改）：

```sql
-- 查看现有摄影师
SELECT id, name, latitude, longitude, location_visible FROM photographer LIMIT 10;
-- 给两个摄影师写入广州天河区附近坐标（天河体育中心 113.2771, 23.1289 附近）
UPDATE photographer SET latitude = 23.1289, longitude = 113.2771, last_location_update_time = NOW() WHERE id = <id1>;
UPDATE photographer SET latitude = 23.1350, longitude = 113.2650, last_location_update_time = NOW() WHERE id = <id2>;
```

- [ ] **Step 3: 启动服务验证启动同步**

Run: `mvn -f "D:\JavaWork\PhotoBooker\PhotoBooker\pom.xml" spring-boot:run`（后台运行）
Expected: 日志出现 `摄影师地理位置索引初始化完成，共同步 N 个摄影师`；若无该日志或启动失败，检查：
- ES/Qdrant 相关配置是否导致启动失败（application-local.yml 的 `spring.ai` 与 `qdrant`/`elasticsearch` 配置）——若失败且与 GEO 无关，属既有问题，记录并说明，不影响本次功能验收
- Redis 连通性（`192.168.100.129:6379`）

- [ ] **Step 4: curl 验证接口**

```bash
# 查询点：天河体育中心 (113.2771, 23.1289)，半径 5km
curl "http://localhost:8080/photo/photographer/nearby?latitude=23.1289&longitude=113.2771&radius=5&limit=20"
```
Expected: `code=1`，返回包含 Step 2 写入坐标的摄影师，含 `distanceKm` 字段且按距离升序
```bash
# 无摄影师区域（如北京 39.90, 116.40）
curl "http://localhost:8080/photo/photographer/nearby?latitude=39.90&longitude=116.40&radius=5"
```
Expected: `code=1`，`data=[]`

- [ ] **Step 5: 验证可见性过滤与参数校验**

```sql
-- 把一个摄影师设为不可见
UPDATE photographer SET location_visible = 0 WHERE id = <id1>;
```
```bash
curl "http://localhost:8080/photo/photographer/nearby?latitude=23.1289&longitude=113.2771&radius=5"
```
Expected: 不可见的摄影师不再出现在结果中
```bash
# 非法参数
curl "http://localhost:8080/photo/photographer/nearby?latitude=200&longitude=113&radius=5"
```
Expected: `code=0`，msg 含 `latitude`
```sql
-- 恢复测试数据（可选，若不需要保留）
UPDATE photographer SET location_visible = 1 WHERE id = <id1>;
```

- [ ] **Step 6: 更新项目文档（docs/ 下已有 README 风格文档则追加）**

在 `docs/` 下新建 `redis_geo_nearby.md`，内容：
- GEO key：`photographer:geo`（member=摄影师id，坐标=经度/纬度）
- 接口：`GET /photo/photographer/nearby?latitude=&longitude=&radius=&limit=`
- 双写链路：update-location/管理端更新 → DB + GEO；删除 → GEOREMOVE；启动全量同步（`GeoDataInitializer`）
- 可见性过滤：服务层过滤 `locationVisible=0`

- [ ] **Step 7: Commit 文档**

```bash
git -C "D:\JavaWork\PhotoBooker\PhotoBooker" add docs/redis_geo_nearby.md
git -C "D:\JavaWork\PhotoBooker\PhotoBooker" commit -m "第一版，补充Redis GEO附近检索功能文档"
```
