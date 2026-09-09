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
import static org.mockito.Mockito.doThrow;
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
    void updatePhotographer_partialUpdateMergesDbCoordinatesIntoGeoIndex() {
        // 部分字段更新（body 未传坐标）→ 以 DB 现坐标合并，GEO member 保留而非被删除
        Photographer existing = Photographer.builder().id(7L).userId(100L)
                .latitude(23.125178).longitude(113.280637).build();
        when(photographerMapper.selectByUserId(100L)).thenReturn(existing);
        Photographer update = Photographer.builder().id(7L).userId(100L)
                .name("新名字").build();

        service.updatePhotographer(100L, update);

        verify(photographerMapper).updateById(update);
        verify(photographerGeoService).add(7L, 113.280637, 23.125178);
        verify(photographerGeoService, never()).remove(any());
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
    void deletePhotographer_geoRemoveFailureDoesNotAffectDbDelete() {
        when(photographerMapper.deleteById(7L)).thenReturn(1);
        doThrow(new RuntimeException("Redis down")).when(photographerGeoService).remove(7L);

        boolean result = service.deletePhotographer(7L);

        assertTrue(result);
    }

    @Test
    void deletePhotographer_skipsGeoRemoveWhenDbDeleteFails() {
        when(photographerMapper.deleteById(7L)).thenReturn(0);

        boolean result = service.deletePhotographer(7L);

        assertFalse(result);
        verify(photographerGeoService, never()).remove(any());
    }
}
