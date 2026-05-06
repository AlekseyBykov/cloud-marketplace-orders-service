package dev.abykov.cloudmarketplace.orders.testdata;

import dev.abykov.cloudmarketplace.orders.dto.api.Address;
import dev.abykov.cloudmarketplace.orders.dto.api.CreateOrderRequest;
import okhttp3.mockwebserver.MockResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.ResourceUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class TestDataProvider {

    /**
     * Возвращает результат запроса о доступности и ценах блюд.
     * Одно из возвращаемых блюд недоступно для заказа.
     */
    public static MockResponse partialSuccessResponse() {
        return new MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(readPartiallySuccessfulResponse());
    }

    public static MockResponse successResponse() {
        return new MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.OK.value())
                .setBody(readSuccessfulResponse());
    }

    public static CreateOrderRequest createOrderRequest() {
        return CreateOrderRequest.builder()
                .address(Address.builder()
                        .city("City")
                        .street("Street")
                        .house(1)
                        .apartment(1)
                        .build())
                .nameToQuantity(Map.of(
                        "One", 1,
                        "Two", 2,
                        "Three", 3
                ))
                .build();
    }

    public static String readSuccessfulResponse() {
        return readFileToString("wiremock/success-response.json");
    }

    public static String readPartiallySuccessfulResponse() {
        return readFileToString("wiremock/partially-success-response.json");
    }

    private static String readFileToString(String filePath) {
        try {
            Path path = ResourceUtils.getFile("classpath:" + filePath).toPath();
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
