package dev.abykov.cloudmarketplace.orders.dto.api;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateOrderRequest {
    private Map<String, Integer> nameToQuantity;

    @Valid
    private Address address;
}
