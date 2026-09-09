package com.xhxi.photobooker.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xhxi.photobooker.entity.Admin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminMapper extends BaseMapper<Admin> {
    @Select("SELECT * FROM admin WHERE username = #{username}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "userId", column = "user_id"),
        @Result(property = "username", column = "username"),
        @Result(property = "password", column = "password"),
        @Result(property = "realName", column = "real_name"),
        @Result(property = "phone", column = "phone"),
        @Result(property = "role", column = "role"),
        @Result(property = "status", column = "status")
    })
    Admin getByUsername(String username);

    @Select("select count(*) from admin where  user_id =#{id}")
    int selectByUserId(Long id);
}
