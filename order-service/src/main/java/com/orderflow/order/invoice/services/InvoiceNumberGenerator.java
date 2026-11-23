package com.orderflow.order.invoice.services;

import com.orderflow.order.invoice.entities.InvoiceCounter;
import com.orderflow.order.invoice.repositories.InvoiceCounterRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
public class InvoiceNumberGenerator {

    private final InvoiceCounterRepository counters;

    public InvoiceNumberGenerator(InvoiceCounterRepository counters) {
        this.counters = counters;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String next(LocalDate issueDate) {
        String fy = financialYear(issueDate);
        counters.createIfMissing(fy);
        InvoiceCounter counter = counters.lockByFinancialYear(fy).orElseThrow();
        return String.format("INV-%s-%06d", fy, counter.next());
    }

    static String financialYear(LocalDate d) {
        int startYear = d.getMonthValue() >= 4 ? d.getYear() : d.getYear() - 1;
        return startYear + "-" + String.format("%02d", (startYear + 1) % 100);
    }
}
