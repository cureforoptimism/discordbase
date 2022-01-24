package com.cureforoptimism.discordbase.discord.command;

import com.cureforoptimism.discordbase.application.DiscordBot;
import com.cureforoptimism.discordbase.service.CoinGeckoService;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@Component
public class MagicCommand implements DiscordCommand {
  private final DiscordBot discordBot;
  private final CoinGeckoService coinGeckoService;

  @Override
  public String getName() {
    return "magic";
  }

  @Override
  public String getDescription() {
    return "shows $MAGIC price in USD and ETH";
  }

  @Override
  public String getUsage() {
    return null;
  }

  @Override
  public Mono<Message> handle(MessageCreateEvent event) {
    return event
        .getMessage()
        .getChannel()
        .flatMap(
            c ->
                c.createMessage(
                    "MAGIC: $"
                        + discordBot.getCurrentPrice()
                        + " ("
                        + String.format(
                            "`%.6f`",
                            coinGeckoService
                                .getCoinFullData()
                                .getMarketData()
                                .getCurrentPrice()
                                .get("eth"))
                        + " ETH"
                        + ")"));
  }

  @Override
  public Mono<Void> handle(ChatInputInteractionEvent event) {
    return null;
  }
}
