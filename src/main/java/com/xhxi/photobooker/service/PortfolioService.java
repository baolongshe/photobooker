package com.xhxi.photobooker.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xhxi.photobooker.entity.Portfolio;

import java.util.List;

/**
 * 摄影师作品服务接口
 */
public interface PortfolioService extends IService<Portfolio> {
    
    /**
     * 保存作品
     */
    Portfolio savePortfolio(Portfolio portfolio);


    /**
     * 根据ID查询作品
     */
    Portfolio findPortfolioById(Long id);
    
    /**
     * 根据摄影师ID分页查询作品
     */
    IPage<Portfolio> findPortfoliosByPhotographerId(Long photographerId, Integer page, Integer size);
    
    /**
     * 根据摄影师ID查询精选作品
     */
    List<Portfolio> findFeaturedPortfoliosByPhotographerId(Long photographerId, Integer limit);
    
    /**
     * 根据分类查询作品
     */
    List<Portfolio> findPortfoliosByCategory(String category);
    
    /**
     * 根据标签搜索作品
     */
    List<Portfolio> findPortfoliosByTag(String tag);
    
    /**
     * 更新作品
     */
    Portfolio updatePortfolio(Long id, Portfolio portfolio);
    
    /**
     * 删除作品
     */
    boolean deletePortfolio(Long id);
    
    /**
     * 设置作品为精选
     */
    boolean setFeatured(Long id, boolean isFeatured);
    
    /**
     * 增加浏览量
     */
    boolean incrementViewCount(Long id);
    
    /**
     * 增加点赞数
     */
    boolean incrementLikeCount(Long id);
    
    /**
     * 减少点赞数
     */
    boolean decrementLikeCount(Long id);
    
    /**
     * 更新作品状态
     */
    boolean updateStatus(Long id, Integer status);
    
    /**
     * 更新排序权重
     */
    boolean updateSortWeight(Long id, Integer sortWeight);
} 