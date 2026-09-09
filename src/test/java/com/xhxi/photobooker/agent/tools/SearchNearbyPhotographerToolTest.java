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
