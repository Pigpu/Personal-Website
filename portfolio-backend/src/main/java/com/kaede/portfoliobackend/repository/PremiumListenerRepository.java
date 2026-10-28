package com.kaede.portfoliobackend.repository;

import com.kaede.portfoliobackend.entity.PremiumListener;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PremiumListenerRepository extends JpaRepository<PremiumListener, Long> {
    List<PremiumListener> findAllByOrderByGrantedAtDescUserIdAsc();
}
