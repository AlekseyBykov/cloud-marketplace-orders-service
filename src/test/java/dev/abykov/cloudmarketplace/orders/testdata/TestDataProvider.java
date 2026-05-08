package dev.abykov.cloudmarketplace.orders.testdata;

import dev.abykov.cloudmarketplace.orders.dto.api.Address;
import dev.abykov.cloudmarketplace.orders.dto.api.CreateOrderRequest;
import dev.abykov.cloudmarketplace.orders.entity.OrderLineItem;
import okhttp3.mockwebserver.MockResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public final class TestDataProvider {

    private static final String MENU_ONE = "One";
    private static final String MENU_TWO = "Two";
    private static final String MENU_THREE = "Three";

    private static final String SUCCESS_RESPONSE_PATH =
            "wiremock/success-response.json";

    private static final String PARTIAL_SUCCESS_RESPONSE_PATH =
            "wiremock/partially-success-response.json";

    private TestDataProvider() {
    }

    public static MockResponse successResponse() {
        return jsonResponse(readSuccessfulResponse())
                .setResponseCode(HttpStatus.OK.value());
    }

    public static MockResponse partialSuccessResponse() {
        return jsonResponse(readPartiallySuccessfulResponse());
    }

    public static CreateOrderRequest createOrderRequest() {
        return CreateOrderRequest.builder()
                .address(validAddress())
                .nameToQuantity(
                        Map.of(
                                MENU_ONE, 1,
                                MENU_TWO, 2,
                                MENU_THREE, 3
                        )
                )
                .build();
    }

    public static CreateOrderRequest createInvalidOrderRequest() {
        return CreateOrderRequest.builder()
                .address(invalidAddress())
                .nameToQuantity(
                        Map.of(
                                MENU_ONE, 10,
                                MENU_TWO, 20,
                                MENU_THREE, 30
                        )
                )
                .build();
    }

    public static List<OrderLineItem> createdItems() {
        return List.of(
                createItem(MENU_ONE, 10.1, 10),
                createItem(MENU_TWO, 20.2, 20),
                createItem(MENU_THREE, 30.3, 30)
        );
    }

    public static String readSuccessfulResponse() {
        return readClasspathFile(SUCCESS_RESPONSE_PATH);
    }

    public static String readPartiallySuccessfulResponse() {
        return readClasspathFile(PARTIAL_SUCCESS_RESPONSE_PATH);
    }

    private static MockResponse jsonResponse(String body) {
        return new MockResponse()
                .addHeader(
                        HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .setBody(body);
    }

    private static Address validAddress() {
        return Address.builder()
                .city("City")
                .street("Street")
                .house(1)
                .apartment(1)
                .build();
    }

    private static Address invalidAddress() {
        return Address.builder()
                .city("")
                .street("")
                .house(-1)
                .apartment(-1)
                .build();
    }

    private static OrderLineItem createItem(
            String name,
            double price,
            int quantity
    ) {
        return OrderLineItem.builder()
                .menuItemName(name)
                .price(BigDecimal.valueOf(price))
                .quantity(quantity)
                .build();
    }

    private static String readClasspathFile(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            byte[] bytes = resource.getInputStream().readAllBytes();

            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to read test resource: " + path,
                    e
            );
        }
    }
}
