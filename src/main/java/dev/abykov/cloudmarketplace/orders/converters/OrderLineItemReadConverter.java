package dev.abykov.cloudmarketplace.orders.converters;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.abykov.cloudmarketplace.orders.entity.OrderLineItem;
import dev.abykov.cloudmarketplace.orders.exception.OrderServiceException;
import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.List;

/**
 * Converts PostgreSQL JSONB data into a collection of {@link OrderLineItem}.
 *
 * <p>This converter is used by Spring Data R2DBC when reading data from
 * a JSONB column and mapping it back into Java objects.
 *
 * <p>It deserializes JSON content into {@code List<OrderLineItem>} using
 * Jackson {@link com.fasterxml.jackson.databind.ObjectMapper}.
 *
 * <p><b>Note:</b> This conversion is required because R2DBC does not
 * support automatic mapping of JSONB columns to complex Java types.
 *
 * @see OrderLineItemWriteConverter
 */
@ReadingConverter
@RequiredArgsConstructor
public class OrderLineItemReadConverter implements Converter<Json, List<OrderLineItem>> {

    private final ObjectMapper objectMapper;

    /**
     * Deserializes JSONB value into a list of order line items.
     *
     * @param value JSON value retrieved from PostgreSQL
     * @return list of order line items
     * @throws OrderServiceException if deserialization fails
     */
    @Override
    public List<OrderLineItem> convert(Json value) {
        try {
            return objectMapper.readValue(value.asArray(), new TypeReference<List<OrderLineItem>>() {
            });

        } catch (IOException e) {
            var msg = String.format("Failed to convert JSON %s to MenuLineItemCollection", value.asString());
            throw new OrderServiceException(msg, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
