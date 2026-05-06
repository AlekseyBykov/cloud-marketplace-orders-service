package dev.abykov.cloudmarketplace.orders.mapper;

import dev.abykov.cloudmarketplace.orders.dto.api.Address;
import dev.abykov.cloudmarketplace.orders.dto.api.CreateOrderRequest;
import dev.abykov.cloudmarketplace.orders.dto.api.OrderResponse;
import dev.abykov.cloudmarketplace.orders.dto.external.MenuInfo;
import dev.abykov.cloudmarketplace.orders.dto.external.OrderMenuResponse;
import dev.abykov.cloudmarketplace.orders.entity.Order;
import dev.abykov.cloudmarketplace.orders.entity.OrderLineItem;
import dev.abykov.cloudmarketplace.orders.entity.OrderStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public Order mapToOrder(
            CreateOrderRequest request,
            String username,
            OrderMenuResponse infoResponse)
    {
        Map<String, MenuInfo> menuInfoMap = infoResponse.getItems().stream()
                .collect(Collectors.toMap(MenuInfo::getName, Function.identity()));

        List<OrderLineItem> items = request.getNameToQuantity().entrySet().stream()
                .map(entry -> {
                    String name = entry.getKey();
                    Integer quantity = entry.getValue();
                    MenuInfo info = menuInfoMap.get(name);

                    return new OrderLineItem(
                            name,
                            info.getPrice(),
                            quantity
                    );
                })
                .toList();

        BigDecimal total = items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Order.builder()
                .items(items)
                .totalPrice(total)
                .city(request.getAddress().getCity())
                .street(request.getAddress().getStreet())
                .house(request.getAddress().getHouse())
                .apartment(request.getAddress().getApartment())
                .status(OrderStatus.CREATED)
                .createdBy(username)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .totalPrice(order.getTotalPrice())
                .orderLineItems(order.getItems())
                .address(Address.builder()
                        .city(order.getCity())
                        .street(order.getStreet())
                        .house(order.getHouse())
                        .apartment(order.getApartment())
                        .build())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
