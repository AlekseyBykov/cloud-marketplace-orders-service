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

    @BeforeEach
    void setupDb(@Value("classpath:sql/insert-orders.sql") Resource script) {
        executeSql(script);
    }

    @AfterEach
    void cleanupDb(@Value("classpath:sql/delete-orders.sql") Resource script) {
        executeSql(script);
    }

    private void executeSql(Resource script) {
        new ResourceDatabasePopulator(script)
                .populate(connectionFactory)
                .block();
    }

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

    private void assertExistingOrder(OrderResponse order, LocalDateTime expectedCreatedAt) {
        assertThat(order.getOrderId()).isNotNull();
        assertThat(order.getAddress().getCity()).isEqualTo("CityOne");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.getCreatedAt()).isEqualTo(expectedCreatedAt);
    }

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

    private void assertOrderItems(OrderResponse order) {
        ArrayList<OrderLineItem> items = new ArrayList<>(order.getOrderLineItems());
        items.sort(Comparator.comparing(OrderLineItem::getPrice));

        assertThat(items)
                .map(OrderLineItem::getMenuItemName)
                .containsExactly("One", "Two", "Three");
    }

    private void stubMenuServiceSuccess() {
        wiremock.stubFor(post(MENU_INFO_PATH)
                .willReturn(okJson(readSuccessfulResponse())));
    }

    private void verifyMenuServiceCalledOnce() {
        wiremock.verify(1, postRequestedFor(urlEqualTo(MENU_INFO_PATH)));
    }
}
