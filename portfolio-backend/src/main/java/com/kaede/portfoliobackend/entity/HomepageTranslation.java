package com.kaede.portfoliobackend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "homepage_translations")
public class HomepageTranslation {
    @Id
    @Column(length = 2)
    private String language;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String desc1;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String desc2;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String contactPublic;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String contactProf;
}
