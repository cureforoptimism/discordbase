package com.cureforoptimism.discordbase.repository;

import com.cureforoptimism.discordbase.domain.Bird;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BirdRepository extends JpaRepository<Bird, Long> {}
