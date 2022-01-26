package com.cureforoptimism.discordbase.discord.command;

import com.cureforoptimism.discordbase.domain.Prayer;
import com.cureforoptimism.discordbase.repository.PrayerRepository;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
@Slf4j
public class PrayCommand implements DiscordCommand {
  private final PrayerRepository prayerRepository;

  @Override
  public String getName() {
    return "pray";
  }

  @Override
  public String getDescription() {
    return "Pray for the lost donkeys";
  }

  @Override
  public String getUsage() {
    return null;
  }

  @Override
  public Boolean adminOnly() {
    return false;
  }

  @Override
  public Mono<Message> handle(MessageCreateEvent event) {
    log.info("!pray command received");

    Long discordId = event.getMessage().getUserData().id().asLong();
    if (!canPray(discordId)) {
      return Mono.empty();
    }

    prayerRepository.save(
        Prayer.builder()
            .discordUserId(discordId)
            .discordId(
                event.getMessage().getUserData().username()
                    + "#"
                    + event.getMessage().getUserData().discriminator())
            .createdAt(new Date())
            .build());

    // Prayers are currently silent...
    return Mono.empty();
  }

  @Override
  public Mono<Void> handle(ChatInputInteractionEvent event) {
    return null;
  }

  private boolean canPray(Long discordId) {
    final var previousPrayer =
        prayerRepository.findFirstByDiscordUserIdOrderByCreatedAtDesc(discordId);

    if (previousPrayer.isPresent()) {
      Date previousHeehawDate = previousPrayer.get().getCreatedAt();

      return previousHeehawDate == null
          || !previousPrayer
              .get()
              .getCreatedAt()
              .after(new Date(System.currentTimeMillis() - 30000L));
    }

    return true;
  }
}
