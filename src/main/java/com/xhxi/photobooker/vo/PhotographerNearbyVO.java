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
