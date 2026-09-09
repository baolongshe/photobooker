package com.xhxi.photobooker.service;

import com.xhxi.photobooker.entity.Photographer;
import com.xhxi.photobooker.vo.PhotographerNearbyVO;

import java.util.List;

public interface PhotographerService {
    Photographer savePhotographer(Photographer photographer);
    Photographer findPhotographerById(Long id);
    List<Photographer> findAllPhotographer();
    Photographer updatePhotographer(Long id, Photographer photographer);
    boolean deletePhotographer(Long id);

    Photographer findByUserId(Long userId);
    
    // 更新摄影师的完成订单数
    void updateOrderCount(Long photographerId);

    List<Photographer> getAllPhotographers();

    // 附近摄影师检索：GEO 索引取 id+距离 → 批量回表 → 过滤不可见 → 按距离升序
    List<PhotographerNearbyVO> searchNearbyPhotographers(double latitude, double longitude, double radiusKm, int limit);
}