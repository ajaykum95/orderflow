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

// Report का असली डेटा Postgres से आता है (वही "सच का स्रोत" है); Mongo में सिर्फ़ उसका स्नैपशॉट रखा जाता है।
// (parameter के नाम from/to नहीं रखे: from JPQL का शब्द है)
public interface ReportOrderRepository extends Repository<Order, Long> {

    // हर status के कितने orders और उनका कुल: [status, count, sum]
    @Query("select o.status, count(o), sum(o.totalAmount) from Order o "
            + "where o.createdAt >= :fromTs and o.createdAt < :toTs group by o.status")
    List<Object[]> statusSummary(@Param("fromTs") Instant fromTs, @Param("toTs") Instant toTs);

    // सबसे ज़्यादा बिकने वाले products (कमाई के हिसाब से): [productId, name, quantity, revenue]
    @Query("select i.productId, i.productName, sum(i.quantity), sum(i.unitPrice * i.quantity) from OrderItem i "
            + "where i.order.status = :status and i.order.createdAt >= :fromTs and i.order.createdAt < :toTs "
            + "group by i.productId, i.productName order by sum(i.unitPrice * i.quantity) desc")
    List<Object[]> topProducts(@Param("status") OrderStatus status, @Param("fromTs") Instant fromTs,
                               @Param("toTs") Instant toTs, Pageable pageable);

    // उन orders के invoices में कुल टैक्स (कोई invoice न हो तो null)
    @Query("select sum(inv.totalTax) from Invoice inv where inv.orderId in "
            + "(select o.id from Order o where o.status = :status and o.createdAt >= :fromTs and o.createdAt < :toTs)")
    BigDecimal taxCollected(@Param("status") OrderStatus status, @Param("fromTs") Instant fromTs,
                            @Param("toTs") Instant toTs);
}
