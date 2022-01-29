package com.cureforoptimism.discordbase;

import discord4j.core.event.domain.message.MessageCreateEvent;

public class Utilities {
  public static boolean isAdminEvent(MessageCreateEvent event) {
    // TODO: Make approved roles a set, and hardcode server ID so this can't be added elsewhere and
    // queried

    final var approved =
        event
            .getMember()
            .filter(
                m -> {
                  final var role =
                      m.getRoles()
                          .filter(
                              r ->
                                  r.getName().equalsIgnoreCase("donkeynator")
                                      || r.getName().equalsIgnoreCase("ass patrol")
                                      || r.getName().equalsIgnoreCase("modonkey")
                                      || r.getName().equals("core donkey"))
                          .blockFirst();

                  return role != null;
                })
            .orElse(null);

    return approved != null;
  }
}
