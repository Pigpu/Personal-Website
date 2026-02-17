package com.kaede.portfoliobackend.config;

import com.kaede.portfoliobackend.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. 从请求头中获取 Authorization
        String authHeader = request.getHeader("Authorization");

        // 2. 检查是否有 Bearer 令牌
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                // 3. 解析 Token 提取用户名和角色
                String username = jwtUtils.extractUsername(token);
                String role = jwtUtils.extractRole(token);

                // 在 JwtAuthenticationFilter 中
                if (role != null) {
                    // 确保这里的 role 是 "ROLE_ADMIN"，而不是 "ADMIN"
                    // 如果数据库存的是 "ADMIN"，这里要写成: "ROLE_" + role
                    String authorityRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority(authorityRole);

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            username, null, Collections.singletonList(authority));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }

                // 在 JwtAuthenticationFilter.java 的 doFilterInternal 中
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // 打印一下，看看解析出来的用户名和角色对不对
                    System.out.println("JWT 解析成功 - 用户: " + username + ", 角色: " + role);

                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role);
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            username, null, Collections.singletonList(authority));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // 4. 将角色封装成 Spring Security 认得的 Authority (注意：必须加 ROLE_ 前缀)
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role);

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            username, null, Collections.singletonList(authority));

                    // 5. 正式存入安全上下文，告诉后面所有的过滤器：“这个人通过了，他是管理员/用户”
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception e) {
                // Token 无效或过期，这里可以选择记录日志，或者直接放行（让后面的鉴权挡住）
                logger.error("JWT 验证失败: " + e.getMessage());
            }
        }


        // 6. 继续执行后面的过滤器链
        filterChain.doFilter(request, response);
    }
}
