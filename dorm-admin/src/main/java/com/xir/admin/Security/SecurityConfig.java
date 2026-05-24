package com.xir.admin.Security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;  // ✅ 注入你的 JWT 过滤器

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()  // ✅ 登录、注册、验证码放行
                        .requestMatchers("/dashboard/**").permitAll()  // 先放行测试
                        .requestMatchers("/building/**").permitAll()
                        .requestMatchers("/room/**").permitAll()
                        .requestMatchers("/student/**").permitAll()
                        .requestMatchers("/repair/**").permitAll()
                        .requestMatchers("/notice/**").permitAll()
                        .requestMatchers("/invite/**").permitAll()
                        .requestMatchers("/room/**").permitAll()
                        .requestMatchers("/dorm-change/**").permitAll()
                        .requestMatchers("/student/**").permitAll()
                        .requestMatchers("/student/template").permitAll()
                        .requestMatchers("/checkin/**").permitAll()
                        .requestMatchers("/room/detail/**").permitAll()
                        .requestMatchers("/checkout/**").permitAll()
                        .requestMatchers("/notice/**").permitAll()
                        .requestMatchers("/settings/**").permitAll()
                        .requestMatchers("/change/**").permitAll()
                        .requestMatchers("/repair/**").permitAll()
                        .requestMatchers("/worker/**").permitAll()
                        .requestMatchers("/invite/**").permitAll()
                        .requestMatchers("/admin/**").permitAll()
                        .requestMatchers("/student-api/**").permitAll()
                        .requestMatchers("/leave/**").permitAll()
                                .requestMatchers("/change/**").permitAll()
                                .requestMatchers("/checkout/**").permitAll()
                                .requestMatchers("/student-api/**").permitAll()
                                .requestMatchers("/dorm-change/agree-swap/**").permitAll()
                                .requestMatchers("/dorm-change/reject-swap/**").permitAll()
                                .requestMatchers("/upload/**").permitAll()


                        .anyRequest().authenticated()
//                                .anyRequest().permitAll()//放行全部
                )
                // ✅ 关键：把 JWT 过滤器加到 Spring Security 过滤器链里
             .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}