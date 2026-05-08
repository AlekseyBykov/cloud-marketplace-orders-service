package dev.abykov.cloudmarketplace.orders.client;

import dev.abykov.cloudmarketplace.orders.config.props.OrderServiceProps;
import dev.abykov.cloudmarketplace.orders.dto.external.MenuInfo;
import dev.abykov.cloudmarketplace.orders.dto.external.OrderMenuRequest;
import dev.abykov.cloudmarketplace.orders.dto.external.OrderMenuResponse;
import dev.abykov.cloudmarketplace.orders.testdata.TestDataProvider;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration-style test for {@link MenuClient}.
 *
 * <p>Uses MockWebServer to simulate Menu Service and verify HTTP interaction behavior.</p>
 *
 * <p>This test covers:
 * <ul>
 *     <li>Correct HTTP request construction (method, path)</li>
 *     <li>Retry behavior on transient failures (5xx responses)</li>
 *     <li>Timeout handling for slow responses</li>
 *     <li>Proper deserialization of JSON responses into DTOs</li>
 * </ul>
 *
 * <p>The real Menu Service is not used. Instead, MockWebServer acts as a controllable
 * test double that allows simulating various network scenarios.</p>
 *
 * <p>No Spring context is started — the client is tested in isolation for speed
 * and deterministic behavior.</p>
 */
class MenuClientTest {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(1);
    private static final Duration RETRY_BACKOFF = Duration.ofMillis(10);
    private static final int RETRY_COUNT = 3;
    private static final double RETRY_JITTER = 0.75;

    private final OrderServiceProps props = new OrderServiceProps(
            "http://localhost:9091",
            "/api/menu-items/resolve",

            DEFAULT_TIMEOUT,
            RETRY_BACKOFF,
            RETRY_COUNT,
            RETRY_JITTER
    );

    private MenuClient menuClient;
    private MockWebServer mockWebServer;

    /**
     * Sets up a mock HTTP server and initializes MenuClient for testing.
     *
     * <p>MockWebServer is used to simulate Menu Service behavior without making real network calls.</p>
     *
     * <p>WebClient is configured to use the mock server's base URL,
     * so all outgoing HTTP requests are intercepted and controlled within the test.</p>
     *
     * <p>This allows testing retry, timeout, and error-handling logic in isolation,
     * without starting a full Spring context.</p>
     */
    @BeforeEach
    void setup() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        var webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        menuClient = new MenuClient(webClient, props);
    }

    /**
     * Shuts down the mock HTTP server after each test.
     *
     * <p>Ensures that resources (ports, threads) are released and tests remain isolated.</p>
     */
    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    /**
     * Verifies that MenuClient correctly handles transient failures using retry and timeout logic.
     *
     * <p>Simulates the following interaction with Menu Service:
     * <ul>
     *     <li>1st request → HTTP 503 (service unavailable)</li>
     *     <li>2nd request → delayed response (timeout)</li>
     *     <li>3rd request → successful response</li>
     * </ul>
     *
     * <p>Expected behavior:
     * <ul>
     *     <li>Client retries on 5xx errors and timeouts</li>
     *     <li>Timeout is applied per retry attempt</li>
     *     <li>Final successful response is returned</li>
     *     <li>Total number of HTTP calls equals number of attempts</li>
     * </ul>
     */
    @Test
    void getMenuInfo_returnsInfo_whenRetriesSucceed() throws Exception {
        // given: Menu Service fails, then times out, then succeeds
        mockServiceUnavailable();
        mockDelayedResponse();
        mockSuccessfulResponse();

        // when: calling MenuClient
        Mono<OrderMenuResponse> response = menuClient.getMenuInfo(
                new OrderMenuRequest(List.of("One", "Two", "Three"))
        );

        // then: response is correct and retries were performed
        assertResponseCorrect(response);
        verifyRequests(3);
    }

    @Test
    void getMenuInfo_returnsInfo_whenAllIsOk() throws Exception {
        mockWebServer.enqueue(TestDataProvider.partialSuccessResponse());

        Mono<OrderMenuResponse> response = menuClient.getMenuInfo(
                new OrderMenuRequest(List.of("One", "Two", "Three"))
        );

        assertResponseCorrect(response);
        verifyRequests(1);
    }

    private void assertResponseCorrect(Mono<OrderMenuResponse> response) {
        StepVerifier.create(response)
                .expectNextMatches(result -> {

                    List<MenuInfo> items = result.getItems();
                    items.sort(Comparator.comparing(MenuInfo::getName));

                    assertThat(items)
                            .map(MenuInfo::getName)
                            .containsExactly("One", "Three", "Two");

                    assertThat(items)
                            .map(MenuInfo::getPrice)
                            .containsExactly(
                                    BigDecimal.valueOf(10.1),
                                    BigDecimal.valueOf(30.3),
                                    null
                            );

                    assertThat(items)
                            .map(MenuInfo::isAvailable)
                            .containsExactly(true, true, false);

                    return true;
                })
                .verifyComplete();
    }

    private void verifyRequests(int times) throws Exception {
        for (int i = 0; i < times; i++) {
            RecordedRequest req = mockWebServer.takeRequest(1000, TimeUnit.MILLISECONDS);

            assertThat(req).isNotNull();
            assertThat(req.getMethod()).isEqualTo("POST");
            assertThat(req.getPath()).isEqualTo(props.getMenuInfoPath());
        }

        assertThat(mockWebServer.takeRequest(1000, TimeUnit.MILLISECONDS)).isNull();
    }

    private void mockServiceUnavailable() {
        mockWebServer.enqueue(
                new MockResponse().setResponseCode(HttpStatus.SERVICE_UNAVAILABLE.value())
        );
    }

    private void mockDelayedResponse() {
        mockWebServer.enqueue(
                TestDataProvider.partialSuccessResponse()
                        .setBodyDelay(1500, TimeUnit.MILLISECONDS)
        );
    }

    private void mockSuccessfulResponse() {
        mockWebServer.enqueue(
                TestDataProvider.partialSuccessResponse()
        );
    }
}
