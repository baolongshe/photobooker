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
                        new GeoDistanceResult(9L, 0.5),
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
        // GEO 返回了 1、3、9 三个（9 距离最近 0.5km），DB 批量查出三个（含不可见的 9）
        when(photographerMapper.selectBatchIds(anyCollection())).thenReturn(List.of(p1, p3, pHidden));

        List<PhotographerNearbyVO> result = service.searchNearbyPhotographers(23.12, 113.28, 5, 20);

        // 顺序按 GEO 距离升序：9(0.5km) 距离最近但 locationVisible=0 被过滤，故为 3(1.2km) → 1(2.7km)
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
