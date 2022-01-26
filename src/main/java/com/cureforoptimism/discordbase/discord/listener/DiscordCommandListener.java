package com.cureforoptimism.discordbase.discord.listener;

import com.cureforoptimism.discordbase.discord.command.DiscordCommand;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import java.util.Collection;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
public class DiscordCommandListener {
  private final Collection<DiscordCommand> commands;
  final Pattern pattern = Pattern.compile("^!?he+ha+w+", Pattern.MULTILINE);
  final Pattern heehee = Pattern.compile("^!?he+he+", Pattern.MULTILINE);

  public DiscordCommandListener(ApplicationContext applicationContext) {
    commands = applicationContext.getBeansOfType(DiscordCommand.class).values();
  }

  public Mono<Void> handle(ChatInputInteractionEvent event) {
    try {
      Flux.fromIterable(commands)
          .filter(command -> command.getName().equals(event.getCommandName()))
          .next()
          .flatMap(command -> command.handle(event))
          .block();
    } catch (Exception ex) {
      log.error(ex.getMessage());
    }

    return Mono.empty();
  }

  public void handle(MessageCreateEvent event) {
    try {
      String message = event.getMessage().getContent().toLowerCase();
      if (pattern.matcher(message.toLowerCase()).matches()
          || heehee.matcher(message.toLowerCase()).matches()) {
        message = "!heehaw";
      } else if (!message.startsWith("!")) {
        return;
      }

      // Trim leading !
      String[] parts = message.split(" ");
      if (parts.length > 0 && !parts[0].isEmpty()) {
        String commandName = parts[0].substring(1);

        Flux.fromIterable(commands)
            .filter(command -> command.getName().equals(commandName))
            .next()
            .flatMap(
                command -> {
                  // Verify that this is a message in a server and not a DM (for now)
                  Message msg = event.getMessage();
                  if (msg.getGuildId().isEmpty()) {
                    return Mono.empty();
                  }

                  return command.handle(event);
                })
            .block();
      }
    } catch (Exception ex) {
      log.error("Error received in listener loop. Will resume.", ex);
    }
  }
}
