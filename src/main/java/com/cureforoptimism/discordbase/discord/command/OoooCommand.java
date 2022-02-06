package com.cureforoptimism.discordbase.discord.command;

import com.cureforoptimism.discordbase.Constants;
import com.cureforoptimism.discordbase.domain.Oooo;
import com.cureforoptimism.discordbase.repository.OoooRepository;
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
public class OoooCommand implements DiscordCommand {
  private final OoooRepository ooooRepository;
  private final Set<String> suffixes;

  public OoooCommand(OoooRepository ooooRepository) {
    this.ooooRepository = ooooRepository;

    this.suffixes = Set.of("Oooooooooo");
  }

  @Override
  public String getName() {
    return "oooo";
  }

  @Override
  public String getDescription() {
    return "Oooo!";
  }

  @Override
  public String getUsage() {
    return null;
  }

  @Override
  public Mono<Message> handle(MessageCreateEvent event) {
    final var previousCroak =
        ooooRepository.findFirstByDiscordUserIdOrderByCreatedAtDesc(
            event.getMessage().getUserData().id().asLong());
    if (previousCroak.isPresent()) {
      Date previousCroakDate = previousCroak.get().getCreatedAt();

      if (previousCroakDate != null
          && previousCroak
              .get()
              .getCreatedAt()
              .after(new Date(System.currentTimeMillis() - 30000L))) {
        return Mono.empty();
      }
    }

    ooooRepository.save(
        Oooo.builder()
            .discordUserId(event.getMessage().getUserData().id().asLong())
            .discordId(
                event.getMessage().getUserData().username()
                    + "#"
                    + event.getMessage().getUserData().discriminator())
            .createdAt(new Date())
            .build());

    return event
        .getMessage()
        .getChannel()
        .flatMap(
            c ->
                c.createMessage(
                    Constants.EMOJI_PIXILFRAME_039
                        + " **"
                        + NumberFormat.getIntegerInstance()
                            .format(ooooRepository.findFirstByOrderByIdDesc().getId())
                        + " oooos!** Ooooogaooooooooo!"));
  }

  @Override
  public Mono<Void> handle(ChatInputInteractionEvent event) {
    if (!canOooo(event.getInteraction().getUser().getId().asLong())) {
      return Mono.empty();
    }

    ooooRepository.save(
        Oooo.builder()
            .discordUserId(event.getInteraction().getUser().getId().asLong())
            .discordId(
                event.getInteraction().getUser().getUsername()
                    + "#"
                    + event.getInteraction().getUser().getDiscriminator())
            .createdAt(new Date())
            .build());

    String suffix =
        suffixes.stream().skip(new Random().nextInt(suffixes.size())).findFirst().orElse("");

    event
        .reply(
            Constants.EMOJI_PIXILFRAME_039
                + " **"
                + NumberFormat.getIntegerInstance()
                    .format(ooooRepository.findFirstByOrderByIdDesc().getId())
                + " oooos!** Ooooogaooooooooo!")
        .block();

    return Mono.empty();
  }

  private boolean canOooo(Long discordId) {
    final var previousHeehaw =
        ooooRepository.findFirstByDiscordUserIdOrderByCreatedAtDesc(discordId);
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
}
