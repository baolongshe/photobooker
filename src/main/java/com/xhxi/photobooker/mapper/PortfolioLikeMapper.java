package com.xhxi.photobooker.mapper;



import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xhxi.photobooker.entity.Portfolio;
import com.xhxi.photobooker.entity.PortfolioLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PortfolioLikeMapper extends BaseMapper<PortfolioLike> {
    @Select("SELECT COUNT(*) FROM portfolio_like WHERE user_id = #{userId} AND portfolio_id = #{portfolioId}")
    int countByUserAndPortfolio(Long userId, Long portfolioId);

    @Select("SELECT * FROM portfolio WHERE id IN (SELECT portfolio_id FROM portfolio_like WHERE user_id = #{userId})")
    List<Portfolio> findPortfoliosByUserId(Long userId);
}