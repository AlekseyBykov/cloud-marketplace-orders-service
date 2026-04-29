package dev.abykov.cloudmarketplace.orders.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a customer order in the Cloud Marketplace system.
 *
 * <p>This entity is mapped using Spring Data R2DBC (not JPA/Hibernate).
 * It is a simple data holder without persistence context, lazy loading,
 * or proxy mechanics.
 * <p>
 * Unlike JPA entities, this class is not managed by an ORM.
 * Each instance is independent and represents a single DB row.
 *
 * <p><b>Implications:</b>
 * <ul>
 *     <li>No Hibernate proxies are used</li>
 *     <li>No dirty checking or entity lifecycle management</li>
 *     <li>equals/hashCode implementations are typically not required</li>
 * </ul>
 *
 * <p>Order items are stored as a JSONB column and mapped using custom
 * R2DBC converters.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Table("orders")
public class Order {

    @Id
    private Long id;

    @Column("total_price")
    private BigDecimal totalPrice;

    private String city;
    private String street;

    private Integer house;
    private Integer apartment;

    /**
     * List of ordered items.
     *
     * <p>Stored as JSONB in the database and mapped using custom
     * R2DBC converters.
     */
    @Column("items")
    private List<OrderLineItem> items;

    private OrderStatus status;

    /**
     * Identifier of the user who created the order.
     * Currently represented as a username.
     */
    @Column("created_by")
    private String createdBy;

    /**
     * Timestamp when the order was created.
     * Automatically populated by Spring Data R2DBC auditing.
     */
    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    /**
     * Timestamp when the order was last updated.
     * Automatically updated on each modification.
     */
    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
