package com.kaede.portfoliobackend.controller;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class CaptchaController {

    // 使用 Hutool 的定时缓存，设置过期时间为 5 分钟 (300000 毫秒)
    // 这样不用担心有人狂刷验证码导致服务器内存爆满，过期会自动清理
    public static final TimedCache<String, String> CAPTCHA_CACHE = CacheUtil.newTimedCache(300000);

    static {
        // 启动定时清理任务，每 5 秒检查一次过期的验证码
        CAPTCHA_CACHE.schedulePrune(5000);
    }

    @GetMapping("/captcha")
    public Map<String, String> getCaptcha() {
        // 创建线段干扰的验证码 (宽 120, 高 40, 字符数 4, 干扰线 20)
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 20);

        // 获取生成的验证码文字
        String code = lineCaptcha.getCode();
        // 生成唯一标识 UUID
        String uuid = UUID.randomUUID().toString();

        // 存入缓存
        CAPTCHA_CACHE.put(uuid, code);

        // 返回 UUID 和 Base64 格式的图片给前端
        Map<String, String> result = new HashMap<>();
        result.put("uuid", uuid);
        result.put("img", lineCaptcha.getImageBase64Data()); // "data:image/png;base64,..."

        return result;
    }
}