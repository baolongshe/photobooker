package com.xhxi.photobooker.service.impl;

import com.xhxi.photobooker.entity.Photographer;
import com.xhxi.photobooker.service.GeoDistanceResult;
import com.xhxi.photobooker.service.PhotographerGeoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PhotographerGeoServiceImpl implements PhotographerGeoService {

    private static final String GEO_KEY = "photographer:geo";

    /** Redis GEOADD 对纬度的实际边界（geohash 实现限制），超出会在 Redis 侧报错 */
    private static final double MAX_VALID_LATITUDE = 85.05112878;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void add(Long photographerId, Double longitude, Double latitude) {
        if (photographerId == null || !isValidCoordinate(longitude, latitude)) {
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
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                .includeDistance()
                .sortAscending()
                .limit(limit);
        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                stringRedisTemplate.opsForGeo().radius(GEO_KEY, circle, args);

        List<GeoDistanceResult> nearby = new ArrayList<>();
        if (results != null) {
            for (GeoResult<RedisGeoCommands.GeoLocation<String>> r : results) {
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
                && latitude >= -MAX_VALID_LATITUDE && latitude <= MAX_VALID_LATITUDE
                && longitude >= -180 && longitude <= 180;
    }
}
