package com.kaede.portfoliobackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class ProjectLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long projectId;

    // 记录是谁点的赞
    private String username;

    // 记录点赞时间
    private LocalDateTime createdAt;
}