package com.kaede.portfoliobackend.model; // 确保包名与你的项目一致

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Career {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String startDate; // 存储格式如 "2026-02-11"
    private String endDate;
    private Boolean isCurrent;

    // period 可以保留，也可以通过 startDate 和 endDate 动态生成
    private String period;

    private String company;
    private String position;
    @Column(columnDefinition = "TEXT")
    private String description;

    // 如果技术标签也可能很长，也可以改为 TEXT
    @Column(columnDefinition = "TEXT")
    private String tags;
}