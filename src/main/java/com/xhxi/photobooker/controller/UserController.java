package com.xhxi.photobooker.controller;
import com.xhxi.photobooker.dto.PasswordDTO;
import com.xhxi.photobooker.dto.PhoneCodeDTO;
import com.xhxi.photobooker.dto.UpdatePhoneDTO;
import com.xhxi.photobooker.dto.UserLoginDTO;
import com.xhxi.photobooker.entity.Address;
import com.xhxi.photobooker.entity.User;
import com.xhxi.photobooker.exception.BusinessException;
import com.xhxi.photobooker.properties.JwtProperties;
import com.xhxi.photobooker.result.Result;
import com.xhxi.photobooker.service.UserService;
import com.xhxi.photobooker.utils.JwtUtil;
import com.xhxi.photobooker.vo.UserLoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/photo/user")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private JwtProperties jwtProperties;
    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private AuthenticationManager authenticationManager;
    /*用户登录*/
    @PostMapping("/login")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO){
        try {
            // 验证用户名密码
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            userLoginDTO.getUsername(),
                            userLoginDTO.getPassword()
                    )
            );
            // 2. 获取认证后的UserDetails
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            // 3. 查询数据库用户信息
            User user = userService.findUserByUsername(userDetails.getUsername());

            // 4. 生成JWT
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = JwtUtil.generateToken((UserDetails) authentication.getPrincipal(), user.getId(), jwtProperties.getUserSecretKey(),jwtProperties.getUserTtl());

            //5. 创建返回数据
            UserLoginVO userLoginVO = UserLoginVO.builder()
                    .id(user.getId())
                    .openid(user.getOpenid())
                    .token(jwt)
                    .avatar(user.getAvatar())
                    .role(user.getRole())
                    .username(user.getUsername())
                    .build();

            return Result.success(userLoginVO);
        } catch (BadCredentialsException e) {
            // 账号或密码错误
            throw new BusinessException("账号或密码错误");
        }
    }


    /*注册用户*/
    @PostMapping("/sign")
    public Result<Boolean> sign(@RequestBody User user){
        //判断用户是否存在
        String username = user.getUsername();
        User existUser = userService.findUserByUsername(username);
        //不存在则注册
        if(existUser == null){
            userService.saveUser(user);
            return Result.success(true);
        }else{
            return Result.error("用户名已存在");
        }
    }

    /*批量修改用户*/
    @PostMapping("/update")
    @Transactional
    public Result<Boolean> update(@RequestBody List<User> users){
        return userService.updateUsers(users);
    }

    /*获取用户列表*/
    @GetMapping("/list")
    public Result<List<User>> getUserList(){
        List<User> users = userService.findAllUsers();
        return Result.success(users);
    }
    @PostMapping("/delete")
    @Transactional
    public Result<Boolean> delete(@RequestBody List<User> users)
    {
        // 提取用户ID列表
        List<Long> userIds = users.stream()
                .map(User::getId)
                .collect(Collectors.toList());
        
        // 通过服务层批量删除
        for (Long id : userIds) {
            userService.deleteUser(id);
        }
        return Result.success(true);
    }

    /**
     * 根据id获取用户信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        User user = userService.findUserById(id);
        return user != null ? Result.success(user) : Result.error("用户不存在");
    }


    /**
     * 修改密码
     * @param passwordDTO
     * @return
     */
    @PutMapping("/password")
    public Result<Boolean> changePassword(@RequestBody PasswordDTO passwordDTO) {
        boolean isSuccess = userService.changePassword(passwordDTO);
        if (isSuccess) {
            return Result.success(true);
        }
        return Result.error("密码修改失败，请检查原密码是否正确");
    }

    // 发送手机验证码
    @PostMapping("/sendPhoneCode")
    public Result<String> sendPhoneCode(@RequestBody PhoneCodeDTO dto) {
        String phone = dto.getPhone();
        if (phone == null || phone.length() != 11) {
            return Result.error("手机号格式不正确");
        }

        // 1. 生成6位验证码
        String code = String.valueOf(100000 + new Random().nextInt(900000));

        String redisKey = "phone:code:" + phone;
        // 2. 存入Redis，有效期1分钟
        if(null==stringRedisTemplate.opsForValue().get(redisKey)) {
            stringRedisTemplate.opsForValue().set(redisKey, code, 1, TimeUnit.MINUTES);
        }else {
            return Result.error("验证码已发送，请勿重复发送");
        }
        // 3. 返回验证码（实际开发中应调用短信API）
        return Result.success(code);
    }

    @PostMapping("/updatePhone")
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> updatePhone(@RequestBody UpdatePhoneDTO dto) {
        String phone = dto.getPhone();
        String code = dto.getCode();
        Long userId = dto.getUserId();

        if (phone == null || code == null || userId == null) {
            return Result.error("参数不完整");
        }

        // 1. 校验验证码
        String redisKey = "phone:code:" + phone;
        String realCode = stringRedisTemplate.opsForValue().get(redisKey);
        if (realCode == null) {
            return Result.error("验证码已过期，请重新获取");
        }
        if (!realCode.equals(code)) {
            return Result.error("验证码错误");
        }

        // 2. 更新手机号（通过服务层更新用户并同步摄影师表）
        User user = userService.findUserById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setPhone(phone);
        userService.saveUser(user);

        // 3. 删除验证码
        stringRedisTemplate.delete(redisKey);

        return Result.success(true);
    }

    // 获取用户所有地址
    @GetMapping("/address/{userId}")
    public Result<List<Address>> getUserAddresses(@PathVariable Long userId) {
        return Result.success(userService.getUserAddresses(userId));
    }

    // 添加地址
    @PostMapping("/address")
    public Result<Boolean> addAddress(@RequestBody Address address) {
        userService.addAddress(address);
        return Result.success(true);
    }

    // 删除地址
    @DeleteMapping("/address/{id}")
    public Result<Boolean> deleteAddress(@PathVariable Long id) {
        userService.deleteAddress(id);
        return Result.success(true);
    }

    // 设置默认地址
    @PostMapping("/address/default")
    public Result<Boolean> setDefaultAddress(@RequestParam Long userId, @RequestParam Long addressId) {
        userService.setDefaultAddress(userId, addressId);
        return Result.success(true);
    }
}
