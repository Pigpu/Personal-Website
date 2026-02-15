package com.kaede.portfoliobackend.controller;

import com.kaede.portfoliobackend.entity.User;
import com.kaede.portfoliobackend.repository.UserRepository;
import com.kaede.portfoliobackend.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils; // 注入刚才写的工具类

    // 注册接口
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("用户名已存在");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("ROLE_USER"); // 新用户默认都是 ROLE_USER
        userRepository.save(user);
        return ResponseEntity.ok("注册成功");
    }

    // 登录接口
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        // 1. 查找用户
        Optional<User> userOptional = userRepository.findByUsername(username);

        // 2. 检查用户是否存在
        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // 3. 验证密码
            if (passwordEncoder.matches(password, user.getPassword())) {
                // 成功：签发 Token 并返回数据
                String token = jwtUtils.generateToken(user.getUsername(), user.getRole());

                Map<String, Object> response = new HashMap<>();
                response.put("token", token);
                response.put("username", user.getUsername());
                response.put("role", user.getRole());

                return ResponseEntity.ok(response);
            }
        }

        // 4. 统一处理所有失败情况（用户名不存在或密码错误）
        return ResponseEntity.status(401).body("用户名或密码错误");
    }
}