package com.kaede.portfoliobackend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. 开启 CORS 并禁用 CSRF
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 显式放行所有 OPTIONS 预检请求（这是解决 CORS 的关键）
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 【公开路径】 - 任何人都能看文章、生涯列表、图片
                        .requestMatchers(HttpMethod.GET, "/api/articles/**").permitAll()
                        // 修正路径匹配：覆盖 /api/career/list 以及未来可能的详情页
                        .requestMatchers(HttpMethod.GET, "/api/career/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()

                        //评论相关权限
                        .requestMatchers(HttpMethod.GET, "/api/comments/article/**").permitAll()//评论查询许可
                        // 需要【登录】即可操作：发表评论
                        // 注意：这里用 authenticated()，不需要 ADMIN 角色，只要是注册用户就行
                        .requestMatchers(HttpMethod.POST, "/api/comments/save").authenticated()
                        // 删除评论必须是管理员 (ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/comments/**").hasRole("ADMIN")

                        // 【生涯管理权限】 - 仅限管理员
                        .requestMatchers(HttpMethod.POST, "/api/career/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/career/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/career/**").hasRole("ADMIN")

                        // 【文章管理权限】 - 仅限管理员
                        .requestMatchers(HttpMethod.POST, "/api/articles/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/articles/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/articles/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/upload").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/articles/*/like").authenticated()
                        .requestMatchers("/api/upload/**").hasRole("ADMIN")

                        // 【作品管理权限】
                        .requestMatchers(HttpMethod.GET, "/api/projects/**").permitAll() // 大家都能看
                        .requestMatchers(HttpMethod.POST, "/api/projects/*/like").authenticated() // 登录后能点赞
                        .requestMatchers(HttpMethod.PUT, "/api/projects/*").authenticated() //只有管理员可以修改
                        .requestMatchers("/api/projects/save", "/api/projects/delete/**").hasRole("ADMIN") // 只有管理员能存删



                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 💡 关键：添加全局跨域配置 Bean
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173")); // 允许前端来源
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
