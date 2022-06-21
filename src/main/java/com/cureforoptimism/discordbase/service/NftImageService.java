package com.cureforoptimism.discordbase.service;

import com.cureforoptimism.discordbase.domain.Bird;
import com.cureforoptimism.discordbase.domain.Trait;
import com.cureforoptimism.discordbase.repository.BirdRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.web3j.contracts.eip721.generated.ERC721Metadata;
import org.web3j.tx.exceptions.ContractCallException;

@Service
@Slf4j
public class NftImageService {
  @Autowired
  @Qualifier("erc721Metadata")
  private final ERC721Metadata erc721Metadata;

  private final BirdRepository birdRepository;

  public NftImageService(ERC721Metadata erc721Metadata, BirdRepository birdRepository) {
    this.erc721Metadata = erc721Metadata;
    this.birdRepository = birdRepository;
  }

  public Optional<Bird> getBird(String tokenId) {
    var birdOpt = birdRepository.findById(Long.parseLong(tokenId));
    if (birdOpt.isPresent()) {
      return birdOpt;
    }

    final ObjectMapper objMapper = new ObjectMapper();

    String tokenUriResponse = "";
    while (tokenUriResponse.isEmpty()) {
      try {
        tokenUriResponse = erc721Metadata.tokenURI(new BigInteger(tokenId)).send();
      } catch (ContractCallException ex) {
        if (ex.getMessage().contains("Token does not exist")) {
          return Optional.empty();
        }
      } catch (Exception ex) {
        log.error("Error retrieving image: " + tokenId, ex);
        try {
          Thread.sleep(1000);
        } catch (InterruptedException ignored) {
          // meh
        }
      }
    }

    tokenUriResponse = tokenUriResponse.replace("ipfs://", "https://gateway.pinata.cloud/ipfs/");

    String tokenJson;
    try {
      HttpClient httpClient = HttpClient.newHttpClient();
      HttpRequest request = HttpRequest.newBuilder().GET().uri(new URI(tokenUriResponse)).build();

      tokenJson = httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
    } catch (URISyntaxException | InterruptedException | IOException ex) {
      log.error("Exception on URI: " + tokenUriResponse, ex);
      return Optional.empty();
    }

    try {
      final var bird = objMapper.readValue(tokenJson.getBytes(StandardCharsets.UTF_8), Bird.class);

      bird.setId(Long.parseLong(tokenId));
      for (Trait trait : bird.getTraits()) {
        trait.setBird(bird);
      }

      birdRepository.save(bird);

      return Optional.of(bird);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public BufferedImage getImage(String tokenId) {
    final var birdOpt = getBird(tokenId);
    if (birdOpt.isEmpty()) {
      return null;
    }

    final var bird = birdOpt.get();
    final var suffix =
        Optional.ofNullable(bird.getImage())
            .filter(f -> f.contains("."))
            .map(f -> f.substring(bird.getImage().lastIndexOf(".") + 1))
            .orElse(".png");

    final Path path = Paths.get("img_cache/birds/", tokenId + "." + suffix);
    if (path.toFile().exists()) {
      // Read
      try {
        ByteArrayInputStream bytes = new ByteArrayInputStream(Files.readAllBytes(path));
        return ImageIO.read(bytes);
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      }
    }

    try {
      String ipfsImage = bird.getImage().replace("ipfs://", "https://gateway.pinata.cloud/ipfs/");

      HttpClient httpClient = HttpClient.newHttpClient();
      HttpRequest request = HttpRequest.newBuilder().GET().uri(new URI(ipfsImage)).build();

      final var response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

      // Add to cache
      log.info("Writing new cached object: " + path);
      Files.write(path, response.body());

      ByteArrayInputStream imgBytes = new ByteArrayInputStream(response.body());
      return ImageIO.read(imgBytes);
    } catch (URISyntaxException | InterruptedException | IOException ex) {
      log.error("Exception on URI: " + tokenId, ex);
      return null;
    }
  }
}
