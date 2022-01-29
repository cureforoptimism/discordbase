package com.cureforoptimism.discordbase.discord.listener;

import com.cureforoptimism.discordbase.Constants;
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
  final Pattern yeehaw = Pattern.compile("^!?ye+ha+w+", Pattern.MULTILINE);

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
      // 933053775453757530 is #the-barn, and 934412437707431976L is donkivia; let's ignore stuff in there
      final var channelId = event.getMessage().getChannel().block().getId().asLong();

      if (channelId == 934412437707431976L) { // || channelId == 933053775453757530L) {
        return;
      }

      String message = event.getMessage().getContent().toLowerCase();

      if (pattern.matcher(message).matches()
          || heehee.matcher(message).matches()
          || yeehaw.matcher(message).matches()) {
        message = "!heehaw";
      } else if (message.toLowerCase().startsWith(Constants.EMOJI_PRAY)
          || message.toLowerCase().startsWith("<:holydonke:934097858574053416>")) {
        message = "!pray";
      } else if (yeehaw.matcher(message.toLowerCase()).matches()) {

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
