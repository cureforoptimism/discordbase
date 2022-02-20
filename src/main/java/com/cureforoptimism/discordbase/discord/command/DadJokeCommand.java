package com.cureforoptimism.discordbase.discord.command;

import com.cureforoptimism.discordbase.Constants;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class DadJokeCommand implements DiscordCommand {
  private List<Joke> jokes;

  private static class Joke {
    String question;
    String answer;

    public Joke(String question, String answer) {
      this.question = question;
      this.answer = answer;
    }
  }

  public DadJokeCommand() {
    jokes = new ArrayList<>();

    try {
      final var jokeStream = new ClassPathResource("dadjokes.txt").getInputStream();

      String[] jokesPairs =
          new String(jokeStream.readAllBytes(), StandardCharsets.UTF_8).split("\n");

      int x = 1;
      String previousPart = "";
      for (String part : jokesPairs) {
        if (part.isEmpty()) {
          x = 1;
          continue;
        }

        if (x % 2 == 0) {
          jokes.add(new Joke(previousPart, part));
        } else {
          previousPart = part;
        }

        x++;
      }
    } catch (IOException ex) {
      log.error("Unable to read joke file", ex);
    }
  }

  @Override
  public String getName() {
    return "dadjoke";
  }

  @Override
  public String getDescription() {
    return "Tells a random donkey dad joke!";
  }

  @Override
  public String getUsage() {
    return null;
  }

  @Override
  public Boolean adminOnly() {
    return false;
  }

  private Joke getRandomJoke() {
    Random rand = new Random();
    return jokes.get(rand.nextInt(jokes.size()));
  }

  @Override
  public Mono<Message> handle(MessageCreateEvent event) {
    Joke joke = getRandomJoke();

    return event
        .getMessage()
        .getChannel()
        .flatMap(
            c ->
                c.createMessage(
                    joke.question + "\n||" + joke.answer + "||" + Constants.EMOJI_HEEHAW));
  }

  @Override
  public Mono<Void> handle(ChatInputInteractionEvent event) {
    Joke joke = getRandomJoke();

    event.reply(joke.question + "\n||" + joke.answer + "||" + Constants.EMOJI_HEEHAW).block();

    return Mono.empty();
  }
}
