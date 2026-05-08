package dev.abykov.cloudmarketplace.orders.controller;

import dev.abykov.cloudmarketplace.orders.dto.api.CreateOrderRequest;
import dev.abykov.cloudmarketplace.orders.dto.api.OrderResponse;
import dev.abykov.cloudmarketplace.orders.dto.api.SortBy;
import dev.abykov.cloudmarketplace.orders.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive REST controller for customer orders.
 *
 * <p>Provides endpoints for:</p>
 * <ul>
 *     <li>Creating new orders</li>
 *     <li>Loading user orders</li>
 * </ul>
 *
 * <p><b>Authentication note:</b></p>
 * <ul>
 *     <li>User identity is temporarily passed via HTTP header:
 *     {@code X-User-Name}</li>
 *     <li>This is a simplified approach used before introducing Spring Security</li>
 * </ul>
 *
 * <p><b>Reactive stack:</b></p>
 * <ul>
 *     <li>Built with Spring WebFlux</li>
 *     <li>Endpoints return {@link Mono} and {@link Flux}</li>
 *     <li>Request processing is non-blocking</li>
 * </ul>
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/orders")
@Tag(
        name = "Orders",
        description = "REST API for customer orders"
)
public class OrderController {

    public static final String USER_HEADER = "X-User-Name";

    private final OrderService orderService;

    @Operation(
            summary = "Create order",
            description = "Creates new customer order"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order created"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error",
                    content = @Content(
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Menu item not found",
                    content = @Content(
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader(USER_HEADER) String username
    ) {
        log.info("Creating order for user={}", username);

        return orderService.createOrder(request, username);
    }

    @Operation(
            summary = "Get user orders",
            description = "Returns customer orders sorted by creation date"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orders loaded"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters",
                    content = @Content(
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    @GetMapping
    public Flux<OrderResponse> getOrders(
            @RequestParam(defaultValue = "0")
            @PositiveOrZero(message = "Page index must be >= 0")
            int from,

            @RequestParam(defaultValue = "10")
            @Positive(message = "Page size must be > 0")
            int size,

            @RequestParam(defaultValue = "DATE_ASC")
            SortBy sortBy,

            @RequestHeader(USER_HEADER)
            String username
    ) {
        log.info("Loading orders for user={}", username);

        return orderService.getOrdersOfUser(
                username,
                sortBy,
                from,
                size
        );
    }
}
