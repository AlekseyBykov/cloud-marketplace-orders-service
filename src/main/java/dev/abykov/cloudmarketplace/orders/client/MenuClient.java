package dev.abykov.cloudmarketplace.orders.client;

import dev.abykov.cloudmarketplace.orders.config.props.OrderServiceProps;
import dev.abykov.cloudmarketplace.orders.dto.external.OrderMenuRequest;
import dev.abykov.cloudmarketplace.orders.dto.external.OrderMenuResponse;
import dev.abykov.cloudmarketplace.orders.exception.OrderServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
public class MenuClient {

    private final WebClient webClient;
    private final OrderServiceProps props;

    /**
     * Fetches menu items info (price + availability) from Menu Service.
     * <p>
     * This method is fully non-blocking and resilient:
     * - applies timeout per request attempt
     * - retries only on transient errors (5xx, timeout)
     * - uses exponential backoff with jitter
     */
    public Mono<OrderMenuResponse> getMenuInfo(OrderMenuRequest request) {
        return webClient
                .post()
                .uri(props.getMenuInfoPath())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::is5xxServerError, this::map5xxToException)
                .bodyToMono(OrderMenuResponse.class)
                .transform(this::applyTimeout)
                .transform(this::applyRetryPolicy);
    }

    /**
     * Maps 5xx responses from Menu Service to a domain-specific exception.
     */
    private Mono<? extends Throwable> map5xxToException(ClientResponse response) {
        return Mono.error(new OrderServiceException(
                "Menu Service Unavailable",
                HttpStatus.SERVICE_UNAVAILABLE
        ));
    }

    /**
     * Applies timeout to each request attempt.
     * <p>
     * Important:
     * Timeout is applied BEFORE retry, so each retry has its own timeout window.
     */
    private <T> Mono<T> applyTimeout(Mono<T> mono) {
        return mono.timeout(props.getDefaultTimeout());
    }

    /**
     * Applies retry policy for transient failures.
     * <p>
     * Retries are triggered ONLY for:
     * - OrderServiceException (mapped 5xx errors)
     * - TimeoutException
     * <p>
     * Uses exponential backoff + jitter to avoid synchronized retries across instances.
     */
    private <T> Mono<T> applyRetryPolicy(Mono<T> mono) {
        return mono.retryWhen(
                Retry.backoff(props.getRetryCount(), props.getRetryBackoff())
                        .jitter(props.getRetryJitter())
                        .filter(this::isRetryableError)
                        .onRetryExhaustedThrow((spec, signal) -> {
                            throw new OrderServiceException(
                                    "Failed to fetch info from Menu Service after max retry attempts",
                                    HttpStatus.SERVICE_UNAVAILABLE
                            );
                        })
        );
    }

    /**
     * Defines which errors are considered retryable.
     */
    private boolean isRetryableError(Throwable t) {
        return t instanceof OrderServiceException || t instanceof TimeoutException;
    }
}
