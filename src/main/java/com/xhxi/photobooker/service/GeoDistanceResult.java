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
