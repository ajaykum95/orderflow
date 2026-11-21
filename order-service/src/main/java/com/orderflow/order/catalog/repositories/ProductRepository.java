package com.orderflow.order.catalog.repositories;

import com.orderflow.order.catalog.entities.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByActiveTrue(Pageable pageable);

    Optional<Product> findByIdAndActiveTrue(Long id);

    @Modifying
    @Query("update Product p set p.stock = p.stock - :qty where p.id = :id and p.active = true and p.stock >= :qty")
    int decreaseStock(@Param("id") Long id, @Param("qty") int qty);

    @Modifying
    @Query("update Product p set p.stock = p.stock + :qty where p.id = :id")
    int increaseStock(@Param("id") Long id, @Param("qty") int qty);
}
