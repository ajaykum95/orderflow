package com.orderflow.order.invoice.repositories;

import com.orderflow.order.invoice.entities.InvoiceCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InvoiceCounterRepository extends JpaRepository<InvoiceCounter, String> {

    @Modifying
    @Query(value = "insert into invoice_counters (financial_year, last_number) values (:fy, 0) "
            + "on conflict (financial_year) do nothing", nativeQuery = true)
    void createIfMissing(@Param("fy") String fy);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from InvoiceCounter c where c.financialYear = :fy")
    Optional<InvoiceCounter> lockByFinancialYear(@Param("fy") String fy);
}
