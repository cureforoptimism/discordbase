package com.cureforoptimism.discordbase.service;

import com.cureforoptimism.discordbase.Constants;
import com.cureforoptimism.discordbase.application.DiscordBot;
import com.cureforoptimism.discordbase.domain.SmolAgeSale;
import com.cureforoptimism.discordbase.repository.SmolAgeSaleRepository;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SalesService {
  private final SmolAgeSaleRepository smolAgeSaleRepository;
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
      SmolAgeSale lastPostedSale =
          smolAgeSaleRepository.findFirstByPostedIsTrueOrderByBlockTimestampDesc();

      if (lastPostedSale != null) {
        lastPostedBlockTimestamp =
            smolAgeSaleRepository
                .findFirstByPostedIsTrueOrderByBlockTimestampDesc()
                .getBlockTimestamp();
      }
    }

    List<SmolAgeSale> newSales =
        smolAgeSaleRepository.findByBlockTimestampIsAfterAndPostedIsFalseOrderByBlockTimestampAsc(
            lastPostedBlockTimestamp);
    if (!newSales.isEmpty()) {
      final Double ethMktPrice = marketPriceMessageSubscriber.getLastMarketPlace().getEthPrice();

      final NumberFormat decimalFormatZeroes = new DecimalFormat("#,###.00");
      final NumberFormat decimalFormatOptionalZeroes = new DecimalFormat("0.###");

      List<Long> channelList = new ArrayList<>();
      if (System.getenv("PROD") != null) {
        channelList.add(Constants.CHANNEL_SALES_BOT);
        channelList.add(Constants.CHANNEL_SALES_MILLIBOBS);
      }

      // Odd; additional channels don't get the image. Maybe need separate file uploads.
      channelList.add(Constants.CHANNEL_TEST_GENERAL);

      for (SmolAgeSale smolAgeSale : newSales) {
        final String ethValue = decimalFormatOptionalZeroes.format(smolAgeSale.getSalePrice());
        final String usdValue =
            decimalFormatZeroes.format(ethMktPrice * smolAgeSale.getSalePrice().doubleValue());

        final int tokenId = smolAgeSale.getTokenId();

        final var img = nftImageService.getImage(Integer.toString(tokenId));
        if (img == null) {
          continue;
        }

        ByteArrayOutputStream baos = nftImageService.getImage(Integer.toString(tokenId));
        byte[] bytes = baos.toByteArray();

        for (Long channelId : channelList) {
          final MessageCreateSpec messageCreateSpec =
              MessageCreateSpec.builder()
                  .addFile(
                      "smolage_" + tokenId + "_" + channelId + ".gif",
                      new ByteArrayInputStream(bytes))
                  .addEmbed(
                      EmbedCreateSpec.builder()
                          .title("**SOLD**")
                          .description("Neandersmol #" + tokenId)
                          .url(
                              "https://trove.treasure.lol/collection/neandersmols/"
                                  + smolAgeSale.getTokenId())
                          .addField("USD", "$" + usdValue, true)
                          .addField("ETH", "Ξ" + ethValue, true)
                          .image("attachment://smolage_" + tokenId + "_" + channelId + ".gif")
                          .timestamp(smolAgeSale.getBlockTimestamp().toInstant())
                          .build())
                  .build();

          discordBot.postMessage(messageCreateSpec, List.of(channelId));
        }

        smolAgeSale.setPosted(true);
        smolAgeSaleRepository.save(smolAgeSale);

        log.info("New sale posted for " + smolAgeSale.getTokenId());

        if (smolAgeSale.getBlockTimestamp().after(lastPostedBlockTimestamp)) {
          lastPostedBlockTimestamp = smolAgeSale.getBlockTimestamp();
        }
      }
    }
  }
}
