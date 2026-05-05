package dev.abykov.cloudmarketplace.orders.service;

import dev.abykov.cloudmarketplace.orders.dto.api.CreateOrderRequest;
import dev.abykov.cloudmarketplace.orders.dto.api.OrderResponse;
import dev.abykov.cloudmarketplace.orders.dto.api.SortBy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrderService {

    Mono<OrderResponse> createOrder(CreateOrderRequest request, String username);

    Flux<OrderResponse> getOrdersOfUser(String username, SortBy sortBy, int from, int size);
}
