package com.xhxi.photobooker.service.impl;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.region.Region;
import com.xhxi.photobooker.controller.FileController;
import com.xhxi.photobooker.entity.Portfolio;
import com.xhxi.photobooker.mapper.PortfolioMapper;
import com.xhxi.photobooker.service.PortfolioService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 摄影师作品服务实现类
 */
@Service
public class PortfolioServiceImpl extends ServiceImpl<PortfolioMapper, Portfolio> implements PortfolioService {
    
    @Autowired
    private PortfolioMapper portfolioMapper;


    @Override
    @Transactional
    public Portfolio savePortfolio(Portfolio portfolio) {

        Date now = new Date();
        portfolio.setCreateTime(now);
        portfolio.setUpdateTime(now);

        // 设置默认值
        if (portfolio.getStatus() == null) {
            portfolio.setStatus(1); // 默认展示
        }
        if (portfolio.getViewCount() == null) {
            portfolio.setViewCount(0);
        }
        if (portfolio.getLikeCount() == null) {
            portfolio.setLikeCount(0);
        }
        if (portfolio.getIsFeatured() == null) {
            portfolio.setIsFeatured(0);
        }
        if (portfolio.getSortWeight() == null) {
            portfolio.setSortWeight(0);
        }
        
        portfolioMapper.insert(portfolio);
        return portfolio;
    }





    @Override
    public Portfolio findPortfolioById(Long id) {
        return portfolioMapper.selectById(id);
    }
    
    @Override
    public IPage<Portfolio> findPortfoliosByPhotographerId(Long photographerId, Integer page, Integer size) {
        Page<Portfolio> pageParam = new Page<>(page, size);
        return portfolioMapper.selectByPhotographerId(pageParam, photographerId);
    }
    
    @Override
    public List<Portfolio> findFeaturedPortfoliosByPhotographerId(Long photographerId, Integer limit) {
        return portfolioMapper.selectFeaturedByPhotographerId(photographerId, limit);
    }
    
    @Override
    public List<Portfolio> findPortfoliosByCategory(String category) {
        return portfolioMapper.selectByCategory(category);
    }
    
    @Override
    public List<Portfolio> findPortfoliosByTag(String tag) {
        return portfolioMapper.selectByTag(tag);
    }
    
    @Override
    @Transactional
    public Portfolio updatePortfolio(Long id, Portfolio portfolio) {
        Portfolio existingPortfolio = portfolioMapper.selectById(id);
        if (existingPortfolio == null) {
            throw new RuntimeException("作品不存在");
        }
        
        portfolio.setId(id);
        portfolio.setUpdateTime(new Date());
        portfolioMapper.updateById(portfolio);
        return portfolio;
    }
    
    @Override
    @Transactional
    public boolean deletePortfolio(Long id) {
        return portfolioMapper.deleteById(id) > 0;
    }
    
    @Override
    @Transactional
    public boolean setFeatured(Long id, boolean isFeatured) {
        Portfolio portfolio = new Portfolio();
        portfolio.setId(id);
        portfolio.setIsFeatured(isFeatured ? 1 : 0);
        portfolio.setUpdateTime(new Date());
        return portfolioMapper.updateById(portfolio) > 0;
    }
    
    @Override
    @Transactional
    public boolean incrementViewCount(Long id) {
        return portfolioMapper.incrementViewCount(id) > 0;
    }
    
    @Override
    @Transactional
    public boolean incrementLikeCount(Long id) {
        return portfolioMapper.incrementLikeCount(id) > 0;
    }
    
    @Override
    @Transactional
    public boolean decrementLikeCount(Long id) {
        return portfolioMapper.decrementLikeCount(id) > 0;
    }
    
    @Override
    @Transactional
    public boolean updateStatus(Long id, Integer status) {
        Portfolio portfolio = new Portfolio();
        portfolio.setId(id);
        portfolio.setStatus(status);
        portfolio.setUpdateTime(new Date());
        return portfolioMapper.updateById(portfolio) > 0;
    }
    
    @Override
    @Transactional
    public boolean updateSortWeight(Long id, Integer sortWeight) {
        Portfolio portfolio = new Portfolio();
        portfolio.setId(id);
        portfolio.setSortWeight(sortWeight);
        portfolio.setUpdateTime(new Date());
        return portfolioMapper.updateById(portfolio) > 0;
    }
} 