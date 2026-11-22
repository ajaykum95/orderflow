package com.orderflow.order.invoice.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// हर वित्त-वर्ष (जैसे "2026-27") की एक पंक्ति, जिसमें आख़िरी इस्तेमाल हुआ नंबर लिखा है
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
