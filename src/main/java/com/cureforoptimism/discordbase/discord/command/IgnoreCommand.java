package com.cureforoptimism.discordbase.discord.command;

import com.cureforoptimism.discordbase.Utilities;
import com.cureforoptimism.discordbase.application.DiscordBot;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
public class IgnoreCommand implements DiscordCommand {
  private final DiscordBot discordBot;

  @Override
  public String getName() {
    return "ignore";
  }

  @Override
  public String getDescription() {
    return "Add a channel to the ignore list";
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
    if (!Utilities.isAdminEvent(event)) {
      return Mono.empty();
    }

    String message = event.getMessage().getContent().toLowerCase();
    String[] parts = message.split(" ");
    if (parts.length < 2) {
      return event
          .getMessage()
          .getChannel()
          .flatMap(c -> c.createMessage("No channel ID specified to ignore!"));
    }

    String channelStr = parts[1].trim();

    if (channelStr.startsWith("<#") && channelStr.endsWith(">")) {
      channelStr = channelStr.replace("<", "").replace("#", "").replace(">", "");
    }

    long channelId;

    try {
      channelId = Long.parseLong(channelStr);
    } catch (NumberFormatException ex) {
      return event
          .getMessage()
          .getChannel()
          .flatMap(
              c ->
                  c.createMessage(
                      "Invalid channel specified"));
    }

    discordBot.getIgnoredChannels().add(channelId);

    return event
        .getMessage()
        .getChannel()
        .flatMap(
            c ->
                c.createMessage(
                    "Now ignoring channel: "
                        + parts[1]
                        + "; to start listening again, use !unignore <channel_id>"));
  }

  @Override
  public Mono<Void> handle(ChatInputInteractionEvent event) {
    return null;
  }
}
