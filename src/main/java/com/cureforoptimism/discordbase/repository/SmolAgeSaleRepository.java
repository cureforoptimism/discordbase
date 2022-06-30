package com.cureforoptimism.discordbase.repository;

import com.cureforoptimism.discordbase.domain.SmolAgeSale;
import java.util.Date;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SmolAgeSaleRepository extends JpaRepository<SmolAgeSale, Long> {
  SmolAgeSale findFirstByPostedIsTrueOrderByBlockTimestampDesc();

  boolean existsByTxAndTokenId(String tx, Integer tokenId);

  List<SmolAgeSale> findByBlockTimestampIsAfterAndPostedIsFalseOrderByBlockTimestampAsc(
      Date blockTimestamp);
}
