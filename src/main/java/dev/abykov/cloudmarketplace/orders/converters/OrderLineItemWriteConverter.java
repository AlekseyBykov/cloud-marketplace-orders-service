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

@WritingConverter
@RequiredArgsConstructor
public class OrderLineItemWriteConverter implements Converter<List<OrderLineItem>, Json> {

    private final ObjectMapper objectMapper;

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
