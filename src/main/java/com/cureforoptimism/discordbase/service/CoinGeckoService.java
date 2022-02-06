package com.cureforoptimism.discordbase.service;

import com.cureforoptimism.discordbase.application.DiscordBot;
import com.litesoftwares.coingecko.CoinGeckoApiClient;
import com.litesoftwares.coingecko.domain.Coins.CoinFullData;
import java.util.Optional;
import lombok.Getter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class CoinGeckoService implements MagicValueService {
  private final CoinGeckoApiClient client;
  private final DiscordBot discordClient;
  private final boolean enabled = false;
  @Getter private CoinFullData coinFullData;
  @Getter private Optional<Double> ethPrice;

  public CoinGeckoService(CoinGeckoApiClient client, DiscordBot discordClient) {
    this.client = client;
    this.discordClient = discordClient;
  }

  @Scheduled(fixedDelay = 30000)
  public synchronized void refreshMagicPrice() {
    this.coinFullData = client.getCoinById("magic");
    this.ethPrice =
        Optional.of(
            client
                .getPrice("ethereum", "usd", false, false, false, false)
                .get("ethereum")
                .get("usd"));

    if (!enabled) {
      return;
    }

    try {
      final var priceMap = client.getPrice("magic", "usd", false, false, true, false);
      if (priceMap.containsKey("magic")
          && priceMap.get("magic").containsKey("usd")
          && priceMap.get("magic").containsKey("usd_24h_change")) {
        discordClient.refreshMagicPrice(
            priceMap.get("magic").get("usd"),
            priceMap.get("magic").get("usd_24h_change"),
            null,
            null,
            null,
            null,
            null,
            null,
            null);
      }
    } catch (Exception ex) {
      // Ignore, it'll retry
    }
  }
}