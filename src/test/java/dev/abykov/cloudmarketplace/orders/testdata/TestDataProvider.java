package dev.abykov.cloudmarketplace.orders.testdata;

import okhttp3.mockwebserver.MockResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.ResourceUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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
