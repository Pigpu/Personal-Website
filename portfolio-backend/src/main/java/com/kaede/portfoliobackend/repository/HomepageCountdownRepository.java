package com.kaede.portfoliobackend.repository;

import com.kaede.portfoliobackend.entity.HomepageCountdown;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;

public interface HomepageCountdownRepository extends JpaRepository<HomepageCountdown, Long> {
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO homepage_countdown (id, event, target_date)
            VALUES (1, :event, :targetDate)
            ON CONFLICT (id) DO UPDATE SET event = EXCLUDED.event, target_date = EXCLUDED.target_date
            """, nativeQuery = true)
    void saveCurrent(@Param("event") String event, @Param("targetDate") LocalDate targetDate);
}
