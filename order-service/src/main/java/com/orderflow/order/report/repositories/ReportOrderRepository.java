package com.orderflow.order.report.repositories;

import com.orderflow.order.order.entities.Order;
import com.orderflow.order.order.entities.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface ReportOrderRepository extends Repository<Order, Long> {

    @Query("select o.status, count(o), sum(o.totalAmount) from Order o "
            + "where o.createdAt >= :fromTs and o.createdAt < :toTs group by o.status")
    List<Object[]> statusSummary(@Param("fromTs") Instant fromTs, @Param("toTs") Instant toTs);

    @Query("select i.productId, i.productName, sum(i.quantity), sum(i.unitPrice * i.quantity) from OrderItem i "
            + "where i.order.status = :status and i.order.createdAt >= :fromTs and i.order.createdAt < :toTs "
            + "group by i.productId, i.productName order by sum(i.unitPrice * i.quantity) desc")
    List<Object[]> topProducts(@Param("status") OrderStatus status, @Param("fromTs") Instant fromTs,
                               @Param("toTs") Instant toTs, Pageable pageable);

    @Query("select sum(inv.totalTax) from Invoice inv where inv.orderId in "
            + "(select o.id from Order o where o.status = :status and o.createdAt >= :fromTs and o.createdAt < :toTs)")
    BigDecimal taxCollected(@Param("status") OrderStatus status, @Param("fromTs") Instant fromTs,
                            @Param("toTs") Instant toTs);
}
