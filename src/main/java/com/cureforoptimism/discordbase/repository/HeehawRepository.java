package com.cureforoptimism.discordbase.repository;

import com.cureforoptimism.discordbase.domain.Heehaw;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HeehawRepository extends JpaRepository<Heehaw, Long> {
  Heehaw findFirstByOrderByIdDesc();

  Optional<Heehaw> findFirstByDiscordUserIdOrderByCreatedAtDesc(Long discordUserId);
}
