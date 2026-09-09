# 附近摄影师：前端 UI 入口 + AI Agent 工具接入 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让已就绪的 Redis GEO 附近检索能力真正闭环：前端新增"附近摄影师"页面（浏览器定位+手动输入兜底），AI Agent 新增 `searchNearbyPhotographer` 工具。

**Architecture:** 后端零新增查询逻辑——新工具类只是 `PhotographerService.searchNearbyPhotographers()` 的胶水，经 `AgentToolFunctionConfig` 注册为 Spring AI Function，`AiChatController` 登记工具名并补充系统提示词；前端新增 Vue 页面 + 路由 + 两处入口（列表页按钮、导航栏）。

**Tech Stack:** Java 17+ / Spring Boot / Spring AI / MyBatis-Plus / Redis GEO（已有）；Vue3 + TypeScript + Element Plus + vue-router。

**Spec:** `docs/superpowers/specs/2026-08-04-nearby-frontend-ai-design.md`

## Global Constraints

- **非 git 仓库**：本目录无 `.git`，所有任务的结束点是"验证通过"，**无 commit 步骤**。
- 后端不新增任何业务查询逻辑，一律复用 `PhotographerService.searchNearbyPhotographers(latitude, longitude, radiusKm, limit)` —— 注意签名**纬度在前**（与 GEO 内部经度在前不同）。
- 参数校验范围（与 `/nearby` 接口一致）：lat ∈ [-90,90]、lng ∈ [-180,180]、radiusKm ∈ (0,100]、limit ∈ [1,50]。
- 工具参数默认值：radiusKm=5、limit=10（前端页面 limit 传 20）。
- 前端不引入新依赖；定位只用 `navigator.geolocation`，失败弹手动输入，默认坐标 23.1289, 113.2771（广州天河）。
- `/photo/photographer/nearby` 接口与 GEO 索引**不动**。
- 运行环境：后端 `D:\JavaWork\PhotoBooker\PhotoBooker`（端口 8099，`.\mvnw.cmd`），前端 `test` 子目录（dev 端口 3000，vite 将 `/photo`、`/api` 代理到 8099）。
- 测试/验证命令（Windows PowerShell）：
  - 单测：`.\mvnw.cmd test -Dtest=SearchNearbyPhotographerToolTest`（在 PhotoBooker 目录）
  - 后端编译：`.\mvnw.cmd -q -DskipTests compile`
  - 前端类型检查+构建：`npm run build`（vue-tsc + vite build，在 test 目录）
- 测试风格：JUnit 5 + Mockito（`@ExtendWith(MockitoExtension.class)`，见 `PhotographerGeoServiceImplTest`）。

---

### Task 1: SearchNearbyPhotographerTool 工具类（TDD）

**Files:**
- Create: `src/main/java/com/xhxi/photobooker/agent/tools/SearchNearbyPhotographerTool.java`
- Test: `src/test/java/com/xhxi/photobooker/agent/tools/SearchNearbyPhotographerToolTest.java`

**Interfaces:**
- Consumes: `PhotographerService.searchNearbyPhotographers(double latitude, double longitude, double radiusKm, int limit)` → `List<PhotographerNearbyVO>`（VO 字段：`id/name/phone/avatar/working/orderCount/latitude/longitude/distanceKm`，有 `@Builder`）
- Produces: `SearchNearbyPhotographerTool`（`@Component`，实现 `AgentTool` 接口）：`getName()`=`search_nearby_photographer`，`execute(Map<String,Object>)` 返回 `List<Map<String,Object>>`（键：id/name/phone/avatar/working/distanceKm）或错误文本 `String`。Task 2 的 `@Bean` 直接注入此类。

- [ ] **Step 1: 写失败测试**

创建 `src/test/java/com/xhxi/photobooker/agent/tools/SearchNearbyPhotographerToolTest.java`：

```java
package com.xhxi.photobooker.agent.tools;

import com.xhxi.photobooker.service.PhotographerService;
import com.xhxi.photobooker.vo.PhotographerNearbyVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchNearbyPhotographerToolTest {

    @Mock
    private PhotographerService photographerService;

    @InjectMocks
    private SearchNearbyPhotographerTool tool;

    @Test
    void execute_rejectsOutOfRangeLatitude() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("latitude", 91.0);
        params.put("longitude", 113.2771);
        params.put("radiusKm", 5.0);
        params.put("limit", 10);

        Object result = tool.execute(params);

        assertEquals("参数不合法: latitude 需在 [-90, 90]", result);
        verify(photographerService, never()).searchNearbyPhotographers(anyDouble(), anyDouble(), anyDouble(), anyInt());
    }

    @Test
    void execute_rejectsOutOfRangeLongitude() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("latitude", 23.1289);
        params.put("longitude", 181.0);

        Object result = tool.execute(params);

        assertEquals("参数不合法: longitude 需在 [-180, 180]", result);
        verify(photographerService, never()).searchNearbyPhotographers(anyDouble(), anyDouble(), anyDouble(), anyInt());
    }

    @Test
    void execute_rejectsInvalidRadius() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("latitude", 23.1289);
        params.put("longitude", 113.2771);
        params.put("radiusKm", 0.0);

        Object result = tool.execute(params);

        assertEquals("参数不合法: radiusKm 需在 (0, 100] km", result);
        verify(photographerService, never()).searchNearbyPhotographers(anyDouble(), anyDouble(), anyDouble(), anyInt());
    }

    @Test
    void execute_rejectsInvalidLimit() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("latitude", 23.1289);
        params.put("longitude", 113.2771);
        params.put("radiusKm", 5.0);
        params.put("limit", 51);

        Object result = tool.execute(params);

        assertEquals("参数不合法: limit 需在 [1, 50]", result);
        verify(photographerService, never()).searchNearbyPhotographers(anyDouble(), anyDouble(), anyDouble(), anyInt());
    }

    @Test
    void execute_usesDefaultsAndPassesParamsInOrder() throws Exception {
        when(photographerService.searchNearbyPhotographers(23.1289, 113.2771, 5.0, 10))
                .thenReturn(new ArrayList<>());

        Map<String, Object> params = new HashMap<>();
        params.put("latitude", 23.1289);
        params.put("longitude", 113.2771);

        Object result = tool.execute(params);

        verify(photographerService).searchNearbyPhotographers(23.1289, 113.2771, 5.0, 10);
        assertTrue(result instanceof List);
        assertTrue(((List<?>) result).isEmpty());
    }

    @Test
    void execute_mapsNearbyVoToResultMap() throws Exception {
        List<PhotographerNearbyVO> nearby = new ArrayList<>();
        nearby.add(PhotographerNearbyVO.builder()
                .id(1L).name("张三").phone("13800000000").avatar("a.png")
                .working(1).orderCount(5).latitude(23.13).longitude(113.28).distanceKm(1.23)
                .build());
        when(photographerService.searchNearbyPhotographers(23.13, 113.28, 5.0, 10)).thenReturn(nearby);

        Map<String, Object> params = new HashMap<>();
        params.put("latitude", 23.13);
        params.put("longitude", 113.28);

        Object result = tool.execute(params);

        List<?> list = (List<?>) result;
        assertEquals(1, list.size());
        Map<?, ?> m = (Map<?, ?>) list.get(0);
        assertEquals(1L, m.get("id"));
        assertEquals("张三", m.get("name"));
        assertEquals("13800000000", m.get("phone"));
        assertEquals("a.png", m.get("avatar"));
        assertEquals(1, m.get("working"));
        assertEquals(1.23, m.get("distanceKm"));
    }

    @Test
    void execute_rejectsNonNumericParameter() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("latitude", "abc");
        params.put("longitude", 113.2771);

        Object result = tool.execute(params);

        assertEquals("参数不合法: 数值格式错误", result);
        verify(photographerService, never()).searchNearbyPhotographers(anyDouble(), anyDouble(), anyDouble(), anyInt());
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `.\mvnw.cmd test -Dtest=SearchNearbyPhotographerToolTest`
Expected: 编译失败（`SearchNearbyPhotographerTool` 不存在）。

- [ ] **Step 3: 实现工具类**

创建 `src/main/java/com/xhxi/photobooker/agent/tools/SearchNearbyPhotographerTool.java`：

```java
package com.xhxi.photobooker.agent.tools;

import com.xhxi.photobooker.agent.AgentTool;
import com.xhxi.photobooker.service.PhotographerService;
import com.xhxi.photobooker.vo.PhotographerNearbyVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 附近摄影师检索工具：经纬度+半径 → 距离升序列表（含距离km）。复用 Service 层，不直接碰 Redis */
@Component
public class SearchNearbyPhotographerTool implements AgentTool {

    @Autowired
    private PhotographerService photographerService;

    @Override
    public String getName() {
        return "search_nearby_photographer";
    }

    @Override
    public String getDescription() {
        return "按经纬度和半径检索附近摄影师，返回按距离升序的列表（含距离km）。当用户想找附近/离我近/周边的摄影师时使用";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("latitude", "number - 纬度（必填）");
        schema.put("longitude", "number - 经度（必填）");
        schema.put("radiusKm", "number - 检索半径（公里，默认5）");
        schema.put("limit", "number - 返回数量限制（默认10）");
        return schema;
    }

    @Override
    public Object execute(Map<String, Object> parameters) {
        try {
            Double latitude = parseDouble(parameters.get("latitude"));
            Double longitude = parseDouble(parameters.get("longitude"));
            Double radiusKm = parseDouble(parameters.get("radiusKm"));
            Integer limit = parameters.get("limit") != null
                    ? Integer.parseInt(parameters.get("limit").toString()) : null;

            // 校验范围与 /nearby 接口一致；越界返回错误文本，LLM 直接转述
            if (latitude == null || latitude < -90 || latitude > 90) {
                return "参数不合法: latitude 需在 [-90, 90]";
            }
            if (longitude == null || longitude < -180 || longitude > 180) {
                return "参数不合法: longitude 需在 [-180, 180]";
            }
            if (radiusKm == null) {
                radiusKm = 5.0;
            }
            if (limit == null) {
                limit = 10;
            }
            if (radiusKm <= 0 || radiusKm > 100) {
                return "参数不合法: radiusKm 需在 (0, 100] km";
            }
            if (limit < 1 || limit > 50) {
                return "参数不合法: limit 需在 [1, 50]";
            }

            // 注意：Service 签名纬度在前
            List<PhotographerNearbyVO> nearby = photographerService
                    .searchNearbyPhotographers(latitude, longitude, radiusKm, limit);
            return nearby.stream().map(p -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", p.getId());
                map.put("name", p.getName());
                map.put("phone", p.getPhone());
                map.put("avatar", p.getAvatar());
                map.put("working", p.getWorking());
                map.put("distanceKm", p.getDistanceKm());
                return map;
            }).collect(Collectors.toList());
        } catch (NumberFormatException e) {
            return "参数不合法: 数值格式错误";
        }
    }

    private Double parseDouble(Object value) {
        return value == null ? null : Double.parseDouble(value.toString());
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `.\mvnw.cmd test -Dtest=SearchNearbyPhotographerToolTest`
Expected: 7 个测试全部 PASS。

- [ ] **Step 5: 全量回归**

Run: `.\mvnw.cmd -q test`
Expected: BUILD SUCCESS（含既有 GEO 5 个测试）。

---

### Task 2: AgentToolFunctionConfig 注册 Function Bean

**Files:**
- Modify: `src/main/java/com/xhxi/photobooker/config/AgentToolFunctionConfig.java`（imports、`@Bean`、record）

**Interfaces:**
- Consumes: Task 1 的 `SearchNearbyPhotographerTool`（`execute(Map<String,Object>)`）
- Produces: Spring AI Function Bean `searchNearbyPhotographer`（Bean 名即工具名），Task 3 在 `AiChatController` 中按该名字符串注册。

- [ ] **Step 1: 添加 import 与 @Bean**

在 `AgentToolFunctionConfig.java` 的 import 区加：

```java
import com.xhxi.photobooker.agent.tools.SearchNearbyPhotographerTool;
```

在 `searchPortfolio` Bean 之后（`// ==================== 辅助方法 ====================` 注释之前）插入：

```java
    /**
     * 查找附近摄影师。基于 Redis GEO 距离检索，返回带距离的列表。
     */
    @Bean
    @Description("按经纬度和半径检索附近摄影师，返回按距离升序的列表（含距离km）。当用户想找附近/离我近/周边的摄影师时使用")
    public Function<SearchNearbyPhotographerRequest, List<Map<String, Object>>> searchNearbyPhotographer(
            SearchNearbyPhotographerTool tool) {
        return request -> executeTool(tool, request.toParamMap());
    }
```

- [ ] **Step 2: 添加请求 record**

在 `SearchPortfolioRequest` record 之后插入：

```java
    public record SearchNearbyPhotographerRequest(
            Double latitude,
            Double longitude,
            Double radiusKm,
            Integer limit
    ) {
        Map<String, Object> toParamMap() {
            Map<String, Object> m = new HashMap<>();
            putIfNotNull(m, "latitude", latitude);
            putIfNotNull(m, "longitude", longitude);
            putIfNotNull(m, "radiusKm", radiusKm);
            putIfNotNull(m, "limit", limit);
            return m;
        }
    }
```

- [ ] **Step 3: 编译验证**

Run: `.\mvnw.cmd -q -DskipTests compile`
Expected: BUILD SUCCESS（无编译错误）。

---

### Task 3: AiChatController 注册工具 + 系统提示词

**Files:**
- Modify: `src/main/java/com/xhxi/photobooker/controller/AiChatController.java`

**Interfaces:**
- Consumes: Task 2 的 Bean 名 `searchNearbyPhotographer`（字符串）。
- Produces: 聊天中 LLM 可调用 `searchNearbyPhotographer` 工具。

- [ ] **Step 1: 工具数组追加工具名**

`PUBLIC_TOOL_FUNCTIONS`（约 45-50 行）改为：

```java
    private static final String[] PUBLIC_TOOL_FUNCTIONS = {
            "searchPhotographer",
            "searchPackages",
            "checkAvailability",
            "searchPortfolio",
            "searchNearbyPhotographer"
    };
```

`ALL_TOOL_FUNCTIONS`（约 55-62 行）改为：

```java
    private static final String[] ALL_TOOL_FUNCTIONS = {
            "searchPhotographer",
            "searchPortfolio",
            "searchPackages",
            "checkAvailability",
            "createOrder",
            "updateMySelfInfo",
            "searchNearbyPhotographer"
    };
```

- [ ] **Step 2: 登录段系统提示词追加工具说明**

`buildAgentSystemPrompt` 中登录段"可用工具"列表（现为 1-6 项，以 `prompt.append("6. updateMySelfInfo - ...")` 结尾，其后是 `\n\n` 和约拍强制流程），把结尾改成：

```java
            prompt.append("6. updateMySelfInfo - 更新个人信息（参数: realName, phone, gender, birthday, avatar）\n");
            prompt.append("7. searchNearbyPhotographer - 查找附近摄影师（参数: latitude纬度, longitude经度, radiusKm半径km, limit数量）\n");
            prompt.append("   触发规则：用户说\"附近/离我近/周边\"时使用；用户给了城市或地标时按其大致经纬度调用；用户没说位置时必须先反问用户所在位置，禁止编造坐标\n\n");
```

- [ ] **Step 3: 未登录段系统提示词追加工具说明**

未登录段"可用工具"列表（现为 1-4 项，以 `prompt.append("4. checkAvailability - ...")` 结尾，其后是 `\n\n`），把结尾改成：

```java
            prompt.append("4. checkAvailability - 检查档期（参数: photographerId, requestedTime时间）\n");
            prompt.append("5. searchNearbyPhotographer - 查找附近摄影师（参数: latitude纬度, longitude经度, radiusKm半径km, limit数量）\n");
            prompt.append("   触发规则：用户说\"附近/离我近/周边\"时使用；用户给了城市或地标时按其大致经纬度调用；用户没说位置时必须先反问用户所在位置，禁止编造坐标\n\n");
```

- [ ] **Step 4: 编译验证**

Run: `.\mvnw.cmd -q -DskipTests compile`
Expected: BUILD SUCCESS。

---

### Task 4: 前端页面 PhotographerNearby.vue

**Files:**
- Create: `test/src/views/PhotographerNearby.vue`

**Interfaces:**
- Consumes: `GET /photo/photographer/nearby?latitude&longitude&radius&limit`（经 vite `/photo` 代理 → 8099），返回 `Result<List<PhotographerNearbyVO>>`；`api.get` 拦截器已解包，`res.data` 即 VO 数组（`id/name/phone/avatar/working/orderCount/distanceKm`）。
- Produces: 路由组件 `PhotographerNearby`（Task 5 注册路由，卡片点击跳 `/photographer/:id`）。

- [ ] **Step 1: 创建页面**

创建 `test/src/views/PhotographerNearby.vue`（模板/脚本/style 三段完整内容）：

```vue
<template>
  <div class="photographer-nearby">
    <div class="nearby-header">
      <h2>附近摄影师</h2>
      <div class="nearby-controls">
        <span class="nearby-location" v-if="locationLabel">
          <el-icon><Location /></el-icon> {{ locationLabel }}
        </span>
        <el-select v-model="radius" style="width: 110px" @change="reSearch">
          <el-option v-for="r in radiusOptions" :key="r" :label="`${r} km`" :value="r" />
        </el-select>
        <el-button type="primary" @click="locate" :loading="loading">重新定位</el-button>
      </div>
    </div>

    <div v-if="loading" class="nearby-loading">
      <el-icon class="is-loading" :size="28"><Loading /></el-icon>
      <p>正在定位并查找附近的摄影师...</p>
    </div>

    <el-empty v-else-if="!located" description="未获取到位置，无法检索附近摄影师" />

    <el-empty v-else-if="nearbyList.length === 0" description="附近暂无摄影师" />

    <div v-else class="nearby-grid">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="8" v-for="p in nearbyList" :key="p.id">
          <el-card class="nearby-card" @click="goToDetail(p.id)">
            <div class="nearby-card-header">
              <img :src="p.avatar || defaultAvatar" class="nearby-avatar" />
              <div class="nearby-info">
                <h3>{{ p.name }}</h3>
                <el-tag :type="p.working === 1 ? 'success' : 'info'" size="small">
                  {{ p.working === 1 ? '接单中' : '休息中' }}
                </el-tag>
              </div>
            </div>
            <div class="nearby-details">
              <p class="nearby-distance">
                <el-icon><Location /></el-icon> 距离 {{ formatDistance(p.distanceKm) }}
              </p>
              <p class="nearby-orders">完成订单数：{{ p.orderCount || 0 }}</p>
              <p class="nearby-phone"><el-icon><Phone /></el-icon> {{ p.phone }}</p>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <el-dialog v-model="manualVisible" title="手动输入位置" width="420px">
      <el-form label-width="80px">
        <el-form-item label="纬度">
          <el-input v-model="manualLat" placeholder="纬度 [-90, 90]，如 23.1289" />
        </el-form-item>
        <el-form-item label="经度">
          <el-input v-model="manualLng" placeholder="经度 [-180, 180]，如 113.2771" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="manualVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmManual">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../utils/api'

const router = useRouter()

const radiusOptions = [1, 3, 5, 10, 20]
const radius = ref(5)
const loading = ref(false)
const located = ref(false)
const locationLabel = ref('')
const nearbyList = ref<any[]>([])
const defaultAvatar =
  'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=200&h=200&fit=crop&crop=face'

const manualVisible = ref(false)
const manualLat = ref('23.1289')
const manualLng = ref('113.2771')

const formatDistance = (km: number) => (km != null ? km.toFixed(2) : '-') + ' km'

const goToDetail = (id: number) => router.push(`/photographer/${id}`)

const searchNearby = async (lat: number, lng: number) => {
  loading.value = true
  try {
    const res = await api.get('/photo/photographer/nearby', {
      params: { latitude: lat, longitude: lng, radius: radius.value, limit: 20 }
    })
    nearbyList.value = res.data || []
    located.value = true
  } catch (e) {
    // api.ts 响应拦截器已统一 ElMessage 提示
  } finally {
    loading.value = false
  }
}

const reSearch = async () => {
  if (located.value && locationLabel.value) {
    const [lat, lng] = locationLabel.value.split(', ').map(Number)
    await searchNearby(lat, lng)
  }
}

const locate = () => {
  loading.value = true
  if (!navigator.geolocation) {
    loading.value = false
    manualVisible.value = true
    return
  }
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      const lat = pos.coords.latitude
      const lng = pos.coords.longitude
      locationLabel.value = `${lat.toFixed(4)}, ${lng.toFixed(4)}`
      searchNearby(lat, lng)
    },
    () => {
      loading.value = false
      manualVisible.value = true
    },
    { timeout: 10000, maximumAge: 300000 }
  )
}

const confirmManual = () => {
  const lat = parseFloat(manualLat.value)
  const lng = parseFloat(manualLng.value)
  if (Number.isNaN(lat) || Number.isNaN(lng) || lat < -90 || lat > 90 || lng < -180 || lng > 180) {
    ElMessage.error('经纬度不合法')
    return
  }
  manualVisible.value = false
  locationLabel.value = `${lat.toFixed(4)}, ${lng.toFixed(4)}`
  searchNearby(lat, lng)
}

onMounted(() => locate())
</script>

<style scoped>
.photographer-nearby {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 16px 48px;
}

.nearby-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 24px;
}

.nearby-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.nearby-location {
  color: #666;
  font-size: 13px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.nearby-loading {
  text-align: center;
  padding: 48px 0;
  color: #666;
}

.nearby-card {
  cursor: pointer;
  margin-bottom: 20px;
  transition: transform 0.3s;
}

.nearby-card:hover {
  transform: translateY(-5px);
}

.nearby-card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.nearby-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  object-fit: cover;
}

.nearby-info h3 {
  margin: 0 0 6px;
  color: #333;
}

.nearby-details p {
  margin: 6px 0;
  color: #666;
  font-size: 14px;
}

.nearby-distance {
  color: #409eff;
  font-weight: bold;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
</style>
```

- [ ] **Step 2: 类型检查**

Run（在 `test` 目录）: `npm run build`
Expected: `vue-tsc` 通过、vite build 成功（现有页面本就可构建，新页面不应引入新错误）。

---

### Task 5: 路由注册 + 列表页按钮 + 导航栏入口

**Files:**
- Modify: `test/src/router/index.ts`
- Modify: `test/src/views/PhotographerList.vue`
- Modify: `test/src/components/NavBar.vue`

**Interfaces:**
- Consumes: Task 4 的 `PhotographerNearby.vue`。
- Produces: 可访问的路由 `/photographer-nearby`（公开，未登录可看）+ 两处入口。

- [ ] **Step 1: 注册路由**

`test/src/router/index.ts`：

import 区（`import PhotographerList from '../views/PhotographerList.vue'` 之后）加：

```ts
import PhotographerNearby from '../views/PhotographerNearby.vue'
```

routes 数组（`/photographer-list` 路由之后）加：

```ts
  {
    path: '/photographer-nearby',
    name: 'photographer-nearby',
    component: PhotographerNearby
  },
```

`publicPages`（约 140 行）改为：

```ts
const publicPages = ['home', 'login', 'register', 'photographer-list', 'photographer-nearby', 'photographer-detail', 'PortfolioDetail', 'technical-notes']
```

- [ ] **Step 2: 列表页加按钮**

`test/src/views/PhotographerList.vue` 第 5 行（"地图查看"按钮之后）加：

```html
      <button @click="$router.push('/photographer-nearby')" style="padding: 6px 18px; background: #67C23A; color: #fff; border: none; border-radius: 4px; cursor: pointer; margin-left: 8px;">附近摄影师</button>
```

- [ ] **Step 3: 导航栏加入口**

`test/src/components/NavBar.vue`，"摄影师"菜单项（约 16-19 行）之后加（图标已全局注册，无需 import）：

```html
    <el-menu-item index="/photographer-nearby">
      <el-icon><Location /></el-icon>
      <span>附近摄影师</span>
    </el-menu-item>
```

- [ ] **Step 4: 类型检查**

Run（在 `test` 目录）: `npm run build`
Expected: BUILD 成功。

---

### Task 6: 端到端手动验证

**Files:**
- 无代码变更；验证 Task 1-5 的整体效果。

- [ ] **Step 1: 启动后端**

Run（在 `PhotoBooker` 目录，需先确保 Redis 已启动，端口按 `application.yml` 默认）:
`.\mvnw.cmd spring-boot:run`
Expected: 日志出现 `摄影师地理位置索引初始化完成，共同步 N 个摄影师`。

- [ ] **Step 2: 启动前端**

Run（在 `test` 目录）: `npm run dev`
Expected: 3000 端口可访问。

- [ ] **Step 3: 验证附近检索接口直连**

Run:
```powershell
curl "http://localhost:8099/photo/photographer/nearby?latitude=23.1289&longitude=113.2771&radius=5&limit=20"
```
Expected: `code=1`，data 为带 `distanceKm` 的摄影师列表（或空数组，取决于测试库数据）。

- [ ] **Step 4: 验证页面流程（浏览器手动操作）**

浏览器打开 `http://localhost:3000/#/photographer-nearby`：
1. 授权定位 → 页面显示坐标标签与卡片列表（距离 X.XX km、接单中/休息中 tag）
2. 拒绝定位（或非 HTTPS 环境自动触发）→ 弹出手动输入框，预填 23.1289/113.2771 → 确定后出结果
3. 切换半径 1/3/5/10/20 km → 列表变化（1km 可能为空 → 显示"附近暂无摄影师"）
4. 点卡片 → 跳转 `/photographer/:id` 详情页
5. 列表页"附近摄影师"绿色按钮、导航栏"附近摄影师"入口均可到达本页
6. 未登录状态直接访问该路由不被拦截（publicPages 生效）

- [ ] **Step 5: 验证 AI 聊天触发工具**

前端右下角"小影"对话框：
1. 输入"帮我找附近5公里的摄影师" → AI 反问位置（未给位置时应反问，不编造坐标）
2. 输入"我在广州天河体育中心，附近有摄影师吗" → AI 调用工具并返回带距离的摄影师（或"附近暂时没有摄影师"）
3. 输入"附近有摄影师吗"后回复具体城市名，确认第二轮能正常触发

- [ ] **Step 6: 回归确认**

回到既有页面确认无回归：列表页（筛选/地图按钮）、首页（热门摄影师、AI 对话框）、导航栏跳转。Expected: 全部正常。
