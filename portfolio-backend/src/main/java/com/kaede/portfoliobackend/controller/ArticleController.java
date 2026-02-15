package com.kaede.portfoliobackend.controller;

import com.kaede.portfoliobackend.entity.Article;
import com.kaede.portfoliobackend.repository.ArticleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    @Autowired
    private ArticleRepository articleRepository;

    @GetMapping
    public List<Article> getAll() {
        return articleRepository.findAllByOrderByCreatedAtDesc();
    }

    // 1. 保存文章
    @PostMapping
    public Article save(@RequestBody Article article) {
        return articleRepository.save(article);
    }

    // 2. 获取详情：注意这里是 @PathVariable，去掉了那个错误的 @Long
    @GetMapping("/{id}")
    public Article getById(@PathVariable Long id) {
        return articleRepository.findById(id).orElse(null);
    }

    // 3. 点赞接口
    @PostMapping("/{id}/like")
    public void like(@PathVariable Long id) {
        articleRepository.incrementLikeCount(id);
    }

    @DeleteMapping("/{id}")
    public void deleteArticle(@PathVariable Long id) {
        Article article = articleRepository.findById(id).orElse(null);
        if (article == null) return;

        // 1. 获取文章内容，搜索图片
        String content = article.getContent();
        // 正则表达式匹配：http://localhost:8080/uploads/文件名.jpg
        Pattern pattern = Pattern.compile("http://localhost:8080/uploads/([^\\s\\)]+)");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String fileName = matcher.group(1); // 提取文件名
            try {
                // 拼接本地路径并删除
                Path filePath = Paths.get("D:/HomePageProject/HomepageDB/uploads/" + fileName);
                Files.deleteIfExists(filePath);
                System.out.println("成功删除物理文件: " + fileName);
            } catch (Exception e) {
                System.err.println("物理文件删除失败: " + e.getMessage());
            }
        }

        // 2. 删除数据库记录
        articleRepository.deleteById(id);
    }
}

