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
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
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
        // add_skipsNullCoordinate 不经过 opsForGeo()，需 lenient 避免 UnnecessaryStubbingException
        lenient().when(stringRedisTemplate.opsForGeo()).thenReturn(geoOps);
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
    void add_skipsLatitudeOutsideRedisBoundary() {
        // Redis GEOADD 纬度边界为 ±85.05112878，越界值直接跳过，避免 Redis 侧报错
        geoService.add(100L, 113.280637, 86.0);

        verify(geoOps, never()).add(anyString(), any(Point.class), anyString());
    }

    @Test
    void remove_deletesMember() {
        geoService.remove(100L);

        verify(geoOps).remove(GEO_KEY, "100");
    }

    @Test
    void searchNearby_returnsSortedWithDistance() {
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> mockResults = new ArrayList<>();
        mockResults.add(new GeoResult<>(new RedisGeoCommands.GeoLocation<>("5", new Point(0, 0)), new Distance(1.0, Metrics.KILOMETERS)));
        mockResults.add(new GeoResult<>(new RedisGeoCommands.GeoLocation<>("9", new Point(0, 0)), new Distance(3.5, Metrics.KILOMETERS)));
        when(geoOps.radius(eq(GEO_KEY), any(Circle.class), any(RedisGeoCommands.GeoRadiusCommandArgs.class)))
                .thenReturn(new GeoResults<>(mockResults));

        List<GeoDistanceResult> result = geoService.searchNearby(113.28, 23.12, 5, 20);

        assertEquals(2, result.size());
        assertEquals(5L, result.get(0).getPhotographerId());
        assertEquals(1.0, result.get(0).getDistanceKm());
        assertEquals(9L, result.get(1).getPhotographerId());
        assertEquals(3.5, result.get(1).getDistanceKm());
    }

    @Test
    void searchNearby_returnsEmptyWhenKeyMissing() {
        when(geoOps.radius(eq(GEO_KEY), any(Circle.class), any(RedisGeoCommands.GeoRadiusCommandArgs.class)))
                .thenReturn(new GeoResults<>(new ArrayList<>()));

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
