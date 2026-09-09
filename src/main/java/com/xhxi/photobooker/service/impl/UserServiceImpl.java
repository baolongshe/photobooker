package com.xhxi.photobooker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xhxi.photobooker.controller.AdminController;
import com.xhxi.photobooker.context.BaseContext;
import com.xhxi.photobooker.dto.UserLoginDTO;
import com.xhxi.photobooker.dto.PasswordDTO;
import com.xhxi.photobooker.entity.Address;
import com.xhxi.photobooker.entity.Photographer;
import com.xhxi.photobooker.entity.User;
import com.xhxi.photobooker.mapper.AddressMapper;
import com.xhxi.photobooker.mapper.PhotographerMapper;
import com.xhxi.photobooker.mapper.UserMapper;
import com.xhxi.photobooker.properties.WeChatProperties;
import com.xhxi.photobooker.result.Result;
import com.xhxi.photobooker.service.AdminService;
import com.xhxi.photobooker.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class UserServiceImpl implements UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    public static final String wx_LOGIN ="https://api.weixin.qq.com/sns/jscode2session";
    @Autowired
    private WeChatProperties weChatProperties;
    @Autowired
    private  UserMapper userMapper;
    @Autowired
    private AdminService adminService;
    @Autowired
    private PhotographerMapper photographerMapper;
    @Autowired
    private AddressMapper addressMapper;

    @Override
    public User saveUser(User user) {
        // 注册用户时，用MD5加密密码
        String encryptedPwd = DigestUtils.md5DigestAsHex(user.getPassword().getBytes());
        user.setPassword(encryptedPwd);
        userMapper.insert(user);
        return user;
    }

    @Override
    public User findUserById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public User findUserByUsername(String username) {
        return userMapper.getByUsername(username);
    }

    @Override
    public List<User> findAllUsers() {
        return userMapper.selectList(null);
    }


    @Transactional(rollbackFor = Exception.class)
    public boolean updateUser(User user) {
        // 不允许通过此接口更新密码
        user.setPassword(null);
        int result = userMapper.updateById(user);
        
        // 同步更新摄影师表的相同字段（phone、avatar）
        if (result > 0 && user.getId() != null) {
            syncPhotographerFields(user);
        }
        
        return result > 0;
    }

    @Override
    public void deleteUser(Long id) {

    }

    @Override
    public User selectByUsername(String username) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername,username);
        User user = userMapper.selectOne(queryWrapper);
        //没查找到用户就抛出异常
        if(Objects.isNull(user)){
            throw new RuntimeException("未找到该用户");
        }
        return user;
    }


    public void startOrStop(Integer status, Long id){

        User user = User.builder()
                .status(status)
                .id(id)
                .build();
        userMapper.updateById(user);

    }

    @Override
    public User Login(UserLoginDTO userLoginDTO) {
        String username = userLoginDTO.getUsername();
        String password = userLoginDTO.getPassword();
        //判断用户是否存在
        User user = userMapper.getByUsername(username);
        //不存在
        if (user == null ){
            throw new UserLoginException("用户不存在");
        }
        // 用MD5校验密码
        String encryptedPwd = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!user.getPassword().equals(encryptedPwd)) {
            throw new UserLoginException("密码错误");
        }
        return user;
    }
    /*批量修改*/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> updateUsers(List<User> userList) {
        try {
            for (User user : userList) {
                if (user.getId() != null) {
                    // 只有当 password 不为空时才加密和更新
                    if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                        user.setPassword(DigestUtils.md5DigestAsHex(user.getPassword().getBytes()));
                        if(adminService.getById(user.getId())){
                            adminService.updatePassword(user);
                        }else {
                            logger.debug("用户 {} 不是管理员", user.getId());
                        }
                        userMapper.updateUser(user);
                    } else {
                        logger.debug("用户 {} 传递密码为空，不修改密码", user.getId());
                    }
                    userMapper.updateUser(user);
                    logger.debug("用户 {} 更新完成", user.getId());
                    
                    // 同步更新摄影师表的相同字段（phone、avatar）
                    syncPhotographerFields(user);
                }
            }
            return Result.success(true);
        } catch (Exception e) {
            // 记录异常日志，返回失败结果
            return Result.error("系统异常，更新失败：" + e.getMessage());
        }
    }
    
    /**
     * 同步用户表到摄影师表的相同字段
     */
    private void syncPhotographerFields(User user) {
        // 查询该用户是否是摄影师
        QueryWrapper<Photographer> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", user.getId());
        Photographer photographer = photographerMapper.selectOne(wrapper);
        
        if (photographer != null) {
            boolean needUpdate = false;
            
            // 如果手机号变更，同步到摄影师表
            if (user.getPhone() != null && !user.getPhone().equals(photographer.getPhone())) {
                photographer.setPhone(user.getPhone());
                needUpdate = true;
            }
            
            // 如果头像变更，同步到摄影师表
            if (user.getAvatar() != null && !user.getAvatar().equals(photographer.getAvatar())) {
                photographer.setAvatar(user.getAvatar());
                needUpdate = true;
            }
            
            // 如果有字段变更，更新摄影师表
            if (needUpdate) {
                photographerMapper.updateById(photographer);
            }
        }
    }

    /*public String getOpenid(String code){
        //调用微信接口服务，获得当前微信用户的openId
        Map<String, String> map = new HashMap<>();
        map.put("appid",weChatProperties.getAppid());
        map.put("secret",weChatProperties.getSecret());
        map.put("js_code",code);
        map.put("grant_type","authorization_code");
        String json = HttpClientUtil.doGet(wx_LOGIN, map);

        JSONObject jsonObject = JSON.parseObject(json);//将json字符串解析为jsonObject对象
        String openid = jsonObject.getString("openid");

        return openid;
    }*/

    // 自定义业务异常（需创建该类），替代 RuntimeException，便于统一异常处理
    class UserLoginException extends RuntimeException {
        public UserLoginException(String message) {
            super(message);
        }
    }

    @Override
    public boolean changePassword(PasswordDTO passwordDTO) {
        // 1. 校验用户是否存在
        User user = userMapper.selectById(passwordDTO.getUserId());
        if (user == null) {
            // 在实际应用中，可以抛出更明确的异常
            return false;
        }
        // 2. 验证旧密码是否正确
        String encryptedOldPwd = DigestUtils.md5DigestAsHex(passwordDTO.getOldPassword().getBytes());
        if (!user.getPassword().equals(encryptedOldPwd)) {
            // 旧密码不匹配
            return false;
        }
        // 3. 更新为新密码
        String encryptedNewPwd = DigestUtils.md5DigestAsHex(passwordDTO.getNewPassword().getBytes());
        user.setPassword(encryptedNewPwd);
        int result = userMapper.updateById(user);
        return result > 0;
    }

    @Override
    public Object updateMySelfInfo(Map<String, Object> parameters) {
        // 优先使用参数中的userId，如果不存在则从BaseContext获取（兼容非Agent调用）
        Long userId = null;
        if (parameters.containsKey("userId")) {
            userId = Long.parseLong(parameters.get("userId").toString());
        } else {
            userId = BaseContext.getCurrentId();
        }
        
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        
        User existUser = userMapper.selectById(userId);
        if (existUser == null) {
            throw new RuntimeException("用户不存在");
        }
        
        User updateUser = new User();
        updateUser.setId(userId);
        boolean hasUpdate = false;
        
        if (parameters.containsKey("realName")) {
            String realName = (String) parameters.get("realName");
            if (realName != null && !realName.trim().isEmpty()) {
                updateUser.setRealName(realName.trim());
                hasUpdate = true;
            }
        }
        
        if (parameters.containsKey("phone")) {
            String phone = (String) parameters.get("phone");
            if (phone != null && !phone.trim().isEmpty()) {
                if (!phone.matches("^1[3-9]\\d{9}$")) {
                    throw new RuntimeException("手机号格式不正确");
                }
                updateUser.setPhone(phone.trim());
                hasUpdate = true;
            }
        }
        
        if (parameters.containsKey("gender")) {
            Integer gender = Integer.parseInt(parameters.get("gender").toString());
            if (gender >= 0 && gender <= 2) {
                updateUser.setGender(gender);
                hasUpdate = true;
            } else {
                throw new RuntimeException("性别值无效，应为0-女、1-男、2-其他");
            }
        }
        
        if (parameters.containsKey("birthday")) {
            String birthdayStr = (String) parameters.get("birthday");
            if (birthdayStr != null && !birthdayStr.trim().isEmpty()) {
                try {
                    LocalDate localDate = LocalDate.parse(birthdayStr.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    updateUser.setBirthday(localDate);
                    hasUpdate = true;
                } catch (Exception e) {
                    throw new RuntimeException("生日格式不正确，请使用 yyyy-MM-dd 格式");
                }
            }
        }
        
        if (parameters.containsKey("avatar")) {
            String avatar = (String) parameters.get("avatar");
            if (avatar != null && !avatar.trim().isEmpty()) {
                updateUser.setAvatar(avatar.trim());
                hasUpdate = true;
            }
        }
        
        if (!hasUpdate) {
            throw new RuntimeException("没有提供有效的更新字段");
        }
        
        int result = userMapper.updateById(updateUser);
        if (result <= 0) {
            throw new RuntimeException("更新失败");
        }
        
        syncPhotographerFields(updateUser);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "个人信息更新成功");
        response.put("updatedFields", updateUser);
        
        return response;
    }

    @Override
    public List<Address> getUserAddresses(Long userId) {
        return addressMapper.selectList(new QueryWrapper<Address>().eq("user_id", userId));
    }

    @Override
    public void addAddress(Address address) {
        addressMapper.insert(address);
    }

    @Override
    public void deleteAddress(Long addressId) {
        addressMapper.deleteById(addressId);
    }

    @Override
    public void setDefaultAddress(Long userId, Long addressId) {
        // 先将该用户所有地址isDefault设为0
        addressMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Address>()
                .eq("user_id", userId).set("is_default", 0));
        // 再将目标地址isDefault设为1
        Address address = addressMapper.selectById(addressId);
        if (address != null) {
            address.setIsDefault(1);
            addressMapper.updateById(address);
        }
    }

    @Override
    public void syncPhotographerPhone(Long userId, String phone) {
        Photographer photographer = photographerMapper.selectByUserId(userId);
        if (photographer != null) {
            photographer.setPhone(phone);
            photographerMapper.updateById(photographer);
        }
    }
}
