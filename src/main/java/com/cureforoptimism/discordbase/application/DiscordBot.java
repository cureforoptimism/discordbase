package com.cureforoptimism.discordbase.application;

import com.cureforoptimism.discordbase.Constants;
import com.cureforoptimism.discordbase.discord.event.RefreshEvent;
import com.cureforoptimism.discordbase.discord.listener.DiscordCommandListener;
import com.cureforoptimism.discordbase.service.TokenService;
import discord4j.common.JacksonResources;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.service.ApplicationService;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
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
  @Getter final Set<Long> ignoredChannels;

  // TODO: This sucks. Makes this suck less with a rational pattern.
  @Getter Double currentPrice;
  @Getter Double currentChange;
  @Getter Double currentChange12h;
  @Getter Double currentChange4h;
  @Getter Double currentChange1h;
  @Getter Double currentVolume24h;
  @Getter Double currentVolume12h;
  @Getter Double currentVolume4h;
  @Getter Double currentVolume1h;

  public DiscordBot(ApplicationContext context, TokenService tokenService) {
    this.context = context;
    this.tokenService = tokenService;

    this.ignoredChannels = new HashSet<>();
    this.ignoredChannels.add(Constants.CHANNEL_DONKIVIA);
  }

  public void refreshMagicPrice(
      Double price,
      Double usd24HChange,
      Double change12h,
      Double change4h,
      Double change1h,
      Double volume24h,
      Double volume12h,
      Double volume4h,
      Double volume1h) {
    currentPrice = price;
    currentChange = usd24HChange;
    currentChange12h = change12h;
    currentChange4h = change4h;
    currentChange1h = change1h;
    currentVolume24h = volume24h;
    currentVolume12h = volume12h;
    currentVolume4h = volume4h;
    currentVolume1h = volume1h;

    client.getEventDispatcher().publish(new RefreshEvent(null, null));
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

    client
        .on(RefreshEvent.class)
        .subscribe(
            event -> {
              String nickName = ("MAGIC $" + currentPrice);
              String presence = String.format("24h: %.2f%%", currentChange);
              client.getGuilds().toStream().forEach(g -> g.changeSelfNickname(nickName).block());
              client
                  .updatePresence(ClientPresence.online(ClientActivity.watching(presence)))
                  .block();
            });

    manageSlashCommands(client);

    //    final var channel = client
    //        .getChannelById(Snowflake.of(934757662397190195L)).block();
    //    final var messages =
    // channel.getRestChannel().getMessagesAfter(Snowflake.of(936370363435216937L));
    //    final var prayers = new HashMap<String, Integer>();
    //    messages
    //        .all(
    //            m -> {
    //              final var timestamp = m.timestamp();
    //              final var instantStamp = Instant.parse(timestamp);
    //
    //              if(instantStamp.isAfter(Instant.parse("2022-01-27T22:30:00.728000+00:00"))) {
    //                return true;
    //              }
    //
    //              boolean prayerFound =
    //                  m.content().startsWith("!pray")
    //                      || m.content().toLowerCase().startsWith(Constants.EMOJI_PRAY)
    //                      ||
    // m.content().toLowerCase().startsWith("<:holydonke:934097858574053416>");
    //
    //              final var stickers = m.stickerItems();
    //              if (!stickers.isAbsent()) {
    //                for (PartialStickerData sticker : stickers.get()) {
    //                  if (sticker.name().equalsIgnoreCase("pray")
    //                      || sticker.name().equalsIgnoreCase("holy donkey")) {
    //                    prayerFound = true;
    //                  }
    //                }
    //              }
    //
    //              if (prayerFound) {
    //                String humanId = m.author().username() + "#" + m.author().discriminator();
    //
    //                if (!prayers.containsKey(humanId)) {
    //                  prayers.put(humanId, 0);
    //                }
    //
    //                prayers.put(humanId, prayers.get(humanId) + 1);
    //              }
    //
    //              return true;
    //            })
    //        .block();
    //
    //    final var list =
    // prayers.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(Collectors.toList());
    //    for(Map.Entry<String, Integer> entry : list) {
    //      log.info(entry.getKey() + " - " + entry.getValue());
    //    }
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
}
