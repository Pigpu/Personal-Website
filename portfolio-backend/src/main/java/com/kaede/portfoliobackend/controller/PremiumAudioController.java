package com.kaede.portfoliobackend.controller;

import com.kaede.portfoliobackend.entity.PremiumAudio;
import com.kaede.portfoliobackend.repository.PremiumAudioRepository;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/premium")
public class PremiumAudioController {

    private final PremiumAudioRepository repository;

    public PremiumAudioController(PremiumAudioRepository repository) {
        this.repository = repository;
    }

    // 1. 管理员：获取所有私密音频列表
    @GetMapping("/admin/list")
    public ResponseEntity<List<PremiumAudio>> getAdminList() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(repository.findAll());
    }

    // 2. 管理员：保存音频记录 (上传文件复用之前的 FileController)
    @PostMapping("/admin/save")
    public ResponseEntity<?> savePremiumAudio(@RequestBody PremiumAudio audio) {
        audio.setUuid(UUID.randomUUID().toString()); // 自动生成唯一 UUID
        audio.setCreateAt(LocalDateTime.now());
        repository.save(audio);
        return ResponseEntity.ok("发布成功");
    }

    // 3. 粉丝/游客：根据 UUID 获取音频信息 (不暴露数据库 ID)
    @GetMapping("/play/{uuid}")
    public ResponseEntity<PremiumAudio> getAudioByUuid(@PathVariable String uuid) {
        return repository.findByUuid(uuid)
                .map(audio -> ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(audio))
                .orElse(ResponseEntity.notFound().build());
    }
}
