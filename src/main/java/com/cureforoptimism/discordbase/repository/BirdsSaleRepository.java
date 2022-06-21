package com.cureforoptimism.discordbase.repository;

import com.cureforoptimism.discordbase.domain.BirdsSale;
import java.util.Date;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BirdsSaleRepository extends JpaRepository<BirdsSale, Long> {
  BirdsSale findFirstByPostedIsTrueOrderByBlockTimestampDesc();

  boolean existsByTxAndTokenId(String tx, Integer tokenId);

  List<BirdsSale> findByBlockTimestampIsAfterAndPostedIsFalseOrderByBlockTimestampAsc(
      Date blockTimestamp);
}
