package com.xhxi.photobooker.service;

import com.xhxi.photobooker.entity.Address;
import com.xhxi.photobooker.dto.PasswordDTO;
import com.xhxi.photobooker.dto.UserLoginDTO;
import com.xhxi.photobooker.entity.User;
import com.xhxi.photobooker.result.Result;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface UserService {
    /*保存修改用户*/
    User saveUser(User user);
    /*根据id查询id*/
    User findUserById(Long id);
    /*根据用户名查询用户*/
    User findUserByUsername(String username);
    /*查询所有用户*/
    List<User> findAllUsers();

    void deleteUser(Long id);
    User selectByUsername(String username);

    User Login(UserLoginDTO userLoginDTO);// 示例：登录逻辑
    /*修改密码*/
    Result<Boolean> updateUsers(List<User> userList);

    boolean changePassword(PasswordDTO passwordDTO);

    Object updateMySelfInfo(Map<String, Object> parameters);

    /*地址相关操作*/
    List<Address> getUserAddresses(Long userId);

    void addAddress(Address address);

    void deleteAddress(Long addressId);

    void setDefaultAddress(Long userId, Long addressId);

    /*同步更新摄影师手机号*/
    void syncPhotographerPhone(Long userId, String phone);
}