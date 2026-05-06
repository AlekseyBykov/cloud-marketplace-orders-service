package dev.abykov.cloudmarketplace.orders.service;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import dev.abykov.cloudmarketplace.orders.dto.api.CreateOrderRequest;
import dev.abykov.cloudmarketplace.orders.dto.api.OrderResponse;
import dev.abykov.cloudmarketplace.orders.dto.api.SortBy;
import dev.abykov.cloudmarketplace.orders.entity.OrderLineItem;
import dev.abykov.cloudmarketplace.orders.entity.OrderStatus;
import dev.abykov.cloudmarketplace.orders.service.impl.OrderServiceImpl;
import dev.abykov.cloudmarketplace.orders.testdata.TestDataProvider;
import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Comparator;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static dev.abykov.cloudmarketplace.orders.testdata.TestDataProvider.readSuccessfulResponse;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OrderServiceImpl}.
 *
 * <p><b>Test scope:</b></p>
 * <ul>
 *     <li>Full Spring context is started via {@link SpringBootTest}.</li>
 *     <li>Reactive database access is tested using R2DBC and PostgreSQL (Testcontainers).</li>
 *     <li>External HTTP dependency (Menu Service) is replaced with WireMock.</li>
 * </ul>
 *
 * <p><b>Infrastructure:</b></p>
 * <ul>
 *     <li>PostgreSQL container is started once for all tests.</li>
 *     <li>WireMock server is started as a JUnit extension.</li>
 *     <li>Application properties are overridden via {@link DynamicPropertySource}.</li>
 * </ul>
 *
 * <p><b>What is tested:</b></p>
 * <ul>
 *     <li>Business logic of order creation.</li>
 *     <li>Interaction with external Menu Service via HTTP.</li>
 *     <li>Persistence and retrieval of orders from database.</li>
 * </ul>
 *
 * <p><b>Important:</b></p>
 * <ul>
 *     <li>No real external services are used.</li>
 *     <li>HTTP calls are redirected to WireMock.</li>
 *     <li>Database is real (containerized), not mocked.</li>
 * </ul>
 */
@SpringBootTest
class OrderServiceImplTest {

    private static final String USERNAME = "username1";

    private static final LocalDateTime ORDER_ONE_DATE =
            LocalDateTime.of(2024, Month.FEBRUARY, 18, 10, 23, 54);
    private static final LocalDateTime ORDER_TWO_DATE =
            LocalDateTime.of(2024, Month.FEBRUARY, 20, 10, 23, 54);
    private static final LocalDateTime ORDER_THREE_DATE =
            LocalDateTime.of(2024, Month.FEBRUARY, 22, 10, 23, 54);

    private static final BigDecimal SUCCESS_TOTAL_PRICE = BigDecimal.valueOf(141.4);

    private static final String MENU_INFO_PATH = "/v1/menu-items/menu-info";

    @Autowired
    private OrderServiceImpl orderService;
    @Autowired
    private ConnectionFactory connectionFactory;

    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16.1")
                    .withDatabaseName("test_database")
                    .withUsername("user")
                    .withPassword("password");

    @RegisterExtension
    static WireMockExtension wiremock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    static {
        postgres.start();
    }

    /**
     * Overrides application properties for the test environment.
     *
     * <p>Configures:</p>
     * <ul>
     *     <li>R2DBC connection to Testcontainers PostgreSQL</li>
     *     <li>Flyway JDBC connection (required for migrations)</li>
     *     <li>External Menu Service base URL (redirected to WireMock)</li>
     *     <li>Reduced timeout for faster test execution</li>
     * </ul>
     *
     * <p>This ensures that all external dependencies are isolated and controlled.</p>
     */
    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () ->
                "r2dbc:postgresql://" + postgres.getHost() + ":" + postgres.getFirstMappedPort() + "/test_database");
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);

        registry.add("external.menu-service-url", wiremock::baseUrl);
        registry.add("external.default-timeout", () -> "1s");
    }

    /**
     * Populates database with test data before each test.
     *
     * <p>Uses blocking execution to ensure deterministic state.</p>
     */
    @BeforeEach
    void setupDb(@Value("classpath:sql/insert-orders.sql") Resource script) {
        executeSql(script);
    }

    /**
     * Cleans database after each test.
     *
     * <p>Ensures full isolation between test cases.</p>
     */
    @AfterEach
    void cleanupDb(@Value("classpath:sql/delete-orders.sql") Resource script) {
        executeSql(script);
    }

    /**
     * Executes SQL script against the database.
     *
     * <p><b>Note:</b> Uses blocking call, which is acceptable in test lifecycle.</p>
     */
    private void executeSql(Resource script) {
        new ResourceDatabasePopulator(script)
                .populate(connectionFactory)
                .block();
    }

    /**
     * Verifies that orders are correctly retrieved and sorted by creation date.
     *
     * <p>Checks:</p>
     * <ul>
     *     <li>Only orders of the specified user are returned</li>
     *     <li>Sorting by date (ascending) is applied correctly</li>
     *     <li>Reactive stream completes successfully</li>
     * </ul>
     */
    @Test
    void shouldReturnUserOrdersSortedByDate() {
        Flux<OrderResponse> orders =
                orderService.getOrdersOfUser(USERNAME, SortBy.DATE_ASC, 0, 10);

        StepVerifier.create(orders)
                .assertNext(order -> assertExistingOrder(order, ORDER_ONE_DATE))
                .assertNext(order -> assertExistingOrder(order, ORDER_TWO_DATE))
                .assertNext(order -> assertExistingOrder(order, ORDER_THREE_DATE))
                .verifyComplete();
    }

    /**
     * Verifies successful order creation when all requested menu items are available.
     *
     * <p>Test flow:</p>
     * <ul>
     *     <li>WireMock is configured to simulate successful Menu Service response</li>
     *     <li>Order creation request is sent to the service</li>
     *     <li>Response is validated (status, price, timestamps, items)</li>
     *     <li>HTTP interaction with external service is verified</li>
     * </ul>
     */
    @Test
    void shouldCreateOrderWhenAllMenuItemsAvailable() {
        stubMenuServiceSuccess();

        CreateOrderRequest request = TestDataProvider.createOrderRequest();
        LocalDateTime beforeCreation = LocalDateTime.now().minusNanos(1000);

        Mono<OrderResponse> response =
                orderService.createOrder(request, USERNAME);

        StepVerifier.create(response)
                .assertNext(order -> assertCreatedOrder(order, request, beforeCreation))
                .verifyComplete();

        verifyMenuServiceCalledOnce();
    }

    /**
     * Asserts fields of an existing order retrieved from database.
     */
    private void assertExistingOrder(OrderResponse order, LocalDateTime expectedCreatedAt) {
        assertThat(order.getOrderId()).isNotNull();
        assertThat(order.getAddress().getCity()).isEqualTo("CityOne");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.getCreatedAt()).isEqualTo(expectedCreatedAt);
    }

    /**
     * Asserts fields of a newly created order.
     *
     * <p>Includes validation of:</p>
     * <ul>
     *     <li>Generated ID</li>
     *     <li>Address mapping</li>
     *     <li>Total price calculation</li>
     *     <li>Status</li>
     *     <li>Creation timestamp</li>
     *     <li>Order items</li>
     * </ul>
     */
    private void assertCreatedOrder(
            OrderResponse order,
            CreateOrderRequest request,
            LocalDateTime beforeCreation
    ) {
        assertThat(order.getOrderId()).isNotNull();
        assertThat(order.getAddress()).isEqualTo(request.getAddress());
        assertThat(order.getTotalPrice()).isEqualTo(SUCCESS_TOTAL_PRICE);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.getCreatedAt()).isAfter(beforeCreation);

        assertOrderItems(order);
    }

    /**
     * Verifies order line items.
     *
     * <p>Items are sorted for deterministic comparison.</p>
     */
    private void assertOrderItems(OrderResponse order) {
        ArrayList<OrderLineItem> items = new ArrayList<>(order.getOrderLineItems());
        items.sort(Comparator.comparing(OrderLineItem::getPrice));

        assertThat(items)
                .map(OrderLineItem::getMenuItemName)
                .containsExactly("One", "Two", "Three");
    }

    /**
     * Configures WireMock to simulate successful Menu Service response.
     *
     * <p>All requested menu items are available with predefined prices.</p>
     */
    private void stubMenuServiceSuccess() {
        wiremock.stubFor(post(MENU_INFO_PATH)
                .willReturn(okJson(readSuccessfulResponse())));
    }

    /**
     * Verifies that exactly one HTTP request was sent to Menu Service.
     */
    private void verifyMenuServiceCalledOnce() {
        wiremock.verify(1, postRequestedFor(urlEqualTo(MENU_INFO_PATH)));
    }
}
