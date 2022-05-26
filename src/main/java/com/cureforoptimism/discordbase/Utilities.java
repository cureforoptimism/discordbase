package com.cureforoptimism.discordbase;

import com.cureforoptimism.discordbase.domain.RarityRank;
import com.cureforoptimism.discordbase.repository.DonkRarityRankRepository;
import discord4j.core.event.domain.message.MessageCreateEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Optional;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class Utilities {
  private final DonkRarityRankRepository donkRarityRankRepository;

  public Utilities(DonkRarityRankRepository donkRarityRankRepository) {
    this.donkRarityRankRepository = donkRarityRankRepository;

    // Uncomment to regenerate rarities
    //    generateRanks();
  }

  void generateRanks() {
    InputStream csv;
    CSVParser parser;

    try {
      csv = new ClassPathResource("rarities.csv").getInputStream();
    } catch (IOException ex) {
      log.error("Unable to read CSV", ex);
      return;
    }

    try {
      parser = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(new InputStreamReader(csv));
    } catch (IOException ex) {
      log.error("Unable to parse CSV", ex);
      return;
    }

    parser.stream()
        .forEach(
            r -> {
              final var rank = r.get("nft_rank");
              final var id = r.get("id");
              final var score = r.get("rarity score");

              donkRarityRankRepository.save(
                  RarityRank.builder()
                      .donkId(Long.parseLong(id))
                      .rank(Integer.parseInt(rank))
                      .score(Double.parseDouble(score.replace(",", "")))
                      .build());
            });
    log.info("rarities generated and saved");
  }

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

  public Optional<BufferedImage> getDonkBufferedImage(String id) {
    final Path path = Paths.get("img_cache", "donks", id + ".png");
    if (path.toFile().exists()) {
      // Read
      try {
        ByteArrayInputStream bytes = new ByteArrayInputStream(Files.readAllBytes(path));
        BufferedImage img = ImageIO.read(bytes);
        return Optional.of(img);
      } catch (IOException e) {
        e.printStackTrace();
      }
    } else {
      // Fetch and write
      final var imgOpt = getDonkImage(id);
      if (imgOpt.isPresent()) {
        try {
          HttpClient httpClient = HttpClient.newHttpClient();
          HttpRequest request =
              HttpRequest.newBuilder()
                  .uri(new URI(imgOpt.get()))
                  .timeout(Duration.ofMillis(20000))
                  .build();

          for (int retry = 0; retry <= 5; retry++) {
            log.info("Requesting: " + request.uri().toString());
            final var response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            log.info("Response received: " + response.statusCode());
            if (response.statusCode() == 200) {
              log.info("Writing new cached object: " + path + "; try: " + (retry + 1));
              Files.write(path, response.body());

              ByteArrayInputStream imgBytes = new ByteArrayInputStream(response.body());
              BufferedImage img = ImageIO.read(imgBytes);

              return Optional.of(img);
            } else if (response.statusCode() == 504) {
              log.warn("TIMEOUT; RETURN NOW");
              return Optional.empty();
            } else {
              Thread.sleep(250);
              log.error("Unable to retrieve image (will retry): " + response.statusCode());
            }
          }
        } catch (Exception ex) {
          log.error("Error retrieving donk image");
        }
      }
    }

    return Optional.empty();
  }

  public Optional<String> getDonkImage(String id) {
    // Maybe we'll want to look this up by contract, eventually, since it's the real source of
    // truth. But meh. Let's consider this immutable for now.
    return Optional.of(
        "https://thelostdonkeys.mypinata.cloud/ipfs/QmTSgHvkYHEyxS3TdJ1hznkB1XXW7vXaraFiEwHV9vgdNP/"
            + id
            + ".png");
  }
}
