package dev.abykov.cloudmarketplace.orders.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global HTTP exception handler for reactive REST API.
 *
 * <p>Converts internal application exceptions into structured HTTP responses
 * using {@link ProblemDetail}.</p>
 *
 * <p><b>Why this is needed:</b></p>
 * <ul>
 *     <li>Provides consistent error response format for clients</li>
 *     <li>Separates error mapping from controller logic</li>
 *     <li>Handles validation and deserialization errors globally</li>
 *     <li>Improves API predictability and debugging</li>
 * </ul>
 *
 * <p><b>WebFlux specifics:</b></p>
 * <ul>
 *     <li>Reactive applications use different exception types compared to Spring MVC</li>
 *     <li>Validation errors are represented by {@link WebExchangeBindException}</li>
 *     <li>Responses are returned reactively via {@link Mono}</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleValidationException(
            WebExchangeBindException ex,
            ServerHttpRequest request
    ) {
        Map<String, String> errors = ex.getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        DefaultMessageSourceResolvable::getDefaultMessage,
                        (a, b) -> a
                ));

        log.error("Validation error: {}", errors);

        ProblemDetail pd = createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request
        );

        pd.setProperty("invalid_params", errors);

        return Mono.just(
                ResponseEntity.badRequest().body(pd)
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleUnreadableMessage(
            HttpMessageNotReadableException ex,
            ServerHttpRequest request
    ) {
        log.error("Invalid request body", ex);

        ProblemDetail pd = createProblemDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request
        );

        return Mono.just(
                ResponseEntity.badRequest().body(pd)
        );
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleMethodValidation(
            HandlerMethodValidationException ex,
            ServerHttpRequest request
    ) {
        Map<String, String> errors = new HashMap<>();

        ex.getAllValidationResults().forEach(result ->
                result.getResolvableErrors().forEach(error ->
                        errors.put(
                                result.getMethodParameter().getParameterName(),
                                error.getDefaultMessage()
                        )
                )
        );

        ProblemDetail pd = createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Invalid request parameters",
                request
        );

        pd.setProperty("invalid_params", errors);

        return Mono.just(
                ResponseEntity.badRequest().body(pd)
        );
    }

    @ExceptionHandler(OrderServiceException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleOrderServiceException(
            OrderServiceException ex,
            ServerHttpRequest request
    ) {
        log.error(
                "Order service error: status={}, message={}",
                ex.getStatus(),
                ex.getMessage()
        );

        ProblemDetail pd = createProblemDetail(
                ex.getStatus(),
                ex.getMessage(),
                request
        );

        return Mono.just(
                ResponseEntity.status(ex.getStatus()).body(pd)
        );
    }

    private ProblemDetail createProblemDetail(
            HttpStatus status,
            String message,
            ServerHttpRequest request
    ) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                status,
                message
        );

        pd.setInstance(request.getURI());
        pd.setProperty("timestamp", Instant.now());

        return pd;
    }
}
