package com.cureforoptimism.discordbase.discord.command;

import com.cureforoptimism.discordbase.Constants;
import com.cureforoptimism.discordbase.domain.Heehaw;
import com.cureforoptimism.discordbase.repository.HeehawRepository;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.MessageCreateSpec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class HeehawCommand implements DiscordCommand {
  private final HeehawRepository heehawRepository;
  private final Set<String> suffixes;
  private byte[] heehee;
  private byte[] yeehaw;
  final Pattern hehePattern = Pattern.compile("^!?he+he+", Pattern.MULTILINE);
  final Pattern yeehawPattern = Pattern.compile("^!?ye+ha+w+", Pattern.MULTILINE);

  public HeehawCommand(HeehawRepository heehawRepository) {
    this.heehawRepository = heehawRepository;

    this.suffixes = Set.of("Heehaw!");

    try {
      this.heehee = new ClassPathResource("heehee.jpg").getInputStream().readAllBytes();
      this.yeehaw = new ClassPathResource("yeehaw.jpg").getInputStream().readAllBytes();
    } catch (IOException ex) {
      log.error("Unable to load image", ex);
      System.exit(-1);
    }
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
  public Boolean adminOnly() {
    return false;
  }

  @Override
  public Mono<Message> handle(MessageCreateEvent event) {
    if (!canHeeHaw(event.getMessage().getUserData().id().asLong())) {
      return Mono.empty();
    }

    String msg = event.getMessage().getContent();
    String[] parts = msg.split(" ");

    // Heehee?
    if (hehePattern.matcher(parts[0].toLowerCase()).matches()) {
      return event
          .getMessage()
          .getChannel()
          .flatMap(
              c ->
                  c.createMessage(
                      MessageCreateSpec.builder()
                          .addFile("heehee.jpg", new ByteArrayInputStream(this.heehee))
                          .build()));
    } else if (yeehawPattern.matcher(parts[0].toLowerCase()).matches()) {
      // TODO: DRY, for the love of God
      return event
          .getMessage()
          .getChannel()
          .flatMap(
              c ->
                  c.createMessage(
                      MessageCreateSpec.builder()
                          .addFile("yeehaw.jpg", new ByteArrayInputStream(this.yeehaw))
                          .build()));
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
                    "<a:donketwerk:934113221315551352> **"
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
    if (!canHeeHaw(event.getInteraction().getUser().getId().asLong())) {
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

    event
        .reply(
            Constants.EMOJI_DONKETWERK
                + " **"
                + NumberFormat.getIntegerInstance()
                    .format(heehawRepository.findFirstByOrderByIdDesc().getId())
                + " Heehaws**, and counting! "
                + suffix)
        .block();

    return Mono.empty();
  }
}
