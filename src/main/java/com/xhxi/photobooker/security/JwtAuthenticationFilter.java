package com.xhxi.photobooker.security;

import com.xhxi.photobooker.constant.JwtClaimsConstant;
import com.xhxi.photobooker.context.BaseContext;
import com.xhxi.photobooker.entity.Admin;

import com.xhxi.photobooker.entity.User;
import com.xhxi.photobooker.properties.JwtProperties;
import com.xhxi.photobooker.service.AdminService;

import com.xhxi.photobooker.service.UserService;
import com.xhxi.photobooker.service.impl.AdminServiceImpl;
import com.xhxi.photobooker.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
@Component
@RequiredArgsConstructor
@Slf4j
//认证过滤器
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProperties jwtProperties;
    private final UserDetailsServiceImpl userDetailsServiceImpl;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = getTokenFromRequest(request);

            if (StringUtils.hasText(token)) {
                // 解析token获取用户名
                String username = JwtUtil.getUsernameFromToken(token,jwtProperties.getUserSecretKey());

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    setUserAuthentication(token, username);
                }
            }
        } catch (Exception ex) {
            log.error("JWT token validation failed:{}", ex.getMessage());
        }
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            // 【关键】请求结束后清理 ThreadLocal,防止线程复用时数据污染
            BaseContext.removeCurrentId();
            log.debug("已清理BaseContext用户ID");
        }
    }

        private String getTokenFromRequest (HttpServletRequest request){
            String bearerToken = request.getHeader("Authorization");
            if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
                return bearerToken.substring(7);
            }


            String userToken = request.getHeader(jwtProperties.getUserTokenName());
            if (StringUtils.hasText(userToken)) {
                return userToken;
            }

            String adminToken = request.getHeader(jwtProperties.getAdminTokenName());
            if (StringUtils.hasText(adminToken)) {
                return adminToken;
            }

            return null;
        }

       


        private void setUserAuthentication (String token,String username){
            UserDetails user = userDetailsServiceImpl.loadUserByUsername(username);
            Claims claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
            String rolesStr = (String) claims.get("roles");
            System.out.println("[DEBUG]:rolesStr:"+rolesStr);
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            if (rolesStr != null) {
                rolesStr = rolesStr.replace("[", "").replace("]", "").replace(" ", "");
                for (String role : rolesStr.split(",")) {
                    authorities.add(new SimpleGrantedAuthority(role));
                }
            }

            if (JwtUtil.validateToken(token, user,jwtProperties.getUserSecretKey()) && !JwtUtil.isTokenExpired(token,jwtProperties.getUserSecretKey())) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        user, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                // 【关键】从JWT的claims中获取userId并设置到BaseContext
                Long userId = claims.get("userId", Long.class);
                if (userId != null) {
                    BaseContext.setCurrentId(userId);
                    log.info("设置BaseContext用户ID: {}", userId);
                }
            }

        }

        /*private void setAdminAuthentication (String token,String username){

            UserDetails admin = userDetailsServiceImpl.loadUserByUsername(username);

            if (JwtUtil.validateToken(token,admin) ) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        admin, null,admin.getAuthorities()
                );//Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }*/


}
