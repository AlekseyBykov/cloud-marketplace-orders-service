package dev.abykov.cloudmarketplace.orders.repository;

import dev.abykov.cloudmarketplace.orders.entity.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {

    Flux<Order> findAllByCreatedBy(String username, Pageable pageable);
}
