package dev.abykov.cloudmarketplace.orders.dto.external;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Request payload sent from Orders Service to Menu Service.
 *
 * <p>Contains a list of menu item names that need to be resolved
 * into price and availability information.</p>
 *
 * <p>This DTO represents an external API contract and must match
 * the request structure expected by Menu Service.</p>
 */
@Data
@AllArgsConstructor
public class OrderMenuRequest {

    /**
     * List of menu item names to resolve.
     */
    private List<String> menuNames;
}
