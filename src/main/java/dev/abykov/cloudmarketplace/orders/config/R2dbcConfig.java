package dev.abykov.cloudmarketplace.orders.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.abykov.cloudmarketplace.orders.converters.OrderLineItemReadConverter;
import dev.abykov.cloudmarketplace.orders.converters.OrderLineItemWriteConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.PostgresDialect;

import java.util.List;

/**
 * Configuration for R2DBC (Reactive Relational Database Connectivity).
 *
 * <p>This configuration enables two key features:
 *
 * <ul>
 *     <li><b>Auditing support</b> – automatically populates {@code @CreatedDate}
 *     and {@code @LastModifiedDate} fields on entity persistence</li>
 *
 *     <li><b>Custom type conversions</b> – registers converters required for
 *     mapping complex types (e.g. JSONB columns) to Java objects</li>
 * </ul>
 *
 * <p><b>Important:</b> Unlike JPA/Hibernate, Spring Data R2DBC does not provide
 * built-in support for advanced types such as JSONB. Therefore, custom converters
 * must be explicitly registered.
 *
 * <p>In this service, the {@code items} column (JSONB) is mapped to
 * {@code List<OrderLineItem>} using {@link OrderLineItemReadConverter}
 * and {@link OrderLineItemWriteConverter}.
 *
 * <p>This configuration replaces functionality typically handled by Hibernate
 * in JPA-based applications.
 *
 * <p><b>Note:</b> This configuration is specific to PostgreSQL and uses
 * {@link PostgresDialect} for proper handling of database-specific types.
 */
@Configuration
@EnableR2dbcAuditing
public class R2dbcConfig {

    /**
     * Registers custom converters for R2DBC.
     *
     * <p>These converters handle transformation between PostgreSQL JSONB columns
     * and Java objects.
     *
     * @param objectMapper Jackson object mapper used for JSON serialization/deserialization
     * @return configured {@link R2dbcCustomConversions}
     */
    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions(ObjectMapper objectMapper) {
        List<Converter<?, ?>> converters = List.of(
                new OrderLineItemReadConverter(objectMapper),
                new OrderLineItemWriteConverter(objectMapper)
        );
        return R2dbcCustomConversions.of(PostgresDialect.INSTANCE, converters);
    }
}
