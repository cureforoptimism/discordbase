package com.cureforoptimism.discordbase.service;

import com.cureforoptimism.discordbase.application.DiscordBot;
import com.cureforoptimism.discordbase.domain.MarketPrice;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketPriceMessageSubscriber {
  private final DiscordBot discordBot;

  @Getter private MarketPrice lastMarketPlace;

  public void handleMessage(MarketPrice marketPrice) {
    lastMarketPlace = marketPrice;
  }
}
