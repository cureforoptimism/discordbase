package com.cureforoptimism.discordbase.repository;

import com.cureforoptimism.discordbase.domain.DonkSale;
import java.util.Date;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DonkSaleRepository extends JpaRepository<DonkSale, String> {
  DonkSale findFirstByPostedIsTrueOrderByBlockTimestampDesc();

  List<DonkSale> findByBlockTimestampIsAfterAndPostedIsFalseOrderByBlockTimestampAsc(
      Date blockTimestamp);
}
