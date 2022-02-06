package com.cureforoptimism.discordbase.repository;

import com.cureforoptimism.discordbase.domain.Oooo;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OoooRepository extends JpaRepository<Oooo, Long> {
  Oooo findFirstByOrderByIdDesc();

  Optional<Oooo> findFirstByDiscordUserIdOrderByCreatedAtDesc(Long discordUserId);
}
