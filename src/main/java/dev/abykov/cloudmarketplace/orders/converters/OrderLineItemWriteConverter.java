package dev.abykov.cloudmarketplace.orders.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.abykov.cloudmarketplace.orders.entity.OrderLineItem;
import dev.abykov.cloudmarketplace.orders.exception.OrderServiceException;
import io.r2dbc.postgresql.codec.Json;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Converts a collection of {@link OrderLineItem} objects into PostgreSQL JSONB.
 *
 * <p>This converter is used by Spring Data R2DBC when persisting entities
 * that contain complex types not natively supported by the driver.
 *
 * <p>Specifically, it serializes {@code List<OrderLineItem>} into a JSON string
 * and wraps it into {@link io.r2dbc.postgresql.codec.Json}, which is required
 * for storing data in a JSONB column.
 *
 * <p><b>Note:</b> Unlike JPA/Hibernate, R2DBC does not provide automatic
 * JSON mapping, so custom converters must be explicitly registered.
 *
 * @see OrderLineItemReadConverter
 */
@WritingConverter
@RequiredArgsConstructor
public class OrderLineItemWriteConverter implements Converter<List<OrderLineItem>, Json> {

    private final ObjectMapper objectMapper;

    /**
     * Serializes a list of order line items into JSONB representation.
     *
     * @param menuLineItems list of order line items
     * @return JSON representation suitable for PostgreSQL JSONB column
     * @throws OrderServiceException if serialization fails
     */
    @Override
    public Json convert(@NotNull List<OrderLineItem> menuLineItems) {
        try {
            return Json.of(objectMapper.writeValueAsString(menuLineItems));

        } catch (JsonProcessingException e) {
            var msg = String.format("Failed to convert MenuLineItemCollection %s to JSON", menuLineItems);
            throw new OrderServiceException(msg, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
