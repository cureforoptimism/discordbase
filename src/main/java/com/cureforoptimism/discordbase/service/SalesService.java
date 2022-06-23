package com.cureforoptimism.discordbase.service;

import com.cureforoptimism.discordbase.Constants;
import com.cureforoptimism.discordbase.Utilities;
import com.cureforoptimism.discordbase.application.DiscordBot;
import com.cureforoptimism.discordbase.domain.DonkList;
import com.cureforoptimism.discordbase.domain.DonkSale;
import com.cureforoptimism.discordbase.repository.DonkListRepository;
import com.cureforoptimism.discordbase.repository.DonkRarityRankRepository;
import com.cureforoptimism.discordbase.repository.DonkSaleRepository;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.Color;
import io.github.redouane59.twitter.TwitterClient;
import io.github.redouane59.twitter.dto.tweet.MediaCategory;
import io.github.redouane59.twitter.dto.tweet.TweetParameters;
import io.github.redouane59.twitter.dto.tweet.TweetParameters.Media;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
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
  private final MarketPriceMessageSubscriber marketPriceMessageSubscriber;
  private final DonkRarityRankRepository donkRarityRankRepository;
  private final DonkListRepository donkListRepository;
  private final DonkSaleRepository donkSaleRepository;
  private Date lastPostedBlockSalesTimestamp = null;
  private Date lastPostedBlockListingsTimestamp = null;
  private final DiscordBot discordBot;
  private final Utilities utilities;

  private final TwitterClient twitterClient;

  @Scheduled(fixedDelay = 30000, initialDelay = 10000)
  public synchronized void postNewMarketplaceActivities() {
    postNewSales();
    postNewListings();
  }

  public synchronized void postNewSales() {
    if (discordBot.getCurrentPrice() == null) {
      return;
    }
    // TODO: Perform this check, for sure, before posting tweets
    if (System.getenv("PROD") == null) {
      return;
    }

    if (lastPostedBlockSalesTimestamp == null) {
      DonkSale lastPostedDonkSale =
          donkSaleRepository.findFirstByPostedIsTrueOrderByBlockTimestampDesc();

      if (lastPostedDonkSale != null) {
        lastPostedBlockSalesTimestamp =
            donkSaleRepository
                .findFirstByPostedIsTrueOrderByBlockTimestampDesc()
                .getBlockTimestamp();
      }
    }

    // Can uncomment and replace with recent ID to test new functionality (delete existing tweet
    // first!)
    //        List<DonkSale> newSales =
    //
    // donkSaleRepository.findById("https://arbiscan.io/tx/0x1de03a7bb555289aeb82091e54f7b58a66e293747acaa9072ffc002bea8e0a0e").stream().toList();

    List<DonkSale> newSales =
        donkSaleRepository.findByBlockTimestampIsAfterAndPostedIsFalseOrderByBlockTimestampAsc(
            lastPostedBlockSalesTimestamp);
    if (!newSales.isEmpty()) {
      if (marketPriceMessageSubscriber.getLastMarketPlace() == null) {
        return;
      }

      final Double ethMktPrice = marketPriceMessageSubscriber.getLastMarketPlace().getEthPrice();
      final NumberFormat decimalFormatZeroes = new DecimalFormat("#,###.00");
      final NumberFormat decimalFormatOptionalZeroes = new DecimalFormat("0.###");
      Double currentPrice = discordBot.getCurrentPrice();

      List<Long> channelList = new ArrayList<>();
      if (System.getenv("PROD") != null) {
        channelList.add(Constants.CHANNEL_SALES_BOT);
      }

      // Odd; additional channels don't get the image. Maybe need separate file uploads.
      channelList.add(Constants.CHANNEL_TEST_GENERAL);

      for (DonkSale donkSale : newSales) {
        final BigDecimal usdPrice =
            donkSale.getSalePrice().multiply(BigDecimal.valueOf(currentPrice));
        final Double ethPrice = usdPrice.doubleValue() / ethMktPrice;
        final String ethValue = decimalFormatOptionalZeroes.format(ethPrice);
        final String usdValue = decimalFormatZeroes.format(usdPrice);

        final int adjustedTokenId = donkSale.getTokenId() + 1;

        final var imgOpt = utilities.getDonkBufferedImage(Integer.toString(adjustedTokenId));
        if (imgOpt.isEmpty()) {
          return;
        }

        final var img = imgOpt.get();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
          ImageIO.write(img, "png", baos);
        } catch (IOException e) {
          e.printStackTrace();
          return;
        }

        final var rarityRank =
            donkRarityRankRepository.findByDonkId((long) adjustedTokenId).getRank();

        byte[] bytes = baos.toByteArray();
        final var mediaResponse =
            twitterClient.uploadMedia(
                donkSale.getTokenId() + "_tld.png", bytes, MediaCategory.TWEET_IMAGE);
        final var media = Media.builder().mediaIds(List.of(mediaResponse.getMediaId())).build();
        TweetParameters tweetParameters =
            TweetParameters.builder()
                .media(media)
                .text(
                    "The Lost Donkeys #"
                        + adjustedTokenId
                        + " (Rarity Rank #"
                        + rarityRank
                        + ")\nSold for\nMAGIC: "
                        + decimalFormatOptionalZeroes.format(donkSale.getSalePrice())
                        + "\nUSD: $"
                        + usdValue
                        + "\nETH: "
                        + ethValue
                        + "\n\n"
                        + "https://marketplace.treasure.lol/collection/the-lost-donkeys/"
                        + donkSale.getTokenId()
                        + "\n\n"
                        + "#thelostdonkeys #treasuredao")
                .build();

        twitterClient.postTweet(tweetParameters);

        final MessageCreateSpec messageCreateSpec =
            MessageCreateSpec.builder()
                .addFile("tld_" + adjustedTokenId + ".png", new ByteArrayInputStream(bytes))
                .addEmbed(
                    EmbedCreateSpec.builder()
                        .description(
                            "**SOLD**\nThe Lost Donkeys #"
                                + adjustedTokenId
                                + " (Rarity Rank **#"
                                + rarityRank
                                + "**)")
                        .addField(
                            "MAGIC",
                            decimalFormatOptionalZeroes.format(donkSale.getSalePrice()),
                            true)
                        .addField("USD", "$" + usdValue, true)
                        .addField("ETH", "Ξ" + ethValue, true)
                        .image("attachment://tld_" + adjustedTokenId + ".png")
                        .timestamp(donkSale.getBlockTimestamp().toInstant())
                        .build())
                .build();

        discordBot.postMessage(messageCreateSpec, channelList);

        donkSale.setPosted(true);
        donkSaleRepository.save(donkSale);

        log.info("New donk sale posted for " + donkSale.getTokenId());

        if (donkSale.getBlockTimestamp().after(lastPostedBlockSalesTimestamp)) {
          lastPostedBlockSalesTimestamp = donkSale.getBlockTimestamp();
        }
      }
    }
  }

  public synchronized void postNewListings() {
    if (discordBot.getCurrentPrice() == null) {
      return;
    }

    if (lastPostedBlockListingsTimestamp == null) {
      DonkList lastPostedDonkListing =
          donkListRepository.findFirstByPostedIsTrueOrderByBlockTimestampDesc();

      if (lastPostedDonkListing != null) {
        lastPostedBlockListingsTimestamp =
            donkListRepository
                .findFirstByPostedIsTrueOrderByBlockTimestampDesc()
                .getBlockTimestamp();
      }
    }

    List<DonkList> newListings =
        donkListRepository.findByBlockTimestampIsAfterAndPostedIsFalseOrderByBlockTimestampAsc(
            lastPostedBlockListingsTimestamp);
    if (!newListings.isEmpty()) {
      if (marketPriceMessageSubscriber.getLastMarketPlace() == null) {
        return;
      }

      final NumberFormat decimalFormatZeroes = new DecimalFormat("#,###.00");
      final NumberFormat decimalFormatOptionalZeroes = new DecimalFormat("0.###");
      Double currentPrice = discordBot.getCurrentPrice();

      List<Long> channelList = new ArrayList<>();
      if (System.getenv("PROD") != null) {
        channelList.add(Constants.CHANNEL_LIST_BOT);
      }

      // Odd; additional channels don't get the image. Maybe need separate file uploads.
      channelList.add(Constants.CHANNEL_TEST_GENERAL);

      for (DonkList donkListing : newListings) {
        final Double ethMktPrice = marketPriceMessageSubscriber.getLastMarketPlace().getEthPrice();
        final BigDecimal usdPrice =
            donkListing.getSalePrice().multiply(BigDecimal.valueOf(currentPrice));
        final Double ethPrice = usdPrice.doubleValue() / ethMktPrice;
        final String ethValue = decimalFormatOptionalZeroes.format(ethPrice);
        final String usdValue = decimalFormatZeroes.format(usdPrice);

        final int adjustedTokenId = donkListing.getTokenId() + 1;

        final var imgOpt = utilities.getDonkBufferedImage(Integer.toString(adjustedTokenId));
        if (imgOpt.isEmpty()) {
          return;
        }

        final var img = imgOpt.get();
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
                .addFile("tld_" + adjustedTokenId + ".png", new ByteArrayInputStream(bytes))
                .addEmbed(
                    EmbedCreateSpec.builder()
                        .color(Color.BLUE)
                        .title("LISTED")
                        .url(
                            "https://marketplace.treasure.lol/collection/the-lost-donkeys/"
                                + donkListing.getTokenId())
                        .description(
                            "The Lost Donkeys #"
                                + adjustedTokenId
                                + " (Rarity Rank **#"
                                + donkRarityRankRepository
                                    .findByDonkId((long) adjustedTokenId)
                                    .getRank()
                                + "**)")
                        .addField(
                            "MAGIC",
                            decimalFormatOptionalZeroes.format(donkListing.getSalePrice()),
                            true)
                        .addField("USD", "$" + usdValue, true)
                        .addField("ETH", "Ξ" + ethValue, true)
                        .image("attachment://tld_" + adjustedTokenId + ".png")
                        .timestamp(donkListing.getBlockTimestamp().toInstant())
                        .build())
                .build();

        discordBot.postMessage(messageCreateSpec, channelList);

        donkListing.setPosted(true);
        donkListRepository.save(donkListing);

        log.info("New donk list posted for " + donkListing.getTokenId());

        if (donkListing.getBlockTimestamp().after(lastPostedBlockListingsTimestamp)) {
          lastPostedBlockListingsTimestamp = donkListing.getBlockTimestamp();
        }
      }
    }
  }
}
