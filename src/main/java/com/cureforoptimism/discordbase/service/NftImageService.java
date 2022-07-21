package com.cureforoptimism.discordbase.service;

import com.cureforoptimism.discordbase.domain.SmolAge;
import com.cureforoptimism.discordbase.domain.Trait;
import com.cureforoptimism.discordbase.repository.SmolAgeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
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

  private final SmolAgeRepository smolAgeRepository;

  public NftImageService(ERC721Metadata erc721Metadata, SmolAgeRepository smolAgeRepository) {
    this.erc721Metadata = erc721Metadata;
    this.smolAgeRepository = smolAgeRepository;
  }

  public Optional<SmolAge> getSmolAge(String tokenId) {
    var smolAgeOpt = smolAgeRepository.findById(Long.parseLong(tokenId));
    if (smolAgeOpt.isPresent()) {
      return smolAgeOpt;
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
      final var smolAge =
          objMapper.readValue(tokenJson.getBytes(StandardCharsets.UTF_8), SmolAge.class);

      smolAge.setId(Long.parseLong(tokenId));

      if (smolAge.getTraits() != null) {
        for (Trait trait : smolAge.getTraits()) {
          trait.setSmolAge(smolAge);
        }
      }

      smolAgeRepository.save(smolAge);

      return Optional.of(smolAge);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public ByteArrayOutputStream getImage(String tokenId) {
    final var smolAgeOpt = getSmolAge(tokenId);
    if (smolAgeOpt.isEmpty()) {
      return null;
    }

    final var smolAge = smolAgeOpt.get();
    final var suffix =
        "."
            + Optional.ofNullable(smolAge.getImage())
                .filter(f -> f.contains("."))
                .map(f -> f.substring(smolAge.getImage().lastIndexOf(".") + 1))
                .orElse("gif");

    final Path path = Paths.get("img_cache/smolage/", tokenId + suffix);
    if (path.toFile().exists()) {
      // Read
      try {
        byte[] bytes = Files.readAllBytes(path);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(bytes.length);
        byteArrayOutputStream.writeBytes(bytes);

        return byteArrayOutputStream;
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      }
    }

    try {
      String ipfsImage =
          smolAge.getImage().replace("ipfs://", "https://gateway.pinata.cloud/ipfs/");

      HttpClient httpClient = HttpClient.newBuilder().followRedirects(Redirect.ALWAYS).build();
      HttpRequest request = HttpRequest.newBuilder().GET().uri(new URI(ipfsImage)).build();

      final var response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

      // Add to cache
      log.info("Writing new cached object: " + path);
      Files.write(path, response.body());

      byte[] bytes = response.body();
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(bytes.length);
      byteArrayOutputStream.writeBytes(bytes);

      return byteArrayOutputStream;
    } catch (URISyntaxException | InterruptedException | IOException ex) {
      log.error("Exception on URI: " + tokenId, ex);
      return null;
    }
  }
}
