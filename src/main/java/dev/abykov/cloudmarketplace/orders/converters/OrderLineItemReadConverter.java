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

@ReadingConverter
@RequiredArgsConstructor
public class OrderLineItemReadConverter implements Converter<Json, List<OrderLineItem>> {

    private final ObjectMapper objectMapper;

    @Override
    public List<OrderLineItem> convert(Json value) {
        try {
            return objectMapper.readValue(value.asArray(), new TypeReference<List<OrderLineItem>>() {});

        } catch (IOException e) {
            var msg = String.format("Failed to convert JSON %s to MenuLineItemCollection", value.asString());
            throw new OrderServiceException(msg, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
