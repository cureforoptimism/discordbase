package com.cureforoptimism.discordbase;

import com.cureforoptimism.discordbase.service.TokenService;
import com.litesoftwares.coingecko.CoinGeckoApiClient;
import com.litesoftwares.coingecko.impl.CoinGeckoApiClientImpl;
import io.github.redouane59.twitter.TwitterClient;
import io.github.redouane59.twitter.signature.TwitterCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

@Slf4j
@Configuration
@EnableScheduling
@EnableTransactionManagement
@RequiredArgsConstructor
public class SpringConfiguration {
  private final TokenService tokenService;

  @Bean
  public CoinGeckoApiClient coinGeckoApiClient() {
    return new CoinGeckoApiClientImpl();
  }

  @Bean
  public Web3j web3j() {
    return Web3j.build(new HttpService("https://arb1.arbitrum.io/rpc"));
  }

  @Bean
  public TwitterClient twitterClient() {
    return new TwitterClient(
        TwitterCredentials.builder()
            .accessToken(tokenService.getTwitterApiToken())
            .accessTokenSecret(tokenService.getTwitterApiTokenSecret())
            .bearerToken(tokenService.getTwitterApiBearerToken())
            .apiKey(tokenService.getTwitterApiKey())
            .apiSecretKey(tokenService.getTwitterApiSecret())
            .build());
  }
}
