package com.kaede.portfoliobackend.controller;

import com.kaede.portfoliobackend.entity.Project;
import com.kaede.portfoliobackend.entity.ProjectLike;
import com.kaede.portfoliobackend.repository.ProjectLikeRepository;
import com.kaede.portfoliobackend.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectLikeRepository projectLikeRepository;

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
    public ResponseEntity<?> toggleLike(@PathVariable Long id, org.springframework.security.core.Authentication authentication) {
        if (authentication == null || authentication.getName().equals("anonymousUser")) {
            return ResponseEntity.status(401).body("请先登录");
        }

        String username = authentication.getName();

        return projectRepository.findById(id).map(project -> {
            Optional<ProjectLike> existingLike = projectLikeRepository.findByProjectIdAndUsername(id, username);

            boolean isLikedNow;
            if (existingLike.isPresent()) {
                // 已点赞 -> 取消点赞
                projectLikeRepository.delete(existingLike.get());
                project.setLikeCount(Math.max(0, project.getLikeCount() - 1));
                isLikedNow = false;
            } else {
                // 未点赞 -> 新增点赞
                ProjectLike newLike = new ProjectLike();
                newLike.setProjectId(id);
                newLike.setUsername(username);
                newLike.setCreatedAt(LocalDateTime.now());
                projectLikeRepository.save(newLike);

                project.setLikeCount(project.getLikeCount() + 1);
                isLikedNow = true;
            }

            projectRepository.save(project);

            return ResponseEntity.ok(java.util.Map.of(
                    "likeCount", project.getLikeCount(),
                    "isLiked", isLikedNow
            ));
        }).orElse(ResponseEntity.notFound().build());
    }

    // 2. 获取当前用户的点赞状态
    @GetMapping("/{id}/like-status")
    public ResponseEntity<Boolean> getLikeStatus(@PathVariable Long id, org.springframework.security.core.Authentication authentication) {
        if (authentication == null || authentication.getName().equals("anonymousUser")) {
            return ResponseEntity.ok(false);
        }
        boolean hasLiked = projectLikeRepository.findByProjectIdAndUsername(id, authentication.getName()).isPresent();
        return ResponseEntity.ok(hasLiked);
    }

    // 3. 获取点赞列表 (管理员专用)
    @GetMapping("/{id}/likes-list")
    public ResponseEntity<List<ProjectLike>> getLikesList(@PathVariable Long id) {
        return ResponseEntity.ok(projectLikeRepository.findByProjectIdOrderByCreatedAtDesc(id));
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