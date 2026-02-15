package com.kaede.portfoliobackend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "comments")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private Long articleId;

    private Long userId;

    private Long parentId; // 父评论 ID

    private LocalDateTime createdAt = LocalDateTime.now();

    // 💡 进阶：我们可以添加两个虚拟字段，方便前端显示
    @Transient // 不映射到数据库，仅用于接口返回
    private String username;

    @Transient
    private String parentUsername; // 被回复人的名字
}
