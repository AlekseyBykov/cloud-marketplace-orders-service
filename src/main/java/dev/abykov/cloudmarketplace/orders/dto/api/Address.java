package dev.abykov.cloudmarketplace.orders.dto.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Delivery address for an order.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Address {

    @NotBlank(message = "City must not be blank")
    private String city;

    @NotBlank(message = "Street must not be blank")
    private String street;

    @Positive(message = "House number must be greater than 0")
    private int house;

    @Positive(message = "Apartment number must be greater than 0")
    private int apartment;
}
