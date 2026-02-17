package com.kaede.portfoliobackend.controller;

import com.kaede.portfoliobackend.entity.Project;
import com.kaede.portfoliobackend.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @Autowired
    private ProjectRepository projectRepository;

    // 获取所有作品（带简单的排序参数）
    @GetMapping
    public List<Project> getProjects(@RequestParam(required = false) String sort) {
        if ("likes".equals(sort)) {
            return projectRepository.findAllByOrderByLikeCountDesc();
        }
        return projectRepository.findAllByOrderByCreatedAtDesc();
    }

    // 搜索作品
    @GetMapping("/search")
    public List<Project> searchProjects(@RequestParam String keyword) {
        return projectRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword, keyword);
    }

    // 获取单个作品详情（同时增加播放数）
    @GetMapping("/{id}")
    public ResponseEntity<Project> getProject(@PathVariable Long id) {
        return projectRepository.findById(id).map(p -> {
            p.setViewCount(p.getViewCount() + 1); // 播放数+1
            projectRepository.save(p);
            return ResponseEntity.ok(p);
        }).orElse(ResponseEntity.notFound().build());
    }

    // 点赞作品
    @PostMapping("/{id}/like")
    public ResponseEntity<?> likeProject(@PathVariable Long id) {
        return projectRepository.findById(id).map(p -> {
            p.setLikeCount(p.getLikeCount() + 1);
            projectRepository.save(p);
            return ResponseEntity.ok(p.getLikeCount());
        }).orElse(ResponseEntity.notFound().build());
    }

    // 管理员：保存作品
    @PostMapping("/save")
    public Project saveProject(@RequestBody Project project) {
        // 权限校验已在 SecurityConfig 中通过 .hasRole("ADMIN") 处理
        return projectRepository.save(project);
    }

    // 管理员：删除作品
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(@PathVariable Long id) {
        projectRepository.deleteById(id);
        return ResponseEntity.ok("作品已删除");
    }

    // 更新作品 (管理员权限)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProject(@PathVariable Long id, @RequestBody Project project) {
        return projectRepository.findById(id).map(existing -> {
            existing.setTitle(project.getTitle());
            existing.setDescription(project.getDescription());
            existing.setCategory(project.getCategory());
            // 如果前端传了新的 URL 才更新，防止覆盖为空
            if (project.getCoverUrl() != null && !project.getCoverUrl().isEmpty()) {
                existing.setCoverUrl(project.getCoverUrl());
            }
            if (project.getMediaUrl() != null && !project.getMediaUrl().isEmpty()) {
                existing.setMediaUrl(project.getMediaUrl());
                existing.setMediaType(project.getMediaType());
            }
            if (project.getAttachmentUrl() != null) {
                existing.setAttachmentUrl(project.getAttachmentUrl());
            }

            projectRepository.save(existing);
            return ResponseEntity.ok("作品更新成功");
        }).orElse(ResponseEntity.notFound().build());
    }
}