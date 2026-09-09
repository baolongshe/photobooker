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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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

    @Test
    void run_swallowsGeoSyncFailureAndKeepsStartupAlive() {
        // Redis 不可达时全量同步抛异常 → run() 捕获后不中断启动
        List<Photographer> all = List.of(Photographer.builder().id(1L).build());
        when(photographerService.getAllPhotographers()).thenReturn(all);
        when(photographerGeoService.syncFromDb(all)).thenThrow(new RuntimeException("Redis 不可达"));

        assertDoesNotThrow(() -> initializer.run());
        assertEquals(0, initializer.getSyncedCount());
    }
}
