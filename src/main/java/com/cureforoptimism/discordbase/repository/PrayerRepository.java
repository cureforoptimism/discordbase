package com.cureforoptimism.discordbase.repository;

import com.cureforoptimism.discordbase.domain.Prayer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PrayerRepository extends JpaRepository<Prayer, Long> {
  Optional<Prayer> findFirstByDiscordUserIdOrderByCreatedAtDesc(Long discordUserId);

  @Query(
      value = "SELECT * FROM prayer p GROUP BY discord_id ORDER BY COUNT(discord_id) DESC LIMIT 20;",
      nativeQuery = true)
  List<Prayer> topPrayers();

  long countByDiscordUserId(Long discordUserId);
}
