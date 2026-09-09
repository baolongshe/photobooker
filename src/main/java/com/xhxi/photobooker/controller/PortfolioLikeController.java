package com.xhxi.photobooker.controller;

import com.xhxi.photobooker.entity.Portfolio;
import com.xhxi.photobooker.result.Result;
import com.xhxi.photobooker.service.PortfolioLikeService;
import com.xhxi.photobooker.service.impl.PortfolioLikeServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
@RestController
@RequestMapping("/photo/portfolio-like")
public class PortfolioLikeController {

    @Autowired
    private PortfolioLikeService portfolioLikeService;
    /*根据用户id查询收藏作品信息*/
    @GetMapping("/user/{userId}")
    public Result<List<Portfolio>> getUserPortfolios(@PathVariable Long userId) {
        try {
            List<Portfolio> portfolios = portfolioLikeService.findPortfoliosByUserId(userId);
            return Result.success(portfolios);
        } catch (Exception e) {
            return Result.error("查询用户收藏作品失败: " + e.getMessage());
        }
    }
}
