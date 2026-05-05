package dev.abykov.cloudmarketplace.orders.dto.api;

import dev.abykov.cloudmarketplace.orders.entity.OrderLineItem;
import dev.abykov.cloudmarketplace.orders.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderResponse {

    private Long orderId;
    private BigDecimal totalPrice;
    private List<OrderLineItem> orderLineItems;
    private Address address;
    private OrderStatus status;
    private LocalDateTime createdAt;
}
