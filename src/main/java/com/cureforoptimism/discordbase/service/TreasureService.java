package com.cureforoptimism.discordbase.service;

import com.cureforoptimism.discordbase.domain.DonkList;
import com.cureforoptimism.discordbase.domain.DonkSale;
import com.cureforoptimism.discordbase.domain.MarketType;
import com.cureforoptimism.discordbase.repository.DonkListRepository;
import com.cureforoptimism.discordbase.repository.DonkSaleRepository;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TreasureService {
  private final DonkSaleRepository donkSaleRepository;
  private final DonkListRepository donkListRepository;

  @Scheduled(fixedDelay = 60000)
  public synchronized void updateLatestMarketplace() {
    update(MarketType.LIST);
    update(MarketType.SALE);
  }

  private void update(MarketType marketType) {
    String status =
        switch (marketType) {
          case LIST -> "Active";
          case SALE -> "Sold";
        };

    String jsonBody =
        "{\"query\":\"query getActivity($id: String!, $orderBy: Listing_orderBy!) {\\n  listings(\\n    where: {status: "
            + status
            + ", collection: $id}\\n    orderBy: $orderBy\\n    orderDirection: desc\\n  ) {\\n    ...ListingFields\\n  }\\n}\\n\\nfragment ListingFields on Listing {\\n  blockTimestamp\\n  buyer {\\n    id\\n  }\\n  id\\n  pricePerItem\\n  quantity\\n  seller {\\n    id\\n  }\\n  token {\\n    id\\n    tokenId\\n  }\\n  collection {\\n    id\\n  }\\n  transactionLink\\n}\",\"variables\":{\"id\":\"0x5e84c1a06e6ad1a8ed66bc48dbe5eb06bf2fe4aa\",\"orderBy\":\"blockTimestamp\"},\"operationName\":\"getActivity\"}";
    try {
      HttpClient httpClient = HttpClient.newHttpClient();
      HttpRequest request =
          HttpRequest.newBuilder(
                  new URI("https://api.thegraph.com/subgraphs/name/treasureproject/marketplace"))
              .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
              .header("Content-Type", "application/json")
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      JSONObject obj = new JSONObject(response.body()).getJSONObject("data");
      JSONArray listings = obj.getJSONArray("listings");

      MathContext mc = new MathContext(10, RoundingMode.HALF_UP);

      for (int x = 0; x < listings.length(); x++) {
        final var listing = listings.getJSONObject(x);

        switch (marketType) {
          case LIST -> {
            int tokenId = listing.getJSONObject("token").getInt("tokenId");
            BigDecimal pricePerItem = new BigDecimal(listing.getBigInteger("pricePerItem"), 18, mc);
            Date blockTimeStamp = new Date(listing.getLong("blockTimestamp") * 1000);

            if (donkListRepository.existsByTokenIdAndBlockTimestampAndSalePrice(
                tokenId, blockTimeStamp, pricePerItem)) {
              continue;
            }

            donkListRepository.save(
                DonkList.builder()
                    .tokenId(tokenId)
                    .salePrice(pricePerItem)
                    .blockTimestamp(blockTimeStamp)
                    .posted(false)
                    .build());
          }
          case SALE -> {
            String transactionId = listing.getString("transactionLink");

            if (!donkSaleRepository.existsById(transactionId)) {
              int tokenId = listing.getJSONObject("token").getInt("tokenId");
              BigDecimal pricePerItem =
                  new BigDecimal(listing.getBigInteger("pricePerItem"), 18, mc);
              Date blockTimeStamp = new Date(listing.getLong("blockTimestamp") * 1000);

              donkSaleRepository.save(
                  DonkSale.builder()
                      .id(transactionId)
                      .tokenId(tokenId)
                      .salePrice(pricePerItem)
                      .blockTimestamp(blockTimeStamp)
                      .posted(false)
                      .build());
            }
          }
        }
      }
    } catch (Exception ex) {
      log.error("Exception updating latest sales", ex);
    }
  }
}
