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
 * Integration test for {@link OrderRepository} using Testcontainers and R2DBC.
 *
 * <p><b>Architecture specifics:</b></p>
 * <ul>
 *     <li>Uses {@link DataR2dbcTest} slice — only data layer is loaded (no full Spring context).</li>
 *     <li>Reactive stack (R2DBC) is used for repository operations.</li>
 *     <li>PostgreSQL is provided via Testcontainers.</li>
 * </ul>
 *
 * <p><b>Database initialization strategy:</b></p>
 * <ul>
 *     <li>Flyway is NOT used in tests (even though configured), because {@code @DataR2dbcTest}
 *     does not trigger Flyway auto-configuration.</li>
 *     <li>Schema is created manually via SQL scripts before each test.</li>
 *     <li>Test data is inserted manually using SQL scripts.</li>
 *     <li>Database is cleaned after each test by dropping the table.</li>
 * </ul>
 *
 * <p><b>Reactive vs Blocking:</b></p>
 * <ul>
 *     <li>Repository methods return {@link reactor.core.publisher.Flux} (non-blocking).</li>
 *     <li>Assertions are performed using {@link reactor.test.StepVerifier}.</li>
 *     <li><b>Blocking is used ONLY in test setup/teardown</b> via {@code .block()}:
 *         <ul>
 *             <li>Schema creation</li>
 *             <li>Test data insertion</li>
 *             <li>Cleanup</li>
 *         </ul>
 *     </li>
 *     <li>This is acceptable because test lifecycle is not part of reactive flow.</li>
 * </ul>
 *
 * <p><b>Important note:</b></p>
 * <ul>
 *     <li>Never use {@code .block()} inside production reactive pipelines.</li>
 *     <li>Blocking in tests is safe and simplifies deterministic setup.</li>
 * </ul>
 */
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

    /**
     * Initializes database schema and inserts test data before each test.
     *
     * <p>Uses blocking calls to ensure deterministic state before test execution.</p>
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
     * Cleans database after each test by dropping the table.
     *
     * <p>Ensures full isolation between test cases.</p>
     */
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

    /**
     * Cleans database after each test by dropping the table.
     *
     * <p>Ensures full isolation between test cases.</p>
     */
    private void executeSql(Resource script) {
        new ResourceDatabasePopulator(script)
                .populate(connectionFactory)
                .block();
    }

    /**
     * Executes raw SQL string against the database.
     *
     * <p>Used mainly for cleanup operations.</p>
     *
     * <p><b>Blocking operation:</b> uses {@code .block()} for deterministic execution.</p>
     *
     * @param sql SQL statement
     */
    private void executeSqlFromString(String sql) {
        new ResourceDatabasePopulator(new ByteArrayResource(sql.getBytes()))
                .populate(connectionFactory)
                .block();
    }
}
