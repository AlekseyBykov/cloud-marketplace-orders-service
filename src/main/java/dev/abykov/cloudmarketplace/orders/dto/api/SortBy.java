package dev.abykov.cloudmarketplace.orders.dto.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import dev.abykov.cloudmarketplace.orders.exception.OrderServiceException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
public enum SortBy {

    DATE_ASC(Sort.by(Sort.Direction.ASC, "createdAt")),
    DATE_DESC(Sort.by(Sort.Direction.DESC, "createdAt"));

    @Getter
    private final Sort sort;

    @JsonCreator
    public static SortBy fromString(String str) {
        try {
            return SortBy.valueOf(str.toUpperCase());
        } catch (IllegalArgumentException e) {
            var msg = "Failed to create SortBy from string: %s".formatted(str);
            throw new OrderServiceException(msg, HttpStatus.BAD_REQUEST);
        }
    }
}
