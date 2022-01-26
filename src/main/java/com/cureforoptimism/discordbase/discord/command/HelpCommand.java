package com.cureforoptimism.discordbase.discord.command;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import java.util.Collection;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class HelpCommand implements DiscordCommand {
  final ApplicationContext context;
  final Collection<DiscordCommand> commands;

  public HelpCommand(ApplicationContext context) {
    this.context = context;
    this.commands = context.getBeansOfType(DiscordCommand.class).values();
  }

  @Override
  public String getName() {
    return "help";
  }

  @Override
  public String getDescription() {
    return "This help message";
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
    StringBuilder helpMsg = new StringBuilder();

    for (DiscordCommand command : commands) {
      if (command.adminOnly()) {
        continue;
      }

      helpMsg.append("`!").append(command.getName());

      if (command.getUsage() != null) {
        helpMsg.append(" ").append(command.getUsage());
      }

      helpMsg.append("` - ").append(command.getDescription()).append("\n");
    }

    final var msg =
        EmbedCreateSpec.builder()
            .title("DonkeyBot Help")
            .author(
                "DonkeyBot",
                null,
                "https://pbs.twimg.com/profile_images/1484839171621629954/Dm2FLqb8_400x400.jpg")
            .description(helpMsg.toString())
            .addField(
                "Note: ",
                "This bot developed for fun by `Cure For Optimism#5061`, and is unofficial. Lost Donkey admins aren't associated with this bot and can't help you with issues. Ping Cure For Optimism with any feedback/questions",
                false)
            .build();
    return event.getMessage().getChannel().flatMap(c -> c.createMessage(msg));
  }

  @Override
  public Mono<Void> handle(ChatInputInteractionEvent event) {
    return null;
  }
}
