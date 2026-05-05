package dev.abykov.cloudmarketplace.orders.dto.external;

import lombok.*;

import java.math.BigDecimal;

/**
 * External representation of a menu item used in inter-service communication.
 *
 * <p>Contains minimal information required for order creation:
 * name, price, and availability.</p>
 *
 * <p>This is a snapshot of data provided by Menu Service and should not be
 * confused with internal domain models.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Getter
@Setter
@ToString
public class MenuInfo {

    /**
     * Menu item name.
     */
    private String name;

    /**
     * Current price of the item.
     */
    private BigDecimal price;

    /**
     * Availability flag.
     * If false, the item cannot be ordered.
     */
    private boolean available;
}
