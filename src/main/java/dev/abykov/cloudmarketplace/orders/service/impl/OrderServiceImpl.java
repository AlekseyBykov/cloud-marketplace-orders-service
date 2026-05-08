package dev.abykov.cloudmarketplace.orders.service.impl;

import dev.abykov.cloudmarketplace.orders.client.MenuClient;
import dev.abykov.cloudmarketplace.orders.dto.api.CreateOrderRequest;
import dev.abykov.cloudmarketplace.orders.dto.api.OrderResponse;
import dev.abykov.cloudmarketplace.orders.dto.api.SortBy;
import dev.abykov.cloudmarketplace.orders.dto.external.MenuInfo;
import dev.abykov.cloudmarketplace.orders.dto.external.OrderMenuRequest;
import dev.abykov.cloudmarketplace.orders.dto.external.OrderMenuResponse;
import dev.abykov.cloudmarketplace.orders.entity.Order;
import dev.abykov.cloudmarketplace.orders.exception.OrderServiceException;
import dev.abykov.cloudmarketplace.orders.mapper.OrderMapper;
import dev.abykov.cloudmarketplace.orders.repository.OrderRepository;
import dev.abykov.cloudmarketplace.orders.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Set;

/**
 * Implementation of {@link OrderService}.
 *
 * <p>Responsible for orchestrating order creation and retrieval.</p>
 *
 * <p>This service:
 * <ul>
 *     <li>Fetches menu data from Menu Service</li>
 *     <li>Validates business constraints (availability)</li>
 *     <li>Builds and persists Order entities</li>
 *     <li>Maps entities to API responses</li>
 * </ul>
 *
 * <p>All operations are non-blocking and use reactive types ({@link Mono}, {@link Flux}).</p>
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository repository;
    private final MenuClient menuClient;
    private final OrderMapper orderMapper;

    /**
     * Creates a new order for a given user.
     *
     * <p>Flow:
     * <ol>
     *     <li>Extract requested menu item names</li>
     *     <li>Fetch menu info (price + availability) from Menu Service</li>
     *     <li>Validate that all items are available</li>
     *     <li>Map createOrderRequest + external data to Order entity (snapshot)</li>
     *     <li>Persist order</li>
     *     <li>Map entity to response DTO</li>
     * </ol>
     *
     * @param createOrderRequest  incoming order createOrderRequest
     * @param username user creating the order
     * @return created order response
     */
    @Override
    public Mono<OrderResponse> createOrder(CreateOrderRequest createOrderRequest, String username) {
        Set<String> menuNames = extractMenuNames(createOrderRequest);

        OrderMenuRequest orderMenuRequest = new OrderMenuRequest(new ArrayList<>(menuNames));

        return menuClient.getMenuInfo(orderMenuRequest)
                .flatMap(infoResponse -> processOrderCreation(createOrderRequest, username, infoResponse))
                .map(orderMapper::mapToResponse);
    }

    /**
     * Retrieves paginated and sorted list of user orders.
     *
     * @param username user identifier
     * @param sortBy   sorting strategy
     * @param from     offset (zero-based)
     * @param size     page size
     * @return stream of order responses
     */
    @Override
    public Flux<OrderResponse> getOrdersOfUser(String username, SortBy sortBy, int from, int size) {
        PageRequest pageRequest = buildPageRequest(sortBy, from, size);

        return repository.findAllByCreatedBy(username, pageRequest)
                .map(orderMapper::mapToResponse);
    }

    /**
     * Extracts menu item names from request.
     */
    private Set<String> extractMenuNames(CreateOrderRequest request) {
        return request.getNameToQuantity().keySet();
    }

    /**
     * Full order creation pipeline after menu info is fetched.
     */
    private Mono<Order> processOrderCreation(
            CreateOrderRequest request,
            String username,
            OrderMenuResponse infoResponse
    ) {
        validateAvailability(infoResponse);

        Order order = orderMapper.mapToOrder(request, username, infoResponse);

        return repository.save(order);
    }

    /**
     * Validates that all requested menu items are available.
     *
     * @throws OrderServiceException if any item is unavailable
     */
    private void validateAvailability(OrderMenuResponse infoResponse) {
        boolean allAvailable = infoResponse.getItems().stream()
                .allMatch(MenuInfo::isAvailable);

        if (!allAvailable) {
            throw new OrderServiceException(
                    "Some menu items are not available",
                    HttpStatus.NOT_FOUND
            );
        }
    }

    /**
     * Builds PageRequest from offset-based pagination parameters.
     */
    private PageRequest buildPageRequest(SortBy sortBy, int from, int size) {
        int page = from / size;
        return PageRequest.of(page, size, sortBy.getSort());
    }
}
