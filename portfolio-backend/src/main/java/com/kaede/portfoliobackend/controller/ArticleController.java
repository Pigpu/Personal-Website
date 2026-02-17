package com.kaede.portfoliobackend.controller; // ⚠️ 确认你的包名是否正确

import com.kaede.portfoliobackend.entity.Article;
import com.kaede.portfoliobackend.repository.ArticleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/articles")
@CrossOrigin(origins = "http://localhost:5173") // 允许前端跨域
public class ArticleController {

    @Autowired
    private ArticleRepository articleRepository;

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

    // 6. 点赞文章 (互动力)
    @PostMapping("/{id}/like")
    public ResponseEntity<?> likeArticle(@PathVariable Long id) {
        return articleRepository.findById(id).map(article -> {
            article.setLikeCount(article.getLikeCount() + 1);
            articleRepository.save(article);
            return ResponseEntity.ok(article.getLikeCount()); // 返回最新的点赞数
        }).orElse(ResponseEntity.notFound().build());
    }
}