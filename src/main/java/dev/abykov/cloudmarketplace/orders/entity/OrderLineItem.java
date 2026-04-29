package dev.abykov.cloudmarketplace.orders.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderLineItem {

    private String menuItemName;
    private BigDecimal price;
    private Integer quantity;
}
