package com.orderflow.order.invoice.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "invoice_counters")
public class InvoiceCounter {

    @Id
    private String financialYear;

    private long lastNumber;

    protected InvoiceCounter() {}

    public long next() {
        return ++lastNumber;
    }

    public String getFinancialYear() { return financialYear; }
    public long getLastNumber() { return lastNumber; }
}
