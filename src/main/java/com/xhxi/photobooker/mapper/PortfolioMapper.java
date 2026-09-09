package com.xhxi.photobooker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xhxi.photobooker.entity.Portfolio;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 摄影师作品Mapper接口
 */
@Mapper
public interface PortfolioMapper extends BaseMapper<Portfolio> {
    
    /**
     * 根据摄影师ID分页查询作品
     */
    @Select("SELECT * FROM portfolio WHERE photographer_id = #{photographerId} AND status = 1 ORDER BY sort_weight DESC, create_time DESC")
    IPage<Portfolio> selectByPhotographerId(Page<Portfolio> page, @Param("photographerId") Long photographerId);
    
    /**
     * 根据摄影师ID查询精选作品
     */
    @Select("SELECT * FROM portfolio WHERE photographer_id = #{photographerId} AND is_featured = 1 AND status = 1 ORDER BY sort_weight DESC, create_time DESC LIMIT #{limit}")
    List<Portfolio> selectFeaturedByPhotographerId(@Param("photographerId") Long photographerId, @Param("limit") Integer limit);
    
    /**
     * 根据分类查询作品
     */
    @Select("SELECT * FROM portfolio WHERE category = #{category} AND status = 1 ORDER BY sort_weight DESC, create_time DESC")
    List<Portfolio> selectByCategory(@Param("category") String category);
    
    /**
     * 增加浏览量
     */
    @Update("UPDATE portfolio SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(@Param("id") Long id);
    
    /**
     * 增加点赞数
     */
    @Update("UPDATE portfolio SET like_count = like_count + 1 WHERE id = #{id}")
    int incrementLikeCount(@Param("id") Long id);
    
    /**
     * 减少点赞数
     */
    @Update("UPDATE portfolio SET like_count = GREATEST(like_count - 1, 0) WHERE id = #{id}")
    int decrementLikeCount(@Param("id") Long id);
    
    /**
     * 根据标签搜索作品
     */
    @Select("SELECT * FROM portfolio WHERE tags LIKE CONCAT('%', #{tag}, '%') AND status = 1 ORDER BY sort_weight DESC, create_time DESC")
    List<Portfolio> selectByTag(@Param("tag") String tag);
} 