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

/**
 * Repository integration test for {@link OrderRepository}.
 *
 * <p>This test verifies reactive repository behavior against a real PostgreSQL
 * instance started via Testcontainers.</p>
 *
 * <p><b>Architecture:</b></p>
 *
 * <pre>
 * OrderRepository
 *        ↓
 * R2DBC
 *        ↓
 * PostgreSQL (Testcontainers)
 * </pre>
 *
 * <p><b>Test slice:</b></p>
 * <ul>
 *     <li>{@link DataR2dbcTest} loads only data-layer components.</li>
 *     <li>No full Spring Boot application context is started.</li>
 *     <li>Flyway is disabled for repository slice tests.</li>
 * </ul>
 *
 * <p><b>Database initialization strategy:</b></p>
 * <ul>
 *     <li>Schema is created manually from {@code schema.sql}.</li>
 *     <li>Test data is inserted manually before each test.</li>
 *     <li>Database is cleaned after each test.</li>
 * </ul>
 *
 * <p><b>Reactive specifics:</b></p>
 * <ul>
 *     <li>Repository methods return {@link reactor.core.publisher.Flux}.</li>
 *     <li>Assertions are performed using {@link StepVerifier}.</li>
 *     <li>Blocking operations are used only during test setup/cleanup.</li>
 * </ul>
 */
@DataR2dbcTest(
        properties = {
                "spring.flyway.enabled=false"
        }
)
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

    /**
     * Configures R2DBC connection properties dynamically
     * using Testcontainers PostgreSQL instance.
     *
     * <p>Repository slice tests do not use application.yml
     * database configuration directly.</p>
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String r2dbcUrl =
                "r2dbc:postgresql://"
                        + POSTGRES.getHost()
                        + ":"
                        + POSTGRES.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)
                        + "/"
                        + POSTGRES.getDatabaseName();

        registry.add("spring.r2dbc.url", () -> r2dbcUrl);
        registry.add("spring.r2dbc.username", POSTGRES::getUsername);
        registry.add("spring.r2dbc.password", POSTGRES::getPassword);
    }

    /**
     * Creates database schema and inserts test data before each test.
     *
     * <p>Schema initialization is performed manually because Flyway
     * is disabled in repository slice tests.</p>
     */
    @BeforeEach
    void setUp(
            @Value("classpath:sql/schema.sql") Resource schema,
            @Value("classpath:sql/insert-orders.sql") Resource data
    ) {
        executeSql(schema);
        executeSql(data);
    }

    /**
     * Removes database objects after each test.
     *
     * <p>Ensures complete isolation between test cases.</p>
     */
    @AfterEach
    void tearDown() {
        executeSqlFromString("DROP TABLE IF EXISTS orders CASCADE");
    }

    /**
     * Verifies that repository returns user orders
     * sorted by creation date in ascending order.
     */
    @Test
    void shouldReturnUserOrdersSortedByCreationDate() {
        Flux<Order> result =
                repository.findAllByCreatedBy(
                        USERNAME,
                        FIRST_PAGE_SORTED_BY_DATE_ASC
                );

        StepVerifier.create(result)
                .expectNextMatches(orderMatches(FIRST_ORDER_DATE))
                .expectNextMatches(orderMatches(SECOND_ORDER_DATE))
                .verifyComplete();
    }

    /**
     * Verifies that repository returns user orders
     * sorted by creation date in descending order.
     */
    @Test
    void shouldReturnUserOrdersSortedByCreationDateDesc() {
        Flux<Order> result =
                repository.findAllByCreatedBy(
                        USERNAME,
                        FIRST_PAGE_SORTED_BY_DATE_DESC
                );

        StepVerifier.create(result)
                .expectNextMatches(orderMatches(THIRD_ORDER_DATE))
                .expectNextMatches(
                        orderMatchesWithUpdatedAt(SECOND_ORDER_DATE)
                )
                .verifyComplete();
    }

    /**
     * Verifies that repository returns empty result
     * when user has no orders.
     */
    @Test
    void shouldReturnEmptyResultWhenUserHasNoOrders() {
        Flux<Order> result =
                repository.findAllByCreatedBy(
                        UNKNOWN_USERNAME,
                        FIRST_PAGE_SORTED_BY_DATE_ASC_LARGE
                );

        StepVerifier.create(result)
                .expectNextCount(0)
                .verifyComplete();
    }

    /**
     * Creates predicate for verifying order ownership
     * and creation timestamp.
     */
    private Predicate<Order> orderMatches(LocalDateTime expectedDate) {
        return order ->
                USERNAME.equals(order.getCreatedBy())
                        && expectedDate.equals(order.getCreatedAt());
    }

    /**
     * Creates predicate for verifying order ownership,
     * creation timestamp and updatedAt presence.
     */
    private Predicate<Order> orderMatchesWithUpdatedAt(
            LocalDateTime expectedCreatedAt
    ) {
        return orderMatches(expectedCreatedAt)
                .and(order -> order.getUpdatedAt() != null);
    }

    /**
     * Executes SQL script using blocking setup operations.
     *
     * <p>Blocking is acceptable here because test lifecycle
     * is outside reactive request processing.</p>
     */
    private void executeSql(Resource script) {
        new ResourceDatabasePopulator(script)
                .populate(connectionFactory)
                .block();
    }

    /**
     * Executes raw SQL statement.
     *
     * <p>Mainly used for cleanup operations.</p>
     */
    private void executeSqlFromString(String sql) {
        new ResourceDatabasePopulator(
                new ByteArrayResource(sql.getBytes())
        )
                .populate(connectionFactory)
                .block();
    }
}
