package com.cureforoptimism.discordbase.repository;

import com.cureforoptimism.discordbase.domain.DonkList;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DonkListRepository extends JpaRepository<DonkList, Long> {
  DonkList findFirstByPostedIsTrueOrderByBlockTimestampDesc();

  List<DonkList> findByBlockTimestampIsAfterAndPostedIsFalseOrderByBlockTimestampAsc(
      Date blockTimestamp);

  boolean existsByTokenIdAndBlockTimestampAndSalePrice(
      Integer tokenId, Date blockTimestamp, BigDecimal salePrice);
}
