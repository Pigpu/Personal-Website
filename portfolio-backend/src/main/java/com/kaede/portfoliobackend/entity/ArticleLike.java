package com.kaede.portfoliobackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class ArticleLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long articleId;

    // 记录是谁点的赞
    private String username;

    // 记录点赞时间
    private LocalDateTime createdAt;
}
