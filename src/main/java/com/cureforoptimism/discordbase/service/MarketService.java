package com.cureforoptimism.discordbase.service;

import com.cureforoptimism.discordbase.Constants;
import com.cureforoptimism.discordbase.domain.BirdsSale;
import com.cureforoptimism.discordbase.repository.BirdsSaleRepository;
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
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MarketService {
  private final BirdsSaleRepository birdsSaleRepository;

  @Scheduled(fixedDelay = 60000)
  public synchronized void updateLatestSales() {
    String jsonBody =
        "{\"query\":\"query getActivity($first: Int!, $skip: Int, $includeListings: Boolean!, $includeSales: Boolean!, $includeBids: Boolean!, $listingFilter: Listing_filter, $listingOrderBy: Listing_orderBy, $bidFilter: Bid_filter, $bidOrderBy: Bid_orderBy, $saleFilter: Sale_filter, $saleOrderBy: Sale_orderBy, $orderDirection: OrderDirection) {\\n  listings(\\n    first: $first\\n    where: $listingFilter\\n    orderBy: $listingOrderBy\\n    orderDirection: $orderDirection\\n    skip: $skip\\n  ) @include(if: $includeListings) {\\n    ...ListingFields\\n  }\\n  bids(\\n    first: $first\\n    where: $bidFilter\\n    orderBy: $bidOrderBy\\n    orderDirection: $orderDirection\\n    skip: $skip\\n  ) @include(if: $includeBids) {\\n    ...BidFields\\n  }\\n  sales(\\n    first: $first\\n    where: $saleFilter\\n    orderBy: $saleOrderBy\\n    orderDirection: $orderDirection\\n    skip: $skip\\n  ) @include(if: $includeSales) {\\n    ...SaleFields\\n  }\\n}\\n\\nfragment ListingFields on Listing {\\n  timestamp\\n  id\\n  pricePerItem\\n  quantity\\n  seller {\\n    id\\n  }\\n  token {\\n    id\\n    tokenId\\n  }\\n  collection {\\n    id\\n  }\\n  currency {\\n    id\\n  }\\n  status\\n  expiresAt\\n}\\n\\nfragment BidFields on Bid {\\n  timestamp\\n  id\\n  pricePerItem\\n  quantity\\n  token {\\n    id\\n    tokenId\\n  }\\n  collection {\\n    id\\n  }\\n  currency {\\n    id\\n  }\\n  buyer {\\n    id\\n  }\\n  status\\n  expiresAt\\n  bidType\\n}\\n\\nfragment SaleFields on Sale {\\n  timestamp\\n  id\\n  pricePerItem\\n  quantity\\n  type\\n  seller {\\n    id\\n  }\\n  buyer {\\n    id\\n  }\\n  token {\\n    id\\n    tokenId\\n  }\\n  collection {\\n    id\\n  }\\n  currency {\\n    id\\n  }\\n}\",\"variables\":{\"skip\":0,\"first\":100,\"listingOrderBy\":\"timestamp\",\"saleOrderBy\":\"timestamp\",\"bidOrderBy\":\"timestamp\",\"listingFilter\":{\"collection\":\""
            + Constants.BIRDS_CONTRACT_ID.toLowerCase()
            + "\",\"status\":\"ACTIVE\"},\"bidFilter\":{\"collection\":\"0x4c1054dff878614a30164222625b1d1bfd819873\",\"status\":\"ACTIVE\"},\"saleFilter\":{\"collection\":\""
            + Constants.BIRDS_CONTRACT_ID.toLowerCase()
            + "\"},\"orderDirection\":\"desc\",\"includeListings\":false,\"includeSales\":true,\"includeBids\":false},\"operationName\":\"getActivity\"}";

    try {
      String marketUrl =
          "https://api.thegraph.com/subgraphs/name/vinnytreasure/treasuremarketplace-fast-prod";

      HttpClient httpClient = HttpClient.newHttpClient();
      HttpRequest request =
          HttpRequest.newBuilder(new URI(marketUrl))
              .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
              .header("Content-Type", "application/json")
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      JSONObject obj = new JSONObject(response.body()).getJSONObject("data");
      JSONArray sales = obj.getJSONArray("sales");

      MathContext mc = new MathContext(10, RoundingMode.HALF_UP);

      for (int x = 0; x < sales.length(); x++) {
        final var listing = sales.getJSONObject(x);

        // ID is <user-id>-<collection>-<token-id>-<tx>
        String id = listing.getString("id");
        String transactionId = "https://arbiscan.io/tx/" + id.split("-")[3];
        int tokenId = listing.getJSONObject("token").getInt("tokenId");

        if (!birdsSaleRepository.existsByTxAndTokenId(transactionId, tokenId)) {
          BigDecimal pricePerItem = new BigDecimal(listing.getBigInteger("pricePerItem"), 18, mc);
          Date blockTimeStamp = new Date(listing.getLong("timestamp") * 1000);

          birdsSaleRepository.save(
              BirdsSale.builder()
                  .tx(transactionId)
                  .tokenId(tokenId)
                  .salePrice(pricePerItem)
                  .blockTimestamp(blockTimeStamp)
                  .posted(false)
                  .build());
        }
      }
    } catch (Exception ex) {
      log.error("Exception updating latest sales", ex);
    }
  }
}
