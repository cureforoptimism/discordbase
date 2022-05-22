package com.cureforoptimism.discordbase.service;

import com.litesoftwares.coingecko.CoinGeckoApiClient;
import com.litesoftwares.coingecko.domain.Coins.CoinFullData;
import java.util.Optional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CoinGeckoService {
  private final CoinGeckoApiClient client;
  private final boolean enabled = false;
  @Getter private CoinFullData coinFullData;
  @Getter private Optional<Double> ethPrice;

  public CoinGeckoService(CoinGeckoApiClient client) {
    this.client = client;
  }

  @Scheduled(fixedDelay = 30000)
  public synchronized void refreshMagicPrice() {
    try {
      this.coinFullData = client.getCoinById("magic");
      this.ethPrice =
          Optional.of(
              client
                  .getPrice("ethereum", "usd", false, false, false, false)
                  .get("ethereum")
                  .get("usd"));
    } catch (Exception ex) {
      log.error("Error retrieving MAGIC price from coingecko", ex);
      // Ignore, it'll retry
    } finally {
      client.shutdown();
    }
  }
}
