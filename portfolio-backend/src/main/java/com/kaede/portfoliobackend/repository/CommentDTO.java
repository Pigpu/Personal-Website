package com.kaede.portfoliobackend.repository;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class CommentDTO {
    private Long id;
    private String content;
    private Long articleId;
    private Long userId;
    private String username;       // 发帖人名字
    private Long parentId;
    private String parentUsername; // 被回复人名字
    private LocalDateTime createdAt;
}
