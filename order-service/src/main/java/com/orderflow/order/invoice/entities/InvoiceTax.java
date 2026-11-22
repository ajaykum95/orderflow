package com.orderflow.order.invoice.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

@Embeddable
public class InvoiceTax {

    private String name;

    @Column(precision = 5, scale = 2)
    private BigDecimal rate;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    protected InvoiceTax() {}

    public InvoiceTax(String name, BigDecimal rate, BigDecimal amount) {
        this.name = name;
        this.rate = rate;
        this.amount = amount;
    }

    public String getName() { return name; }
    public BigDecimal getRate() { return rate; }
    public BigDecimal getAmount() { return amount; }
}
