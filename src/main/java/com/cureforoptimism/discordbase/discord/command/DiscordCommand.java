package com.cureforoptimism.discordbase.discord.command;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

public interface DiscordCommand {
  String getName();

  String getDescription();

  String getUsage();

  Boolean adminOnly();

  // Text command
  Mono<Message> handle(MessageCreateEvent event);

  // Slash command
  Mono<Void> handle(ChatInputInteractionEvent event);
}
