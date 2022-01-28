package com.cureforoptimism.discordbase.discord.command;

import com.cureforoptimism.discordbase.domain.Prayer;
import com.cureforoptimism.discordbase.repository.PrayerRepository;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.MessageCreateSpec;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
@Slf4j
public class TopPrayers implements DiscordCommand {
  private final PrayerRepository prayerRepository;

  @Override
  public String getName() {
    return "topprayers";
  }

  @Override
  public String getDescription() {
    return "";
  }

  @Override
  public String getUsage() {
    return null;
  }

  @Override
  public Boolean adminOnly() {
    return true;
  }

  @Override
  public Mono<Message> handle(MessageCreateEvent event) {
    // TODO: Make approved roles a set, and hardcode server ID so this can't be added elsewhere and
    // queried
    final var approved =
        event
            .getMember()
            .filter(
                m -> {
                  final var role =
                      m.getRoles()
                          .filter(
                              r ->
                                  r.getName().equalsIgnoreCase("donkeynator")
                                      || r.getName().equalsIgnoreCase("ass patrol")
                                      || r.getName().equalsIgnoreCase("modonkey")
                                      || r.getName().equals("core donkey"))
                          .blockFirst();

                  return role != null;
                })
            .orElse(null);

    if (approved != null) {
      // Process!
      final List<Prayer> topPrayers = prayerRepository.topPrayers();

      final StringBuilder output = new StringBuilder().append("--- TOP PRAYERS ---\n```");
      int x = 1;

      for (Prayer topPrayer : topPrayers) {
        output
            .append("#")
            .append(x++)
            .append(" - ")
            .append(topPrayer.getDiscordId())
            .append("; ")
            .append(prayerRepository.countByDiscordUserId(topPrayer.getDiscordUserId()))
            .append(" prayers!\n");
      }
      output.append("```");

      event
          .getMessage()
          .getChannel()
          .flatMap(
              c -> c.createMessage(MessageCreateSpec.builder().content(output.toString()).build()))
          .block();

      log.info(topPrayers.toString());
    }

    return Mono.empty();
  }

  @Override
  public Mono<Void> handle(ChatInputInteractionEvent event) {
    // We don't want to publish this; it's admin only
    return null;
  }
}
