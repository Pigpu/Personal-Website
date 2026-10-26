package com.kaede.portfoliobackend.repository;

import com.kaede.portfoliobackend.entity.PremiumAudio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PremiumAudioRepository extends JpaRepository<PremiumAudio, Long> {

    // 自定义查询方法：根据 UUID 查找对应的私密音频记录
    Optional<PremiumAudio> findByUuid(String uuid);

}