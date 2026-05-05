package dev.abykov.cloudmarketplace.orders.dto.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response returned by Menu Service for menu item resolution.
 *
 * <p>Contains a list of menu items with their price and availability status.</p>
 *
 * <p>This DTO mirrors the external API contract and should be kept in sync
 * with Menu Service response structure.</p>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderMenuResponse {

    /**
     * Resolved menu items.
     */
    private List<MenuInfo> items;
}
