package com.orderflow.order.order.repositories;

import com.orderflow.order.order.entities.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = "items")
    Optional<Order> findByCustomerEmailAndIdempotencyKey(String customerEmail, String idempotencyKey);

    @EntityGraph(attributePaths = "items")
    Optional<Order> findWithItemsById(Long id);

    List<Order> findByCustomerEmailOrderByCreatedAtDesc(String customerEmail, Pageable pageable);
}
