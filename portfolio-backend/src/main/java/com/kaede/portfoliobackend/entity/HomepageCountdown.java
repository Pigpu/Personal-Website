package com.kaede.portfoliobackend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import org.hibernate.annotations.Check;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "homepage_countdown")
@Check(name = "homepage_countdown_singleton", constraints = "id = 1")
public class HomepageCountdown {
    @Id
    @Setter(AccessLevel.NONE)
    private Long id = 1L;
    @Column(nullable = false, length = 120)
    private String event;
    @Column(nullable = false)
    private LocalDate targetDate;
}
