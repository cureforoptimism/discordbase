package com.cureforoptimism.discordbase.discord.command;

import com.cureforoptimism.discordbase.Constants;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class TwerkCommand implements DiscordCommand {
  private final String[] twerks =
      new String[] {
        Constants.EMOJI_HEEHAW
            + " "
            + Constants.EMOJI_DONKETWERK
            + " "
            + Constants.EMOJI_HEEHAW
            + "\n"
            + Constants.EMOJI_DONKETWERK
            + " "
            + Constants.EMOJI_HEEHAW
            + " "
            + Constants.EMOJI_DONKETWERK
            + "\n"
            + Constants.EMOJI_HEEHAW
            + " "
            + Constants.EMOJI_DONKETWERK
            + " "
            + Constants.EMOJI_HEEHAW,
        Constants.EMOJI_DONKETWERK
            + " "
            + Constants.EMOJI_CARROTDANCE
            + " "
            + Constants.EMOJI_DONKETWERK
            + "\n"
            + Constants.EMOJI_CARROTDANCE
            + " "
            + Constants.EMOJI_DONKETWERK
            + " "
            + Constants.EMOJI_CARROTDANCE
            + "\n"
            + Constants.EMOJI_DONKETWERK
            + " "
            + Constants.EMOJI_CARROTDANCE
            + " "
            + Constants.EMOJI_DONKETWERK,
        Constants.EMOJI_SMOLTWERK
            + " "
            + Constants.EMOJI_DONKETWERK
            + "\n"
            + Constants.EMOJI_DONKETWERK
            + " "
            + Constants.EMOJI_SMOLTWERK
            + "\n"
            + Constants.EMOJI_DONKETWERK
            + " "
            + Constants.EMOJI_DONKETWERK
            + " "
            + Constants.EMOJI_DONKETWERK
            + "\n"
            + Constants.EMOJI_SMOLTWERK
            + " "
            + Constants.EMOJI_SMOLTWERK
            + " "
            + Constants.EMOJI_SMOLTWERK
      };

  @Override
  public String getName() {
    return "twerk";
  }

  @Override
  public String getDescription() {
    return "Donke Twerk Party!";
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
            c -> c.createMessage(twerks[ThreadLocalRandom.current().nextInt(0, twerks.length)]));
  }

  @Override
  public Mono<Void> handle(ChatInputInteractionEvent event) {
    event.reply(twerks[ThreadLocalRandom.current().nextInt(0, twerks.length)]).block();

    return Mono.empty();
  }
}
