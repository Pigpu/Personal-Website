package com.kaede.portfoliobackend.repository;

import com.kaede.portfoliobackend.entity.FriendLink;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FriendLinkRepository extends JpaRepository<FriendLink, Long> {
    List<FriendLink> findAllByOrderByIdAsc();
}
