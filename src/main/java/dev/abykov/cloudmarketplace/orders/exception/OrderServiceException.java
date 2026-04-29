package dev.abykov.cloudmarketplace.orders.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class OrderServiceException extends RuntimeException {

    private final HttpStatus status;

    public OrderServiceException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}
