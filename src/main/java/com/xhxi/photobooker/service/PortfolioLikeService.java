package com.xhxi.photobooker.service;


import com.xhxi.photobooker.entity.Portfolio;
import com.xhxi.photobooker.entity.PortfolioLike;


import java.util.List;

public interface PortfolioLikeService {
    boolean like(Long userId, Long portfolioId);
    
    /**
     * 收藏订单照片
     */
    boolean likeOrderPhoto(Long userId, Long orderPhotoId, String photoUrl, String thumbnailUrl, String photoNumber, Long orderId);
    
    boolean unlike(Long userId, Long portfolioId);
    boolean hasLiked(Long userId, Long portfolioId);
    List<Portfolio> findPortfoliosByUserId(Long userId);

    /**
     * 查询用户收藏的订单照片
     */
    List<PortfolioLike> findOrderPhotosByUserId(Long userId);
    
    /**
     * 根据订单照片 ID 查询收藏记录
     */
    PortfolioLike findByOrderPhotoId(Long userId, Long orderPhotoId);
    
    /**
     * 取消收藏订单照片
     */
    boolean unlikeOrderPhoto(Long userId, Long orderPhotoId);

}