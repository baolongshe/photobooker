package com.xhxi.photobooker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xhxi.photobooker.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

// 继承 JpaRepository，指定实体类和主键类型
@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("select * from user where openid = #{openId}")
    User getByOpenId(String openId);

    @Select("select * from user where username =#{username}")
    User getByUsername(String username);

    boolean updateBatchSelective(@Param("userList") List<User> userList);


    @Update("update user set password = #{password} where id = #{id}")
    void updatePasswordById(User user);

    void updateUser(User user);
}