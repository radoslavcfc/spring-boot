package com.farm.workers.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║  .NET → Java: HTTP Client Configuration                  ║
 * ║  IHttpClientFactory / named HttpClient  →  WebClient @Bean║
 * ║  services.AddHttpClient("functions", c => {             ║
 * ║    c.Timeout = TimeSpan.FromSeconds(30);                ║
 * ║  });                                                     ║
 * ║  ──────────── IS EQUIVALENT TO ────────────              ║
 * ║  @Bean WebClient webClient() { ... }                    ║
 * ╚══════════════════════════════════════════════════════════╝
 */
@Slf4j
@Configuration
public class WebClientConfig {

    /**
     * Configure WebClient with timeouts and logging
     * This @Bean is injected into AzureFunctionService
     * ≈ registering a named HttpClient in .NET DI
     */
    @Bean
    public WebClient webClient() {
        // Reactor Netty HTTP client (underlying transport)
        HttpClient httpClient = HttpClient.create()
                // Connection timeout  ≈  HttpClientHandler.ConnectTimeout
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                .responseTimeout(Duration.ofSeconds(30))
                .doOnConnected(conn ->
                    conn.addHandlerLast(new ReadTimeoutHandler(30, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS))
                );

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                // Request/response logging filter  ≈  DelegatingHandler in .NET
                .filter(logRequest())
                .filter(logResponse())
                // Default headers for all requests
                .defaultHeader("Accept", "application/json")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * Logging filter for outbound requests
     * ≈ a DelegatingHandler / HttpMessageHandler in .NET
     */
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            log.debug("HTTP Request: {} {}", request.method(), request.url());
            return Mono.just(request);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            log.debug("HTTP Response status: {}", response.statusCode());
            return Mono.just(response);
        });
    }
}
