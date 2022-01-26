package com.cureforoptimism.discordbase.repository;

import com.cureforoptimism.discordbase.domain.Prayer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrayerRepository extends JpaRepository<Prayer, Long> {
  Optional<Prayer> findFirstByDiscordUserIdOrderByCreatedAtDesc(Long discordUserId);
}
