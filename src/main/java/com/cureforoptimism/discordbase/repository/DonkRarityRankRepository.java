package com.cureforoptimism.discordbase.repository;

import com.cureforoptimism.discordbase.domain.RarityRank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

@Component
public interface DonkRarityRankRepository extends JpaRepository<RarityRank, Long> {
  RarityRank findByDonkId(Long donkId);
}
