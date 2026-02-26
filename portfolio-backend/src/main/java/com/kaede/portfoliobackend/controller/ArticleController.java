package com.kaede.portfoliobackend.controller; // ⚠️ 确认你的包名是否正确

import com.kaede.portfoliobackend.entity.Article;
import com.kaede.portfoliobackend.entity.ArticleLike;
import com.kaede.portfoliobackend.repository.ArticleLikeRepository;
import com.kaede.portfoliobackend.repository.ArticleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/articles")// 允许前端跨域
public class ArticleController {

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private ArticleLikeRepository articleLikeRepository;

    // 1. 获取所有文章 (按时间倒序)
    @GetMapping
    public List<Article> getAllArticles() {
        return articleRepository.findAllByOrderByCreatedAtDesc();
    }

    // 2. 获取单篇文章详情 (自动增加阅读量 +1)
    @GetMapping("/{id}")
    public ResponseEntity<Article> getArticle(@PathVariable Long id) {
        return articleRepository.findById(id).map(article -> {
            // 浏览量自增
            article.setViewCount(article.getViewCount() + 1);
            articleRepository.save(article);
            return ResponseEntity.ok(article);
        }).orElse(ResponseEntity.notFound().build());
    }

    // 3. 发布新文章 (管理员)
    @PostMapping
    public Article createArticle(@RequestBody Article article) {
        article.setCreatedAt(LocalDateTime.now());
        // 确保初始值为0，防止空指针
        if (article.getViewCount() == null) article.setViewCount(0);
        if (article.getLikeCount() == null) article.setLikeCount(0);
        return articleRepository.save(article);
    }

    // 4. 更新文章 (管理员) - 对应前端的编辑功能
    @PutMapping("/{id}")
    public ResponseEntity<?> updateArticle(@PathVariable Long id, @RequestBody Article articleDetails) {
        return articleRepository.findById(id).map(article -> {
            article.setTitle(articleDetails.getTitle());
            article.setContent(articleDetails.getContent());
            article.setSummary(articleDetails.getSummary());
            article.setCategory(articleDetails.getCategory());
            article.setCoverUrl(articleDetails.getCoverUrl());
            // 注意：不更新 viewCount 和 likeCount，保留原数据
            articleRepository.save(article);
            return ResponseEntity.ok("文章更新成功");
        }).orElse(ResponseEntity.notFound().build());
    }

    // 5. 删除文章 (管理员)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteArticle(@PathVariable Long id) {
        return articleRepository.findById(id).map(article -> {
            articleRepository.delete(article);
            return ResponseEntity.ok("文章已删除");
        }).orElse(ResponseEntity.notFound().build());
    }

    /// 1. 点赞/取消点赞 (Toggle逻辑)
    @PostMapping("/{id}/like")
    public ResponseEntity<?> toggleLike(@PathVariable Long id, org.springframework.security.core.Authentication authentication) {
        if (authentication == null || authentication.getName().equals("anonymousUser")) {
            return ResponseEntity.status(401).body("请先登录");
        }

        String username = authentication.getName(); // 获取当前登录用户名

        return articleRepository.findById(id).map(article -> {
            // 检查是否已经点过赞
            Optional<ArticleLike> existingLike = articleLikeRepository.findByArticleIdAndUsername(id, username);

            boolean isLikedNow;
            if (existingLike.isPresent()) {
                // 如果点过赞 -> 取消点赞
                articleLikeRepository.delete(existingLike.get());
                article.setLikeCount(Math.max(0, article.getLikeCount() - 1));
                isLikedNow = false;
            } else {
                // 如果没点过 -> 新增点赞
                ArticleLike newLike = new ArticleLike();
                newLike.setArticleId(id);
                newLike.setUsername(username);
                newLike.setCreatedAt(LocalDateTime.now());
                articleLikeRepository.save(newLike);

                article.setLikeCount(article.getLikeCount() + 1);
                isLikedNow = true;
            }

            articleRepository.save(article);

            // 返回最新状态给前端
            return ResponseEntity.ok(java.util.Map.of(
                    "likeCount", article.getLikeCount(),
                    "isLiked", isLikedNow
            ));
        }).orElse(ResponseEntity.notFound().build());
    }

    // 2. 获取当前用户的点赞状态 (前端初始化用)
    @GetMapping("/{id}/like-status")
    public ResponseEntity<Boolean> getLikeStatus(@PathVariable Long id, org.springframework.security.core.Authentication authentication) {
        if (authentication == null || authentication.getName().equals("anonymousUser")) {
            return ResponseEntity.ok(false);
        }
        boolean hasLiked = articleLikeRepository.findByArticleIdAndUsername(id, authentication.getName()).isPresent();
        return ResponseEntity.ok(hasLiked);
    }

    // 3. 获取点赞列表 (管理员专用)
    @GetMapping("/{id}/likes-list")
    public ResponseEntity<List<ArticleLike>> getLikesList(@PathVariable Long id) {
        return ResponseEntity.ok(articleLikeRepository.findByArticleIdOrderByCreatedAtDesc(id));
    }
}