package dev.abykov.cloudmarketplace.orders.controller;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import dev.abykov.cloudmarketplace.orders.dto.api.CreateOrderRequest;
import dev.abykov.cloudmarketplace.orders.dto.api.OrderResponse;
import dev.abykov.cloudmarketplace.orders.entity.OrderStatus;
import dev.abykov.cloudmarketplace.orders.testdata.TestDataProvider;
import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Comparator;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static dev.abykov.cloudmarketplace.orders.controller.OrderController.USER_HEADER;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full integration test for {@link OrderController}.
 *
 * <p>This test verifies the complete HTTP request flow:</p>
 *
 * <pre>
 * WebTestClient
 *      ↓ HTTP
 * OrderController
 *      ↓
 * OrderService
 *      ↓
 * Repository
 *      ↓
 * PostgreSQL (Testcontainers)
 * </pre>
 *
 * <p>External HTTP calls to Menu Service are replaced with WireMock.</p>
 *
 * <p>Test scope includes:</p>
 * <ul>
 *     <li>HTTP request/response handling</li>
 *     <li>Request validation</li>
 *     <li>Exception handling</li>
 *     <li>Reactive service flow</li>
 *     <li>Database interaction</li>
 *     <li>External API integration behavior</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureWebTestClient(timeout = "20000")
class OrderControllerTest {

    private static final String BASE_URL = "/v1/orders";
    private static final String MENU_INFO_PATH =
            "/v1/menu-items/menu-info";

    private static final String USERNAME = "username1";

    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private ConnectionFactory connectionFactory;

    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16.1")
                    .withDatabaseName("test_database")
                    .withUsername("user")
                    .withPassword("password");

    @RegisterExtension
    static WireMockExtension wiremock =
            WireMockExtension.newInstance()
                    .options(wireMockConfig().dynamicPort())
                    .build();

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.r2dbc.url",
                () -> "r2dbc:postgresql://"
                        + postgres.getHost()
                        + ":"
                        + postgres.getFirstMappedPort()
                        + "/test_database"
        );

        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);

        /*
         * Overrides external API configuration for integration tests.
         *
         * <p>Instead of calling a real Menu Service instance,
         * the application is redirected to WireMock.</p>
         *
         * <p>This allows the test to fully control external HTTP responses:
         * status codes, response body, delays and error scenarios.</p>
         */
        registry.add("external.menu-service-url", wiremock::baseUrl);
        registry.add("external.default-timeout", () -> "1s");
    }

    @BeforeEach
    void setupDatabase(
            @Value("classpath:sql/insert-orders.sql") Resource script
    ) {
        executeSql(script);
    }

    @AfterEach
    void cleanupDatabase(
            @Value("classpath:sql/delete-orders.sql") Resource script
    ) {
        executeSql(script);
    }

    @Test
    void shouldCreateOrder() {
        stubMenuServiceSuccess();

        CreateOrderRequest request = TestDataProvider.createOrderRequest();

        webTestClient.post()
                .uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header(USER_HEADER, USERNAME)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(OrderResponse.class)
                .value(response -> {
                    assertThat(response.getOrderId())
                            .isNotNull();

                    assertThat(response.getStatus())
                            .isEqualTo(OrderStatus.CREATED);

                    assertThat(response.getOrderLineItems())
                            .isEqualTo(TestDataProvider.createdItems());
                });

        verifyMenuServiceCalled();
    }

    @Test
    void shouldReturnNotFoundWhenMenuItemUnavailable() {
        stubMenuServicePartialResponse();

        webTestClient.post()
                .uri(BASE_URL)
                .header(USER_HEADER, USERNAME)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(TestDataProvider.createOrderRequest())
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void shouldReturnOrdersSortedByDate() {
        webTestClient.get()
                .uri(BASE_URL + "?from=0&size=10&sortBy=DATE_ASC")
                .header(USER_HEADER, USERNAME)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(OrderResponse.class)
                .value(orders ->
                        assertThat(orders)
                                .hasSize(3)
                                .isSortedAccordingTo(
                                        Comparator.comparing(
                                                OrderResponse::getCreatedAt
                                        )
                                )
                );
    }

    @Test
    void shouldReturnBadRequestForInvalidOrder() {
        webTestClient.post()
                .uri(BASE_URL)
                .header(USER_HEADER, USERNAME)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(TestDataProvider.createInvalidOrderRequest())
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    private void stubMenuServiceSuccess() {
        wiremock.stubFor(
                post(MENU_INFO_PATH)
                        .willReturn(
                                okJson(TestDataProvider.readSuccessfulResponse())
                        )
        );
    }

    private void stubMenuServicePartialResponse() {
        wiremock.stubFor(
                post(MENU_INFO_PATH)
                        .willReturn(
                                okJson(TestDataProvider.readPartiallySuccessfulResponse())
                        )
        );
    }

    private void verifyMenuServiceCalled() {
        wiremock.verify(
                1,
                postRequestedFor(
                        urlEqualTo(MENU_INFO_PATH)
                )
        );
    }

    private void executeSql(Resource script) {
        new ResourceDatabasePopulator(script)
                .populate(connectionFactory)
                .block();
    }
}
