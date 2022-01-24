package com.cureforoptimism.discordbase.discord.command;

import com.cureforoptimism.discordbase.domain.Heehaw;
import com.cureforoptimism.discordbase.repository.HeehawRepository;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Random;
import java.util.Set;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class HeehawCommand implements DiscordCommand {
  private final HeehawRepository heehawRepository;
  private final Set<String> suffixes;

  public HeehawCommand(HeehawRepository heehawRepository) {
    this.heehawRepository = heehawRepository;

    this.suffixes = Set.of("Heehaw!");
  }

  @Override
  public String getName() {
    return "heehaw";
  }

  @Override
  public String getDescription() {
    return "Heehaw!";
  }

  @Override
  public String getUsage() {
    return null;
  }

  @Override
  public Mono<Message> handle(MessageCreateEvent event) {
    if(!canHeeHaw(event.getMessage().getUserData().id().asLong())) {
      return Mono.empty();
    }

    heehawRepository.save(
        Heehaw.builder()
            .discordUserId(event.getMessage().getUserData().id().asLong())
            .discordId(
                event.getMessage().getUserData().username()
                    + "#"
                    + event.getMessage().getUserData().discriminator())
            .createdAt(new Date())
            .build());

    String suffix =
        suffixes.stream().skip(new Random().nextInt(suffixes.size())).findFirst().orElse("");

    return event
        .getMessage()
        .getChannel()
        .flatMap(
            c ->
                c.createMessage(
                    "<:donketwerk:935224181396754442> **"
                        + NumberFormat.getIntegerInstance()
                            .format(heehawRepository.findFirstByOrderByIdDesc().getId())
                        + " Heehaws**, and counting! "
                        + suffix));
  }

  private boolean canHeeHaw(Long discordId) {
    final var previousHeehaw =
        heehawRepository.findFirstByDiscordUserIdOrderByCreatedAtDesc(discordId);
    if (previousHeehaw.isPresent()) {
      Date previousHeehawDate = previousHeehaw.get().getCreatedAt();

      return previousHeehawDate == null
          || !previousHeehaw
          .get()
          .getCreatedAt()
          .after(new Date(System.currentTimeMillis() - 30000L));
    }

    return true;
  }

  @Override
  public Mono<Void> handle(ChatInputInteractionEvent event) {
    if(!canHeeHaw(event.getInteraction().getUser().getId().asLong())) {
      return Mono.empty();
    }

    heehawRepository.save(
        Heehaw.builder()
            .discordUserId(event.getInteraction().getUser().getId().asLong())
            .discordId(
                event.getInteraction().getUser().getUsername()
                    + "#"
                    + event.getInteraction().getUser().getDiscriminator())
            .createdAt(new Date())
            .build());

    String suffix =
        suffixes.stream().skip(new Random().nextInt(suffixes.size())).findFirst().orElse("");

    event.reply("<:donketwerk:935224181396754442> **"
        + NumberFormat.getIntegerInstance()
        .format(heehawRepository.findFirstByOrderByIdDesc().getId())
        + " Heehaws**, and counting! "
        + suffix).block();

    return Mono.empty();
  }
}