package com.xhxi.photobooker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xhxi.photobooker.entity.PortfolioComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface PortfolioCommentMapper extends BaseMapper<PortfolioComment> {
    List<PortfolioComment> selectByPortfolioId(@Param("portfolioId") Long portfolioId, @Param("offset") int offset, @Param("size") int size);
    int countByPortfolioId(@Param("portfolioId") Long portfolioId);
} 