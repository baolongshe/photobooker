package com.xhxi.photobooker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xhxi.photobooker.entity.Photographer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PhotographerMapper extends BaseMapper<Photographer> {
    @Select("select * from photographer where user_id = #{user_id}")
    Photographer selectByUserId(Long user_id);
}
