package com.cureforoptimism.discordbase.application;

import com.cureforoptimism.discordbase.discord.listener.DiscordCommandListener;
import com.cureforoptimism.discordbase.service.TokenService;
import discord4j.common.JacksonResources;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.service.ApplicationService;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DiscordBot implements ApplicationRunner {
  final ApplicationContext context;
  static GatewayDiscordClient client;
  final TokenService tokenService;

  public DiscordBot(ApplicationContext context, TokenService tokenService) {
    this.context = context;
    this.tokenService = tokenService;
  }

  @Override
  public void run(ApplicationArguments args) throws IOException {
    DiscordCommandListener commandListener = new DiscordCommandListener(context);

    client =
        DiscordClientBuilder.create(tokenService.getDiscordToken())
            .build()
            .gateway()
            .login()
            .block();

    if (client == null) {
      log.error("Unable to create Discord client");
      System.exit(-1);
    }

    client
        .getEventDispatcher()
        .on(ChatInputInteractionEvent.class)
        .subscribe(commandListener::handle);

    client.getEventDispatcher().on(MessageCreateEvent.class).subscribe(commandListener::handle);

    log.info("Discord client logged in");
  }

  private void manageSlashCommands(GatewayDiscordClient client) throws IOException {
    if (client == null) {
      throw new IOException("Discord client may not be null to register slash commands");
    }

    final JacksonResources mapper = JacksonResources.create();
    PathMatchingResourcePatternResolver matcher = new PathMatchingResourcePatternResolver();
    final ApplicationService applicationService = client.rest().getApplicationService();
    final long applicationId = client.rest().getApplicationId().block();

    // Already registered commands...
    Map<String, ApplicationCommandData> slashCommands =
        applicationService
            .getGlobalApplicationCommands(applicationId)
            .collectMap(ApplicationCommandData::name)
            .block();

    Map<String, ApplicationCommandRequest> jsonCommands = new HashMap<>();

    // Add new commands
    for (Resource resource : matcher.getResources("commands/*.json")) {
      ApplicationCommandRequest request =
          mapper
              .getObjectMapper()
              .readValue(resource.getInputStream(), ApplicationCommandRequest.class);

      jsonCommands.put(request.name(), request);

      if (!slashCommands.containsKey(request.name())) {
        applicationService.createGlobalApplicationCommand(applicationId, request).block();
        log.info("Created new global command: " + request.name());
      }
    }

    // Delete old/unused commands
    for (ApplicationCommandData command : slashCommands.values()) {
      long commandId = Long.parseLong(command.id());

      ApplicationCommandRequest request = jsonCommands.get(command.name());

      if (request == null) {
        applicationService.deleteGlobalApplicationCommand(applicationId, commandId).block();

        log.info("Deleted global command: " + command.name());
        continue;
      }

      if (hasChanged(command, request)) {
        applicationService
            .modifyGlobalApplicationCommand(applicationId, commandId, request)
            .block();

        log.info("Modified global command: " + request.name());
      }
    }
  }

  // Basically an isEquals for ApplicationCommandData
  private boolean hasChanged(
      ApplicationCommandData discordCommand, ApplicationCommandRequest command) {
    // Compare types
    if (!discordCommand.type().toOptional().orElse(1).equals(command.type().toOptional().orElse(1)))
      return true;

    // Check if description has changed.
    if (!discordCommand.description().equals(command.description().toOptional().orElse("")))
      return true;

    // Check if default permissions have changed
    boolean discordCommandDefaultPermission =
        discordCommand.defaultPermission().toOptional().orElse(true);
    boolean commandDefaultPermission = command.defaultPermission().toOptional().orElse(true);

    if (discordCommandDefaultPermission != commandDefaultPermission) return true;

    // Check and return if options have changed.
    return !discordCommand.options().equals(command.options());
  }

  public void postMessage(MessageCreateSpec messageCreateSpec, List<Long> discordChannelIds) {
    for (Long discordChannelId : discordChannelIds) {
      try {
        final var messages =
            client
                .getChannelById(Snowflake.of(discordChannelId))
                .ofType(MessageChannel.class)
                .flatMap(c -> c.createMessage(messageCreateSpec));

        messages.block();
      } catch (Exception ex) {
        log.warn("Unable to post to channel: " + discordChannelId, ex);
      }
    }
  }
}
