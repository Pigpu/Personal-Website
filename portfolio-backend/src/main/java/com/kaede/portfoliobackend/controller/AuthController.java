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
    public static class RegisterRequest {
        private String username;
        private String password;
        private String captcha; // 用户输入的验证码
        private String uuid;    // 前端获取验证码时绑定的 UUID

        // Getters and Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getCaptcha() { return captcha; }
        public void setCaptcha(String captcha) { this.captcha = captcha; }

        public String getUuid() { return uuid; }
        public void setUuid(String uuid) { this.uuid = uuid; }
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils; // 注入刚才写的工具类

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {

        // --- 步骤 1：验证码校验 ---
        // 确保 UUID 和 Captcha 不为空
        if (request.getUuid() == null || request.getCaptcha() == null) {
            return ResponseEntity.badRequest().body("验证码参数缺失");
        }

        // 从缓存中取出真正的验证码
        String cachedCode = CaptchaController.CAPTCHA_CACHE.get(request.getUuid());

        if (cachedCode == null) {
            return ResponseEntity.badRequest().body("验证码已过期，请点击图片刷新");
        }

        // 校验输入是否正确（忽略大小写）
        if (!cachedCode.equalsIgnoreCase(request.getCaptcha())) {
            return ResponseEntity.badRequest().body("验证码错误");
        }

        // 校验通过后，立即从缓存中删除该验证码，防止被黑客重复利用 (重放攻击)
        CaptchaController.CAPTCHA_CACHE.remove(request.getUuid());


        // --- 步骤 2：执行原有的注册逻辑 ---
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("用户名已存在");
        }

        // 因为参数变成了 request，我们需要手动 new 一个 User 实体来保存到数据库
        User user = new User();
        user.setUsername(request.getUsername());
        // 密码加密
        user.setPassword(passwordEncoder.encode(request.getPassword()));
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