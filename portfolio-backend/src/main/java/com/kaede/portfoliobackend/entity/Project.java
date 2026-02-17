package com.kaede.portfoliobackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "projects")
@Data
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description; // 作品简介

    private String category;    // 分类：如“剪辑”、“原创音乐”等
    private String coverUrl;    // 封面图路径
    private String mediaUrl;    // 视频或音频文件路径
    private String mediaType;   // 类型标志：VIDEO 或 AUDIO
    private String attachmentUrl; // 附件（zip/pdf）路径

    private Integer viewCount = 0; // 播放/阅读量
    private Integer likeCount = 0; // 点赞数

    private LocalDateTime createdAt = LocalDateTime.now();
}