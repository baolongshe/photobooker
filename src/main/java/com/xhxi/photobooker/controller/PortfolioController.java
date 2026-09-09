package com.xhxi.photobooker.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xhxi.photobooker.entity.Portfolio;
import com.xhxi.photobooker.entity.PortfolioLike;
import com.xhxi.photobooker.result.Result;
import com.xhxi.photobooker.service.PortfolioLikeService;
import com.xhxi.photobooker.service.PortfolioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 摄影师作品控制器
 */
@RestController
@RequestMapping("/portfolio")
public class PortfolioController {
    
    @Autowired
    private PortfolioService portfolioService;
    
    @Autowired
    private PortfolioLikeService portfolioLikeService;


    /**
     * 创建作品
     */
    @PostMapping
    public Result<Portfolio> createPortfolio(@RequestBody Portfolio portfolio) {
        try {
            Portfolio savedPortfolio = portfolioService.savePortfolio(portfolio);
            return Result.success(savedPortfolio);
        } catch (Exception e) {
            return Result.error("创建作品失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据ID查询作品
     */
    @GetMapping("/{id}")
    public Result<?> getPortfolioById(@PathVariable Long id, @RequestParam(required = false) Long userId) {
        try {
            // 增加浏览量
            portfolioService.incrementViewCount(id);
            Portfolio portfolio = portfolioService.findPortfolioById(id);
            if (portfolio == null) {
                return Result.error("作品不存在");
            }
            // 构造返回数据，包含isLiked字段
            Map<String, Object> result = new HashMap<>();
            result.put("id", portfolio.getId());
            result.put("photographerId", portfolio.getPhotographerId());
            result.put("title", portfolio.getTitle());
            result.put("description", portfolio.getDescription());
            result.put("category", portfolio.getCategory());
            result.put("tags", portfolio.getTags());
            result.put("coverImage", portfolio.getCoverImage());
            result.put("imageUrls", portfolio.getImageUrls());
            result.put("shootingDate", portfolio.getShootingDate());
            result.put("shootingLocation", portfolio.getShootingLocation());
            result.put("equipment", portfolio.getEquipment());
            result.put("status", portfolio.getStatus());
            result.put("viewCount", portfolio.getViewCount());
            result.put("likeCount", portfolio.getLikeCount());
            result.put("isFeatured", portfolio.getIsFeatured());
            result.put("sortWeight", portfolio.getSortWeight());
            result.put("createTime", portfolio.getCreateTime());
            result.put("updateTime", portfolio.getUpdateTime());
            boolean isLiked = false;
            if (userId != null) {
                isLiked = portfolioLikeService.hasLiked(userId, id);
            }
            result.put("isLiked", isLiked);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("查询作品失败: " + e.getMessage());
        }
    }


    /**
     * 根据摄影师ID分页查询作品
     */
    @GetMapping("/photographer/{photographerId}")
    public Result<IPage<Portfolio>> getPortfoliosByPhotographerId(
            @PathVariable Long photographerId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        try {
            IPage<Portfolio> portfolios = portfolioService.findPortfoliosByPhotographerId(photographerId, page, size);
            return Result.success(portfolios);
        } catch (Exception e) {
            return Result.error("查询作品失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据摄影师ID查询精选作品
     */
    @GetMapping("/photographer/{photographerId}/featured")
    public Result<List<Portfolio>> getFeaturedPortfoliosByPhotographerId(
            @PathVariable Long photographerId,
            @RequestParam(defaultValue = "6") Integer limit) {
        try {
            List<Portfolio> portfolios = portfolioService.findFeaturedPortfoliosByPhotographerId(photographerId, limit);
            return Result.success(portfolios);
        } catch (Exception e) {
            return Result.error("查询精选作品失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据分类查询作品
     */
    @GetMapping("/category/{category}")
    public Result<List<Portfolio>> getPortfoliosByCategory(@PathVariable String category) {
        try {
            List<Portfolio> portfolios = portfolioService.findPortfoliosByCategory(category);
            return Result.success(portfolios);
        } catch (Exception e) {
            return Result.error("查询作品失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据标签搜索作品
     */
    @GetMapping("/search/tag")
    public Result<List<Portfolio>> searchPortfoliosByTag(@RequestParam String tag) {
        try {
            List<Portfolio> portfolios = portfolioService.findPortfoliosByTag(tag);
            return Result.success(portfolios);
        } catch (Exception e) {
            return Result.error("搜索作品失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新作品
     */
    @PutMapping("/{id}")
    public Result<Portfolio> updatePortfolio(@PathVariable Long id, @RequestBody Portfolio portfolio) {
        try {
            Portfolio updatedPortfolio = portfolioService.updatePortfolio(id, portfolio);
            return Result.success(updatedPortfolio);
        } catch (Exception e) {
            return Result.error("更新作品失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除作品
     */
    @DeleteMapping("/{id}")
    public Result<String> deletePortfolio(@PathVariable Long id) {
        try {
            boolean success = portfolioService.deletePortfolio(id);
            if (success) {
                return Result.success("删除成功");
            } else {
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            return Result.error("删除作品失败: " + e.getMessage());
        }
    }
    
    /**
     * 设置作品为精选
     */
    @PutMapping("/{id}/featured")
    public Result<String> setFeatured(@PathVariable Long id, @RequestParam Boolean isFeatured) {
        try {
            boolean success = portfolioService.setFeatured(id, isFeatured);
            if (success) {
                return Result.success(isFeatured ? "设为精选成功" : "取消精选成功");
            } else {
                return Result.error("操作失败");
            }
        } catch (Exception e) {
            return Result.error("操作失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查用户是否已点赞
     */
    @GetMapping("/like/check")
    public Result<Map<String, Boolean>> checkLikeStatus(
            @RequestParam Long userId,
            @RequestParam Long portfolioId) {
        boolean hasLiked = portfolioLikeService.hasLiked(userId, portfolioId);
        Map<String, Boolean> result = new HashMap<>();
        result.put("favorited", hasLiked);
        return Result.success(result);
    }

    /**
     * 点赞作品
     */
    @PostMapping("/{id}/like")
    public Result<String> likePortfolio(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        if (portfolioLikeService.hasLiked(userId, id)) {
            return Result.error("你已经点过赞了");
        }
        boolean success = portfolioLikeService.like(userId, id);
        if (success) {
            portfolioService.incrementLikeCount(id);
            return Result.success("点赞成功");
        } else {
            return Result.error("点赞失败");
        }
    }
    
    /**
     * 取消点赞作品
     */
    @DeleteMapping("/{id}/like")
    public Result<String> unlikePortfolio(@PathVariable Long id, @RequestParam Long userId) {
        boolean success = portfolioLikeService.unlike(userId, id);
        if (success) {
            portfolioService.decrementLikeCount(id);
            return Result.success("取消点赞成功");
        } else {
            return Result.error("取消点赞失败");
        }
    }
    
    /**
     * 收藏订单照片
     */
    @PostMapping("/order-photo/like")
    public Result<String> likeOrderPhoto(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        Long orderPhotoId = Long.valueOf(body.get("orderPhotoId").toString());
        String photoUrl = (String) body.get("photoUrl");
        String thumbnailUrl = (String) body.get("thumbnailUrl");
        String photoNumber = (String) body.get("photoNumber");
        Long orderId = body.get("orderId") != null ? Long.valueOf(body.get("orderId").toString()) : null;
        
        boolean success = portfolioLikeService.likeOrderPhoto(
            userId, orderPhotoId, photoUrl, thumbnailUrl, photoNumber, orderId
        );
        
        if (success) {
            return Result.success("收藏成功");
        } else {
            return Result.error("你已经收藏过这张照片了");
        }
    }
    
    /**
     * 取消收藏订单照片
     */
    @DeleteMapping("/order-photo/like")
    public Result<String> unlikeOrderPhoto(
            @RequestParam Long userId,
            @RequestParam Long orderPhotoId) {
        boolean success = portfolioLikeService.unlikeOrderPhoto(userId, orderPhotoId);
        if (success) {
            return Result.success("取消收藏成功");
        } else {
            return Result.error("取消收藏失败");
        }
    }
    
    /**
     * 检查用户是否已收藏订单照片
     */
    @GetMapping("/order-photo/like/check")
    public Result<Map<String, Boolean>> checkOrderPhotoLikeStatus(
            @RequestParam Long userId,
            @RequestParam Long orderPhotoId) {
        PortfolioLike existing = portfolioLikeService.findByOrderPhotoId(userId, orderPhotoId);
        Map<String, Boolean> result = new HashMap<>();
        result.put("favorited", existing != null);
        return Result.success(result);
    }
    
    /**
     * 查询用户收藏的所有订单照片
     */
    @GetMapping("/order-photo/like/user/{userId}")
    public Result<List<PortfolioLike>> getUserFavoritePhotos(@PathVariable Long userId) {
        try {
            List<PortfolioLike> favoritePhotos = portfolioLikeService.findOrderPhotosByUserId(userId);
            return Result.success(favoritePhotos);
        } catch (Exception e) {
            return Result.error("查询收藏照片失败：" + e.getMessage());
        }
    }
    
    /**
     * 更新作品状态
     */
    @PutMapping("/{id}/status")
    public Result<String> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        try {
            boolean success = portfolioService.updateStatus(id, status);
            if (success) {
                return Result.success("状态更新成功");
            } else {
                return Result.error("状态更新失败");
            }
        } catch (Exception e) {
            return Result.error("状态更新失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新排序权重
     */
    @PutMapping("/{id}/sort")
    public Result<String> updateSortWeight(@PathVariable Long id, @RequestParam Integer sortWeight) {
        try {
            boolean success = portfolioService.updateSortWeight(id, sortWeight);
            if (success) {
                return Result.success("排序权重更新成功");
            } else {
                return Result.error("排序权重更新失败");
            }
        } catch (Exception e) {
            return Result.error("排序权重更新失败: " + e.getMessage());
        }
    }


}