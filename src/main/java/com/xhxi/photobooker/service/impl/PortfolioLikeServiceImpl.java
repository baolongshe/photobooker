package com.xhxi.photobooker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xhxi.photobooker.entity.Portfolio;
import com.xhxi.photobooker.entity.PortfolioLike;
import com.xhxi.photobooker.mapper.PortfolioLikeMapper;
import com.xhxi.photobooker.service.PortfolioLikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class PortfolioLikeServiceImpl implements PortfolioLikeService {
    @Autowired
    private PortfolioLikeMapper likeMapper;
//设置作品收藏

    public boolean like(Long userId, Long portfolioId) {
        if (hasLiked(userId, portfolioId)) return false;
        PortfolioLike like = new PortfolioLike();
        like.setUserId(userId);
        like.setPortfolioId(portfolioId);
        like.setFavoriteType((byte)1);  // 1=作品
        like.setCreateTime(new Date());
        likeMapper.insert(like);
        return true;
    }

    @Override
    public boolean likeOrderPhoto(Long userId, Long orderPhotoId, String photoUrl, String thumbnailUrl, String photoNumber, Long orderId) {
        // 检查是否已收藏
        PortfolioLike existing = findByOrderPhotoId(userId, orderPhotoId);
        if (existing != null) {
            return false;
        }
        
        PortfolioLike like = new PortfolioLike();
        like.setUserId(userId);
        like.setOrderPhotoId(orderPhotoId);
        like.setOrderId(orderId);  // 设置订单ID
        like.setPortfolioId(null);  // 设置为 null
        like.setFavoriteType((byte)2);  // 2=订单照片
        like.setPhotoUrl(photoUrl);
        like.setThumbnailUrl(thumbnailUrl);
        like.setPhotoNumber(photoNumber);
        like.setCreateTime(new Date());
        likeMapper.insert(like);
        return true;
    }

    public boolean unlike(Long userId, Long portfolioId) {
        return likeMapper.delete(
                new QueryWrapper<PortfolioLike>()
                        .eq("user_id", userId)
                        .eq("portfolio_id", portfolioId)
        ) > 0;
    }

    @Override
    public boolean unlikeOrderPhoto(Long userId, Long orderPhotoId) {
        return likeMapper.delete(
            new QueryWrapper<PortfolioLike>()
                .eq("user_id", userId)
                .eq("order_photo_id", orderPhotoId)
        ) > 0;
    }

    @Override
    public PortfolioLike findByOrderPhotoId(Long userId, Long orderPhotoId) {
        return likeMapper.selectOne(
            new QueryWrapper<PortfolioLike>()
                .eq("user_id", userId)
                .eq("order_photo_id", orderPhotoId)
        );
    }

    @Override
    public boolean hasLiked(Long userId, Long portfolioId) {
        return likeMapper.countByUserAndPortfolio(userId, portfolioId) > 0;
    }

    //获取用户喜欢和收藏的作品
    @Override
    public List<Portfolio> findPortfoliosByUserId(Long userId) {
        return likeMapper.findPortfoliosByUserId(userId);

    }

    @Override
    public List<PortfolioLike> findOrderPhotosByUserId(Long userId) {
        LambdaQueryWrapper<PortfolioLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortfolioLike::getUserId, userId)
               .eq(PortfolioLike::getFavoriteType, (byte)2)  // 只查询订单照片类型
               .orderByDesc(PortfolioLike::getCreateTime);
        return likeMapper.selectList(wrapper);
    }
}