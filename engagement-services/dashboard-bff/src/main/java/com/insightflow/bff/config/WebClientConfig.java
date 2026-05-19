package com.insightflow.bff.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Value("${app.timeout.connect-ms:5000}")
    private int connectTimeoutMs;

    @Value("${app.timeout.read-ms:10000}")
    private int readTimeoutMs;

    @Value("${app.downstream.catalog-base-url:lb://catalog-service}")
    private String catalogBaseUrl;

    @Value("${app.downstream.sales-base-url:lb://sales-service}")
    private String salesBaseUrl;

    @Value("${app.downstream.ml-base-url:http://localhost:8000}")
    private String mlBaseUrl;

    private HttpClient buildHttpClient() {
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .responseTimeout(Duration.ofMillis(readTimeoutMs))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(readTimeoutMs, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(connectTimeoutMs, TimeUnit.MILLISECONDS)));
    }

    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(buildHttpClient()));
    }

    @Bean("catalogClient")
    public WebClient catalogClient(WebClient.Builder loadBalancedWebClientBuilder) {
        return loadBalancedWebClientBuilder
                .baseUrl(catalogBaseUrl)
                .build();
    }

    @Bean("salesClient")
    public WebClient salesClient(WebClient.Builder loadBalancedWebClientBuilder) {
        return loadBalancedWebClientBuilder
                .baseUrl(salesBaseUrl)
                .build();
    }

    @Bean("mlClient")
    public WebClient mlClient() {
        // ml-service is Python, not in Eureka — use direct HTTP URL
        return WebClient.builder()
                .baseUrl(mlBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(buildHttpClient()))
                .build();
    }
}
