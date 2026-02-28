package com.kaede.portfoliobackend.controller;

import com.kaede.portfoliobackend.entity.Comment;
import com.kaede.portfoliobackend.repository.CommentRepository;
import com.kaede.portfoliobackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired // 告诉 Spring：帮我找到这个接口的实现类并塞进来
    private UserRepository userRepository;

    @Autowired
    private CommentRepository commentRepository;

    @PostMapping("/save")
    public ResponseEntity<?> saveComment(@RequestBody Comment comment) {
        // 1. 获取当前登录人的身份 (从 Security 上下文中拿)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body("请先登录再发表评论");
        }

        // 2. 这里的身份信息是我们在 JwtAuthenticationFilter 里塞进去的
        String currentUsername = auth.getName();
        // 根据用户名查出对应的 UserID 并设置给评论...

        return userRepository.findByUsername(currentUsername)
                .map(user -> {
                    // 3. 将查到的 UserID 绑定到评论上
                    comment.setUserId(user.getId());
                    comment.setCreatedAt(LocalDateTime.now()); // 确保时间也是最新的

                    commentRepository.save(comment);
                    return ResponseEntity.ok("评论成功");
                })
                .orElse(ResponseEntity.status(404).body("当前登录用户不存在"));
    }

    @GetMapping("/article/{articleId}")
    public ResponseEntity<List<Map<String, Object>>> getComments(@PathVariable Long articleId) {
        // 这里直接返回 Map 列表，Spring Boot 会自动把它转成标准的 JSON
        List<Map<String, Object>> comments = commentRepository.findByArticleIdWithUsernames(articleId);
        return ResponseEntity.ok(comments);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteComment(@PathVariable Long id) {
        // 1. 获取当前发请求的人的身份
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body("请先登录");
        }

        // 2. 检查这条评论是否存在
        Comment comment = commentRepository.findById(id).orElse(null);
        if (comment == null) {
            return ResponseEntity.status(404).body("评论不存在");
        }

        // 3. 核心鉴权：判断是否有权限删除
        String currentUsername = auth.getName();
        return userRepository.findByUsername(currentUsername)
                .map(user -> {
                    // 判断条件：当前用户是评论作者，或者是管理员
                    boolean isAuthor = comment.getUserId().equals(user.getId());
                    boolean isAdmin = "ROLE_ADMIN".equals(user.getRole());

                    if (!isAuthor && !isAdmin) {
                        return ResponseEntity.status(403).body("越权操作：你没有权限删除此评论！");
                    }

                    // 权限校验通过，执行删除
                    commentRepository.deleteById(id);
                    return ResponseEntity.ok("删除成功");
                })
                .orElse(ResponseEntity.status(404).body("当前登录用户不存在"));
    }
}