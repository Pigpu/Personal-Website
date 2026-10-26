package com.kaede.portfoliobackend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "premium_audio")
@Data // 如果没装 Lombok 请手写 Getter/Setter
public class PremiumAudio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String uuid;
    private String title;
    private String audioUrl;
    private LocalDateTime createAt;
}
