package com.xhxi.photobooker.security;

import com.xhxi.photobooker.entity.Admin;
import com.xhxi.photobooker.entity.User;
import com.xhxi.photobooker.mapper.UserMapper;
import com.xhxi.photobooker.service.AdminService;
import com.xhxi.photobooker.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Autowired
    private UserService userService;
    @Autowired
    private AdminService adminService;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        //查询用户信息
        User user = userService.selectByUsername(username);
        //查询admin信息
        Admin admin = adminService.getByUsername(username);
        //比对并生成对应的userDetails
        if(admin != null ) {
            return org.springframework.security.core.userdetails.User.builder()
                    .username(admin.getUsername())
                    .password(admin.getPassword())
                    .roles("ADMIN")//自动添加ROLE_前缀
                    .build();
        } else if (username.equals(user.getUsername())) {
            return org.springframework.security.core.userdetails.User.builder()
                    .username(user.getUsername())
                    .password(user.getPassword())
                    .roles("USER")
                    .build();

        }else {
            throw new UsernameNotFoundException("用户不存在");
        }

    }
}
