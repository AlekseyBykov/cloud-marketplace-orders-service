package dev.abykov.cloudmarketplace.orders.repository;

import dev.abykov.cloudmarketplace.orders.config.R2dbcConfig;
import dev.abykov.cloudmarketplace.orders.entity.Order;
import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.function.Predicate;

@DataR2dbcTest
@Testcontainers
@Import(R2dbcConfig.class)
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
class OrderRepositoryTest {

    private static final String USERNAME = "username1";
    private static final String UNKNOWN_USERNAME = "unknown-user";

    private static final LocalDateTime FIRST_ORDER_DATE =
            LocalDateTime.of(2024, Month.FEBRUARY, 18, 10, 23, 54);

    private static final LocalDateTime SECOND_ORDER_DATE =
            LocalDateTime.of(2024, Month.FEBRUARY, 20, 10, 23, 54);

    private static final LocalDateTime THIRD_ORDER_DATE =
            LocalDateTime.of(2024, Month.FEBRUARY, 22, 10, 23, 54);

    private static final PageRequest FIRST_PAGE_SORTED_BY_DATE_ASC =
            PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "createdAt"));

    private static final PageRequest FIRST_PAGE_SORTED_BY_DATE_DESC =
            PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt"));

    private static final PageRequest FIRST_PAGE_SORTED_BY_DATE_ASC_LARGE =
            PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "createdAt"));

    @Autowired
    private OrderRepository repository;

    @Autowired
    private ConnectionFactory connectionFactory;

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16.1"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String r2dbcUrl = "r2dbc:postgresql://" +
                POSTGRES.getHost() + ":" +
                POSTGRES.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT) + "/" +
                POSTGRES.getDatabaseName();

        registry.add("spring.r2dbc.url", () -> r2dbcUrl);
        registry.add("spring.r2dbc.username", POSTGRES::getUsername);
        registry.add("spring.r2dbc.password", POSTGRES::getPassword);

        // Flyway uses JDBC
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void setUp(
            @Value("classpath:sql/schema.sql") Resource schema,
            @Value("classpath:sql/insert-orders.sql") Resource data
    ) {
        executeSql(schema);
        executeSql(data);
    }

    @AfterEach
    void tearDown() {
        executeSqlFromString("DROP TABLE IF EXISTS orders CASCADE");
    }

    @Test
    void shouldReturnUserOrdersSortedByCreationDate() {
        Flux<Order> result = repository.findAllByCreatedBy(USERNAME, FIRST_PAGE_SORTED_BY_DATE_ASC);

        StepVerifier.create(result)
                .expectNextMatches(orderMatches(FIRST_ORDER_DATE))
                .expectNextMatches(orderMatches(SECOND_ORDER_DATE))
                .verifyComplete();
    }

    @Test
    void shouldReturnUserOrdersSortedByCreationDateDesc() {
        Flux<Order> result = repository.findAllByCreatedBy(USERNAME, FIRST_PAGE_SORTED_BY_DATE_DESC);

        StepVerifier.create(result)
                .expectNextMatches(orderMatches(THIRD_ORDER_DATE))
                .expectNextMatches(orderMatchesWithUpdatedAt(SECOND_ORDER_DATE))
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyResultWhenUserHasNoOrders() {
        Flux<Order> result = repository.findAllByCreatedBy(UNKNOWN_USERNAME, FIRST_PAGE_SORTED_BY_DATE_ASC_LARGE);

        StepVerifier.create(result)
                .expectNextCount(0)
                .verifyComplete();
    }

    private Predicate<Order> orderMatches(LocalDateTime expectedDate) {
        return order ->
                USERNAME.equals(order.getCreatedBy()) &&
                        expectedDate.equals(order.getCreatedAt());
    }

    private Predicate<Order> orderMatchesWithUpdatedAt(LocalDateTime expectedCreatedAt) {
        return orderMatches(expectedCreatedAt).and(order -> order.getUpdatedAt() != null);
    }

    private void executeSql(Resource script) {
        new ResourceDatabasePopulator(script)
                .populate(connectionFactory)
                .block();
    }

    private void executeSqlFromString(String sql) {
        new ResourceDatabasePopulator(new ByteArrayResource(sql.getBytes()))
                .populate(connectionFactory)
                .block();
    }
}
