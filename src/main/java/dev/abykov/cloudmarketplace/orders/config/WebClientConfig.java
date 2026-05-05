package dev.abykov.cloudmarketplace.orders.config;

import dev.abykov.cloudmarketplace.orders.config.props.OrderServiceProps;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@RequiredArgsConstructor
@Configuration
public class WebClientConfig {

    private final OrderServiceProps props;

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl(props.getMenuServiceUrl())
                .build();
    }
}
