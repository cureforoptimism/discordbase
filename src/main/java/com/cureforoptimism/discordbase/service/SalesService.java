package com.cureforoptimism.discordbase.service;

import com.cureforoptimism.discordbase.Constants;
import com.cureforoptimism.discordbase.application.DiscordBot;
import com.cureforoptimism.discordbase.domain.BirdsSale;
import com.cureforoptimism.discordbase.repository.BirdsSaleRepository;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SalesService {
  private final BirdsSaleRepository birdsSaleRepository;
  private final MarketPriceMessageSubscriber marketPriceMessageSubscriber;
  private Date lastPostedBlockTimestamp = null;
  private final DiscordBot discordBot;

  private final NftImageService nftImageService;

  @Scheduled(fixedDelay = 30000, initialDelay = 10000)
  public synchronized void postNewSales() {
    if (marketPriceMessageSubscriber.getLastMarketPlace() == null) {
      return;
    }

    if (lastPostedBlockTimestamp == null) {
      BirdsSale lastPostedSale =
          birdsSaleRepository.findFirstByPostedIsTrueOrderByBlockTimestampDesc();

      if (lastPostedSale != null) {
        lastPostedBlockTimestamp =
            birdsSaleRepository
                .findFirstByPostedIsTrueOrderByBlockTimestampDesc()
                .getBlockTimestamp();
      }
    }

    List<BirdsSale> newSales =
        birdsSaleRepository.findByBlockTimestampIsAfterAndPostedIsFalseOrderByBlockTimestampAsc(
            lastPostedBlockTimestamp);
    if (!newSales.isEmpty()) {
      final Double ethMktPrice = marketPriceMessageSubscriber.getLastMarketPlace().getEthPrice();

      final NumberFormat decimalFormatZeroes = new DecimalFormat("#,###.00");
      final NumberFormat decimalFormatOptionalZeroes = new DecimalFormat("0.##");

      List<Long> channelList = new ArrayList<>();
      if (System.getenv("PROD") != null) {
        channelList.add(Constants.CHANNEL_SALES_BOT);
      }

      // Odd; additional channels don't get the image. Maybe need separate file uploads.
      channelList.add(Constants.CHANNEL_TEST_GENERAL);

      for (BirdsSale birdsSale : newSales) {
        final String ethValue = decimalFormatOptionalZeroes.format(birdsSale.getSalePrice());
        final String usdValue =
            decimalFormatZeroes.format(ethMktPrice * birdsSale.getSalePrice().doubleValue());

        final int tokenId = birdsSale.getTokenId();

        final var img = nftImageService.getImage(Integer.toString(tokenId));
        if (img == null) {
          continue;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
          ImageIO.write(img, "png", baos);
        } catch (IOException e) {
          e.printStackTrace();
          return;
        }

        byte[] bytes = baos.toByteArray();
        final MessageCreateSpec messageCreateSpec =
            MessageCreateSpec.builder()
                .addFile("birds_" + tokenId + ".png", new ByteArrayInputStream(bytes))
                .addEmbed(
                    EmbedCreateSpec.builder()
                        .title("**SOLD**")
                        .description("Birds & Blades #" + tokenId)
                        .url(
                            "https://trove.treasure.lol/collection/birds-n-blades/"
                                + birdsSale.getTokenId())
                        .addField("USD", "$" + usdValue, true)
                        .addField("ETH", "Ξ" + ethValue, true)
                        .image("attachment://birds_" + tokenId + ".png")
                        .timestamp(birdsSale.getBlockTimestamp().toInstant())
                        .build())
                .build();

        discordBot.postMessage(messageCreateSpec, channelList);

        birdsSale.setPosted(true);
        birdsSaleRepository.save(birdsSale);

        log.info("New sale posted for " + birdsSale.getTokenId());

        if (birdsSale.getBlockTimestamp().after(lastPostedBlockTimestamp)) {
          lastPostedBlockTimestamp = birdsSale.getBlockTimestamp();
        }
      }
    }
  }
}
