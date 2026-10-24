package com.kaede.portfoliobackend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import org.hibernate.annotations.Check;

@Data
@Entity
@Table(name = "homepage_listening")
@Check(name = "homepage_listening_singleton", constraints = "id = 1")
public class HomepageListening {
    @Id
    @Setter(AccessLevel.NONE)
    private Long id = 1L;
    @Column(nullable = false, length = 120)
    private String artist = "";
    @Column(nullable = false, length = 200)
    private String title = "";
    @Column(nullable = false, length = 2000)
    private String url = "";
    @Column(nullable = false)
    private boolean enabled;
}
