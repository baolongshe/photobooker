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
