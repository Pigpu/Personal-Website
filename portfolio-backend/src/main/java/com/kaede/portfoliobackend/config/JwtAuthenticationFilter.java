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

                // 4. 只有当用户名存在且当前上下文未认证时，才进行认证设置
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                    // 核心修复：确保 role 带有 "ROLE_" 前缀，完美适配 Spring Security 的 hasRole 语法
                    String authorityRole = (role != null && role.startsWith("ROLE_")) ? role : "ROLE_" + role;
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority(authorityRole);

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            username, null, Collections.singletonList(authority));

                    // 5. 正式存入安全上下文
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    // 打印日志，方便线上排查
                    System.out.println("JWT 解析成功 - 用户: " + username + ", 授权角色: " + authorityRole);
                }
            } catch (Exception e) {
                // Token 无效或过期，记录错误并放行，由后续的 Security 配置拦截
                logger.error("JWT 验证失败: " + e.getMessage());
            }
        }

        // 6. 无论是否有 Token，最后都必须继续执行过滤器链
        filterChain.doFilter(request, response);
    }
}