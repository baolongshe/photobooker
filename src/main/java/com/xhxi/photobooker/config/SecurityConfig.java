package com.xhxi.photobooker.config;

import com.xhxi.photobooker.properties.JwtProperties;
import com.xhxi.photobooker.security.JwtAuthenticationEntryPoint;
import com.xhxi.photobooker.security.JwtAuthenticationFilter;
import com.xhxi.photobooker.security.Md5PasswordEncoder;
import com.xhxi.photobooker.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import static org.springframework.security.config.Customizer.withDefaults;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)//方法级别的security启动注解
public class SecurityConfig {
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    public SecurityConfig(JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
        super();
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)// 禁用CSRF（REST API通常不需要）
                .cors(withDefaults())//启用默认的cors跨域支持
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))//禁用 HTTP Session，强制应用通过 JWT 等机制管理用户状态。
                .authorizeHttpRequests(authz -> authz
                        // 公开接口
                        .requestMatchers(
                                "announcement/latest",
                                "/photo/user/login",
                                "/photo/photographer/**",
                                "/user/register",
                                "/photo/photographer/map",
                                "/photographer/login",
                                "/photographer/register",
                                "/file/**",
                                "/ai/stream-chat",
                                "/ai/task/status",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/webjars/**",
                                "/favicon.ico",
                                "/photo/user/sign",
                                "/photographers",
                                "/photographer/*",
                                "/portfolio/*",
                                "/chat/history/**",
                                "/ws",
                                "/technical-notes",
                                "/photographer-map",
                                "/chat",
                                "/testws",
                                "/test-ws",
                                "/photographer/packages/**",
                                "/pay/**",  // 支付相关接口
                                "/alipay/**",  // 支付宝接口
                                "/payment/**" // 支付接口
                        ).permitAll()
                        // RAG chat requires authentication
                        .requestMatchers("/ai/rag-chat").authenticated()
                        // RAG debug endpoint requires admin role
                        .requestMatchers("/ai/rag-chat-debug").hasRole("ADMIN")
                        // 管理员接口
                        .requestMatchers("/photo/admin/**").hasRole("ADMIN")

                        // 其他接口需要认证
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))

                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);//在认证过滤器之前

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(UserDetailsServiceImpl userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder); // 这里注入的是我们的Md5PasswordEncoder
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http, DaoAuthenticationProvider daoAuthenticationProvider) throws Exception {
        AuthenticationManagerBuilder builder = http.getSharedObject(AuthenticationManagerBuilder.class);
        builder.authenticationProvider(daoAuthenticationProvider);
        return builder.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        logger.info("使用自定义Md5PasswordEncoder");
        return new Md5PasswordEncoder(); //返回混合密码编译器
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtProperties jwtProperties, UserDetailsServiceImpl userDetailsService) {
        return new JwtAuthenticationFilter(jwtProperties, userDetailsService);
    }
}